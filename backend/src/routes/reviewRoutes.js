const express = require('express');
const Review = require('../models/Reviews');
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const asyncHandler = require('../middleware/asyncHandler');
const { evaluateCustomerCertificate } = require('../services/certificateService');

const router = express.Router();

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

// GET /api/reviews/sku/:sku
router.get('/sku/:sku', asyncHandler(async (req, res) => {
  const result = await Review.findOne({ sku: req.params.sku });
  if (!result) {
    return res.status(404).json({ message: 'No reviews found for this SKU' });
  }
  res.json(result);
}));

/**
 * GET /api/reviews
 * Lấy tất cả các đánh giá từ collection 'reviews' (MongoDB)
 * Mỗi document có cấu trúc: { sku, reviews: [{ fullname, rating, time, ... }] }
 * Trả về mảng phẳng đã sort theo thời gian mới nhất
 */
router.get('/', asyncHandler(async (req, res) => {
  const limit = parseInt(req.query.limit) || 0; // 0 = lấy hết

  const reviewDocs = await mongoose.connection.db
    .collection('reviews')
    .find({})
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
  const now = Date.now();
  const validReviews = allReviews.filter((r) => {
    const t = r.time || r.created_at || r.date || r.review_date;
    if (!t) return true;
    if (typeof t === 'object' && t.$date) return new Date(t.$date).getTime() <= now;
    return new Date(t).getTime() <= now;
  });

  const result = limit > 0 ? validReviews.slice(0, limit) : validReviews;

  res.json(result);
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

  const review = {
    fullname: String(req.body.fullname || req.body.fullName || 'Khách hàng VEGGO').trim(),
    customer_id: customerId,
    content: String(req.body.content || '').trim(),
    rating,
    time: new Date(),
    images: Array.isArray(req.body.images) ? req.body.images : [],
    likes: [],
    order_id: orderId,
    replies: [],
  };

  const result = await Review.findOneAndUpdate(
    { sku },
    {
      $pull: { reviews: { customer_id: customerId, order_id: orderId } },
      $setOnInsert: { sku },
    },
    { upsert: true, new: true }
  );

  result.reviews.push(review);
  await result.save();
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
  if (requiredSkus.length > 0 && reviewedSkus.size >= requiredSkus.length) {
    reviewCarbonPointEarned = requiredSkus.length * 2;
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

  try {
    await evaluateCustomerCertificate(customerId, orderId);
  } catch (error) {
    console.error('[review certificate evaluation] Failed:', error);
  }

  res.status(201).json({
    success: true,
    sku,
    orderId,
    status,
    reviewedCount: reviewedSkus.size,
    requiredCount: requiredSkus.length,
    reviewCarbonPointEarned,
    rating: reviewStats.rating,
    reviewCount: reviewStats.reviewCount,
    review,
  });
}));

module.exports = router;
