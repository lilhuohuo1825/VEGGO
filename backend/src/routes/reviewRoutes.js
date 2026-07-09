const express = require('express');
const Review = require('../models/Reviews');
const User = require('../models/User');
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const asyncHandler = require('../middleware/asyncHandler');
const { evaluateCustomerCertificate } = require('../services/certificateService');
const {
  getReviewCarbonPoints,
  isPremiumReview,
  sumOrderReviewCarbonPoints,
  canEditReview,
  getReviewEditDeadline,
} = require('../utils/reviewCarbonRules');

const router = express.Router();

let reviewsCache = null;
let reviewsCacheTime = 0;

function invalidateReviewsCache() {
  reviewsCache = null;
}

function resolvePublicBaseUrl(req) {
  return (process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`).replace(/\/$/, '');
}

function toAbsoluteAvatarUrl(req, avatarUrl) {
  const value = String(avatarUrl || '').trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  const base = resolvePublicBaseUrl(req);
  return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
}

function toAbsoluteMediaUrl(req, mediaUrl) {
  return toAbsoluteAvatarUrl(req, mediaUrl);
}

function normalizeReviewImages(req, images) {
  if (!Array.isArray(images)) return [];
  return images
    .map((image) => toAbsoluteMediaUrl(req, image))
    .filter(Boolean);
}

async function resolveAvatarForCustomer(req, customerId, fallbackUrl = '') {
  const trimmedFallback = String(fallbackUrl || '').trim();
  if (trimmedFallback) {
    return toAbsoluteAvatarUrl(req, trimmedFallback);
  }
  const normalizedCustomerId = String(customerId || '').trim();
  if (!normalizedCustomerId) {
    return '';
  }
  const user = await User.findOne({ CustomerID: normalizedCustomerId })
    .select('avatarUrl')
    .lean();
  return toAbsoluteAvatarUrl(req, user?.avatarUrl || '');
}

async function enrichReviewsWithAvatars(req, reviews) {
  if (!Array.isArray(reviews)) {
    return [];
  }
  const enriched = [];
  for (const review of reviews) {
    const plain = review && typeof review === 'object' ? { ...review } : review;
    plain.avatarUrl = await resolveAvatarForCustomer(
      req,
      plain.customer_id || plain.customerId,
      plain.avatarUrl || plain.avatar_url || ''
    );
    plain.images = normalizeReviewImages(req, plain.images);
    enriched.push(plain);
  }
  return enriched;
}

async function enrichReviewDocument(req, doc) {
  if (!doc) {
    return doc;
  }
  const plain = doc.toObject ? doc.toObject() : { ...doc };
  plain.reviews = await enrichReviewsWithAvatars(req, plain.reviews);
  return plain;
}


const uploadDir = path.join(__dirname, '..', '..', 'uploads', 'reviews');
fs.mkdirSync(uploadDir, { recursive: true });
const mediaUpload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname || '').toLowerCase() || '.jpg';
      const safeExt = ['.jpg', '.jpeg', '.png', '.webp', '.mp4', '.mov', '.webm'].includes(ext) ? ext : '.jpg';
      cb(null, `${Date.now()}-${Math.round(Math.random() * 1e9)}${safeExt}`);
    }
  }),
  limits: { fileSize: 20 * 1024 * 1024 }
});

async function updateProductReviewStats(sku) {
  const reviewDoc = await mongoose.connection.db.collection('reviews').findOne({ sku: String(sku) });
  const reviews = Array.isArray(reviewDoc?.reviews) ? reviewDoc.reviews : [];
  const validReviews = reviews.filter(review => Number.isFinite(Number(review.rating)));
  const reviewCount = validReviews.length;
  const rating = reviewCount === 0
    ? 0
    : Math.round((validReviews.reduce((sum, review) => sum + Number(review.rating), 0) / reviewCount) * 10) / 10;

  await mongoose.connection.db.collection('products').updateOne(
    { sku: String(sku) },
    { $set: { rating, reviewCount, updatedAt: new Date() } }
  );

  return { rating, reviewCount };
}

async function createReviewNotification(customerId, orderId, carbonPoints) {
  if (!customerId || !orderId || !carbonPoints) return;
  await mongoose.connection.db.collection('notifications').updateOne(
    {
      CustomerID: customerId,
      OrderID: orderId,
      type: 'review',
    },
    {
      $setOnInsert: {
        CustomerID: customerId,
        OrderID: orderId,
        category: 'orders',
        type: 'review',
        title: 'Đánh giá thành công',
        body: `Bạn đã đánh giá đơn hàng #${orderId} và nhận được ${carbonPoints} điểm carbon.`,
        action: 'Xem đánh giá',
        iconText: '★',
        targetType: 'order',
        targetId: orderId,
        isRead: false,
        createdAt: new Date(),
      },
    },
    { upsert: true }
  );
}

