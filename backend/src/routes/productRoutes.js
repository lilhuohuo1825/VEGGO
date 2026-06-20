const express = require('express');
const mongoose = require('mongoose');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// ─────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────

const VALID_TABS = ['popular', 'trending', 'newest', 'best_price', 'price_desc', 'price_asc', 'top_rated'];
const DEFAULT_TAB = 'popular';
const HOME_LIMIT = 24;

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

/**
 * Trả về aggregation pipeline hoặc sort object tương ứng với tab.
 *
 * Tab         Logic
 * ─────────── ──────────────────────────────────────────────────────
 * popular     score = purchase_count + liked + (rating * 100)  DESC
 * trending    purchase_count DESC
 * newest      post_date DESC
 * top_rated   rating DESC → purchase_count DESC
 * best_price  price ASC
 */
function getPipelineByTab(tab, limit, skip = 0) {
  const baseMatch = { $match: { status: 'Active' } };
  const skipStage = { $skip: skip };
  const limitStage = { $limit: limit };

  switch (tab) {
    case 'popular':
      // Tính score tổng hợp: purchase_count + liked + rating*100
      // rating (0-5) × 100 để cân bằng với purchase_count/liked
      return [
        baseMatch,
        {
          $addFields: {
            popularScore: {
              $add: [
                { $ifNull: ['$purchase_count', 0] },
                { $ifNull: ['$liked', 0] },
                { $multiply: [{ $ifNull: ['$rating', 0] }, 100] },
              ],
            },
          },
        },
        { $sort: { popularScore: -1 } },
        skipStage,
        limitStage,
        { $project: { popularScore: 0 } },
      ];

    case 'trending':
      return [baseMatch, { $sort: { purchase_count: -1 } }, skipStage, limitStage];

    case 'newest':
      return [baseMatch, { $sort: { post_date: -1 } }, skipStage, limitStage];

    case 'top_rated':
      return [baseMatch, { $sort: { rating: -1, purchase_count: -1 } }, skipStage, limitStage];

    case 'best_price':
    case 'price_desc':
      return [baseMatch, { $sort: { price: -1 } }, skipStage, limitStage];

    case 'price_asc':
      return [baseMatch, { $sort: { price: 1 } }, skipStage, limitStage];

    default:
      return [baseMatch, { $sort: { purchase_count: -1 } }, skipStage, limitStage];
  }
}

// ─────────────────────────────────────────────
// GET /api/products/home?tab=popular
// Dành riêng cho Trang chủ – limit 25, chỉ Active
// ─────────────────────────────────────────────
router.get('/home', asyncHandler(async (req, res) => {
  const rawTab = req.query.tab;
  const tab = VALID_TABS.includes(rawTab) ? rawTab : DEFAULT_TAB;
  
  const limit = parseInt(req.query.limit) || HOME_LIMIT;
  const skip = parseInt(req.query.skip) || 0;

  const pipeline = getPipelineByTab(tab, limit, skip);

  const products = await Product.aggregate(pipeline);

  res.json({
    success: true,
    message: 'Products retrieved successfully',
    count: products.length,
    tab,
    data: products,
  });
}));

// ─────────────────────────────────────────────
// GET /api/products          – Lấy toàn bộ sản phẩm
// GET /api/products/:id      – Chi tiết 1 sản phẩm
// POST /api/products         – Tạo sản phẩm mới
// ─────────────────────────────────────────────

router.get('/', asyncHandler(async (req, res) => {
  const products = await Product.find({ status: 'Active' }).sort({ post_date: -1 });
  res.json(products);
}));

router.get('/:id', asyncHandler(async (req, res) => {
  if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
    return res.status(400).json({ message: 'Invalid product id' });
  }
  const product = await Product.findById(req.params.id);
  if (!product) return res.status(404).json({ message: 'Product not found' });
  res.json(product);
}));

router.post('/', asyncHandler(async (req, res) => {
  const product = await Product.create(req.body);
  res.status(201).json(product);
}));

module.exports = router;
