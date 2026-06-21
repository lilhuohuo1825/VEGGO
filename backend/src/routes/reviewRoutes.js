const express = require('express');
const Review = require('../models/Reviews');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

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

module.exports = router;