// GET /api/reviews/order/:orderId?customer_id=...
router.get('/order/:orderId', asyncHandler(async (req, res) => {
  const orderId = String(req.params.orderId || '').trim();
  const customerId = String(req.query.customer_id || req.query.customerId || '').trim();
  if (!orderId || !customerId) {
    return res.status(400).json({ message: 'orderId and customer_id are required' });
  }

  const reviewDocs = await mongoose.connection.db.collection('reviews')
    .find({ reviews: { $elemMatch: { customer_id: customerId, order_id: orderId } } })
    .toArray();

  const reviewsBySku = {};
  let canEdit = false;
  let latestDeadline = null;

  reviewDocs.forEach((doc) => {
    const sku = String(doc.sku || '').trim();
    if (!sku || !Array.isArray(doc.reviews)) return;
    doc.reviews.forEach((item) => {
      if (String(item.customer_id || '').trim() !== customerId) return;
      if (String(item.order_id || '').trim() !== orderId) return;
      const editable = canEditReview(item);
      if (editable) canEdit = true;
      const deadline = getReviewEditDeadline(item);
      if (deadline && (!latestDeadline || deadline > latestDeadline)) {
        latestDeadline = deadline;
      }
      reviewsBySku[sku] = {
        sku,
        rating: item.rating,
        content: item.content || '',
        images: normalizeReviewImages(req, Array.isArray(item.images) ? item.images : []),
        time: item.time || null,
        // Always compute from current rules to avoid legacy/incorrect stored values.
        carbonPoints: getReviewCarbonPoints(item),
        isPremiumReview: isPremiumReview(item),
        canEdit: editable,
        editDeadline: deadline,
      };
    });
  });

  res.json({
    success: true,
    orderId,
    customerId,
    canEdit,
    editDeadline: latestDeadline,
    reviewsBySku,
  });
}));

// GET /api/reviews/sku/:sku
router.get('/sku/:sku', asyncHandler(async (req, res) => {
  const result = await Review.findOne({ sku: req.params.sku });
  if (!result) {
    return res.status(404).json({ message: 'No reviews found for this SKU' });
  }
  res.json(await enrichReviewDocument(req, result));
}));

/**
 * GET /api/reviews
 * Lấy tất cả các đánh giá từ collection 'reviews' (MongoDB)
 * Mỗi document có cấu trúc: { sku, reviews: [{ fullname, rating, time, ... }] }
 * Trả về mảng phẳng đã sort theo thời gian mới nhất
 */
