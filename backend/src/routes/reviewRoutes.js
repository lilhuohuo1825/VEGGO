const express = require('express');
const Review = require('../models/Reviews');
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

// GET /api/reviews
router.get('/', asyncHandler(async (req, res) => {
  const allReviews = await Review.find({});
  res.json(allReviews);
}));

module.exports = router;