router.get('/', asyncHandler(async (req, res) => {
  const limit = parseInt(req.query.limit, 10) || 0; // 0 = lấy hết
  const lite = req.query.lite === 'true';
  const now = Date.now();

  const slimReview = (review) => {
    if (!lite) {
      return {
        ...review,
        images: normalizeReviewImages(req, review.images),
      };
    }
    return {
      fullname: review.fullname,
      customer_id: review.customer_id,
      content: review.content,
      rating: review.rating,
      time: review.time,
      order_id: review.order_id,
      sku: review.sku,
      productId: review.productId,
    };
  };

  // Admin dashboard chỉ cần vài review gần đây — dùng aggregation + limit, tránh flatten toàn bộ DB.
  if (limit > 0 && limit <= 50) {
    const recent = await mongoose.connection.db.collection('reviews').aggregate([
      { $unwind: '$reviews' },
      {
        $project: {
          _id: 0,
          fullname: '$reviews.fullname',
          customer_id: '$reviews.customer_id',
          content: '$reviews.content',
          rating: '$reviews.rating',
          time: '$reviews.time',
          order_id: '$reviews.order_id',
          images: lite ? { $literal: [] } : '$reviews.images',
          likes: lite ? { $literal: 0 } : '$reviews.likes',
          replies: lite ? { $literal: [] } : '$reviews.replies',
          sku: 1,
          productId: { $ifNull: ['$productId', '$product_id'] },
        },
      },
      { $sort: { time: -1 } },
      { $limit: limit },
    ]).toArray();
    return res.json(recent.map(slimReview));
  }

  if (reviewsCache && (now - reviewsCacheTime < 30000)) {
    const result = limit > 0 ? reviewsCache.slice(0, limit) : reviewsCache;
    return res.json(result.map(slimReview));
  }

  const reviewDocs = await mongoose.connection.db
    .collection('reviews')
    .find({}, { projection: { sku: 1, productId: 1, product_id: 1, reviews: 1 } })
    .toArray();

  // Flatten tất cả reviews từ mọi document
  const allReviews = [];
  reviewDocs.forEach((doc) => {
    if (doc.reviews && Array.isArray(doc.reviews)) {
      doc.reviews.forEach((review) => {
        allReviews.push({
          ...review,
          sku: doc.sku,
          productId: doc.productId || doc.product_id || null,
        });
      });
    }
  });

  // Sort theo thời gian giảm dần (mới nhất lên đầu)
  allReviews.sort((a, b) => {
    const getTime = (r) => {
      const t = r.time || r.created_at || r.date || r.review_date;
      if (!t) return 0;
      if (t instanceof Date) return t.getTime();
      if (typeof t === 'object' && t.$date) return new Date(t.$date).getTime();
      return new Date(t).getTime();
    };
    return getTime(b) - getTime(a);
  });

  // Lọc ra review hợp lệ (không phải trong tương lai)
  const validReviews = allReviews.filter((r) => {
    const t = r.time || r.created_at || r.date || r.review_date;
    if (!t) return true;
    if (typeof t === 'object' && t.$date) return new Date(t.$date).getTime() <= now;
    return new Date(t).getTime() <= now;
  });

  reviewsCache = validReviews;
  reviewsCacheTime = now;

  const result = limit > 0 ? validReviews.slice(0, limit) : validReviews;

  res.json(result.map(slimReview));
}));

router.post('/uploads/media', mediaUpload.array('media', 8), asyncHandler(async (req, res) => {
  const files = req.files || [];
  const baseUrl = process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`;
  res.status(201).json({
    media: files.map((file) => `${baseUrl}/uploads/reviews/${file.filename}`),
  });
}));

router.post('/', asyncHandler(async (req, res) => {
  const sku = String(req.body.sku || '').trim();
  const customerId = String(req.body.customer_id || req.body.customerId || '').trim();
  const orderId = String(req.body.order_id || req.body.orderId || '').trim();
  const rating = Number(req.body.rating);
  if (!sku || !customerId || !orderId || !Number.isFinite(rating) || rating < 1 || rating > 5) {
    return res.status(400).json({ message: 'sku, customer_id, order_id and valid rating are required' });
  }

  const reviewsCollection = mongoose.connection.db.collection('reviews');
  const existingDoc = await reviewsCollection.findOne({ sku });
  const existingReview = Array.isArray(existingDoc?.reviews)
    ? existingDoc.reviews.find((item) =>
      String(item.customer_id || '').trim() === customerId
      && String(item.order_id || '').trim() === orderId
    )
    : null;

  if (existingReview && !canEditReview(existingReview)) {
    return res.status(403).json({
      message: 'Đánh giá chỉ có thể chỉnh sửa trong vòng 3 ngày kể từ khi gửi',
    });
  }

  const review = {
    fullname: String(req.body.fullname || req.body.fullName || 'Khách hàng VEGGO').trim(),
    customer_id: customerId,
    content: String(req.body.content || '').trim(),
    rating,
    time: existingReview?.time || new Date(),
    images: Array.isArray(req.body.images) ? req.body.images : [],
    likes: Array.isArray(existingReview?.likes) ? existingReview.likes : [],
    order_id: orderId,
    replies: Array.isArray(existingReview?.replies) ? existingReview.replies : [],
    updatedAt: new Date(),
  };
  review.carbonPoints = getReviewCarbonPoints(review);
  review.isPremiumReview = isPremiumReview(review);
  const isUpdate = Boolean(existingReview);
  await reviewsCollection.updateOne(
    { sku },
    { $pull: { reviews: { customer_id: customerId, order_id: orderId } } }
  );
  await reviewsCollection.updateOne(
    { sku },
    {
      $push: { reviews: review },
      $setOnInsert: { sku, createdAt: new Date() },
      $set: { updatedAt: new Date() },
    },
    { upsert: true }
  );
  const reviewStats = await updateProductReviewStats(sku);

  const detail = await mongoose.connection.db.collection('order_details').findOne({ OrderID: orderId });
  const order = await mongoose.connection.db.collection('orders').findOne({ OrderID: orderId });
  const orderItems = Array.isArray(detail?.items) ? detail.items : [];
  const requiredSkus = [...new Set(orderItems.map(item => String(item.sku || '').trim()).filter(Boolean))];
  const reviewDocs = await mongoose.connection.db.collection('reviews')
    .find({ sku: { $in: requiredSkus }, reviews: { $elemMatch: { customer_id: customerId, order_id: orderId } } })
    .toArray();
  const reviewedSkus = new Set();
  reviewDocs.forEach((doc) => {
    if (Array.isArray(doc.reviews) && doc.reviews.some(item =>
      String(item.customer_id || '').trim() === customerId && String(item.order_id || '').trim() === orderId
    )) {
      reviewedSkus.add(String(doc.sku || '').trim());
    }
  });

  let status = order?.status;
  let reviewCarbonPointEarned = 0;
  const carbonPoints = review.carbonPoints;
  const orderCompleted = requiredSkus.length > 0 && reviewedSkus.size >= requiredSkus.length;
  if (orderCompleted) {
    reviewCarbonPointEarned = sumOrderReviewCarbonPoints(
      reviewDocs,
      customerId,
      orderId,
      requiredSkus
    );
    if (status !== 'reviewed') {
      await mongoose.connection.db.collection('orders').updateOne(
        { OrderID: orderId },
        {
          $set: {
            status: 'reviewed',
            paymentStatus: 'paid',
            updatedAt: new Date(),
            'routes.reviewed': new Date(),
          },
        }
      );
      status = 'reviewed';
      await createReviewNotification(customerId, orderId, reviewCarbonPointEarned);
    }
  }

  try {
    await evaluateCustomerCertificate(customerId, orderId);
  } catch (error) {
    console.error('[review certificate evaluation] Failed:', error);
  }

  invalidateReviewsCache();

  res.status(201).json({
    success: true,
    sku,
    orderId,
    status,
    reviewedCount: reviewedSkus.size,
    requiredCount: requiredSkus.length,
    carbonPoints,
    isPremiumReview: review.isPremiumReview,
    isUpdate,
    canEdit: canEditReview(review),
    editDeadline: getReviewEditDeadline(review),
    reviewCarbonPointEarned,
    rating: reviewStats.rating,
    reviewCount: reviewStats.reviewCount,
    review,
  });
}));

router.post('/sku/:sku/reviews/:reviewId/like', asyncHandler(async (req, res) => {
  const sku = String(req.params.sku || '').trim();
  const reviewId = String(req.params.reviewId || '').trim();
  const customerId = String(req.body.customer_id || req.body.customerId || '').trim();

  if (!sku || !reviewId) {
    return res.status(400).json({ message: 'sku and reviewId are required' });
  }
  if (!customerId) {
    return res.status(401).json({ message: 'Vui lòng đăng nhập để đánh dấu hữu ích' });
  }

  const reviewsCollection = mongoose.connection.db.collection('reviews');
  const doc = await reviewsCollection.findOne({ sku });
  if (!doc || !Array.isArray(doc.reviews)) {
    return res.status(404).json({ message: 'No reviews found for this SKU' });
  }

  let found = false;
  const updatedReviews = doc.reviews.map((review) => {
    const currentId = review._id != null ? String(review._id) : '';
    if (currentId !== reviewId) {
      return review;
    }
    found = true;
    const likes = Array.isArray(review.likes) ? [...review.likes] : [];
    const existingIndex = likes.findIndex((id) => String(id) === customerId);
    if (existingIndex >= 0) {
      likes.splice(existingIndex, 1);
    } else {
      likes.push(customerId);
    }
    return { ...review, likes };
  });

  if (!found) {
    return res.status(404).json({ message: 'Review not found' });
  }

  await reviewsCollection.updateOne(
    { sku },
    { $set: { reviews: updatedReviews, updatedAt: new Date() } }
  );

  invalidateReviewsCache();

  const enrichedReviews = await enrichReviewsWithAvatars(req, updatedReviews);
  res.json({ sku, reviews: enrichedReviews });
}));

module.exports = router;
