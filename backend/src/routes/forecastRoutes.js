const express = require('express');
const asyncHandler = require('../middleware/asyncHandler');
const PriceForecast = require('../models/PriceForecast');
const Product = require('../models/Product');

const router = express.Router();

/**
 * GET /api/forecast/:productId
 * Returns the cached price prediction for a product
 */
router.get('/:productId', asyncHandler(async (req, res) => {
  const { productId } = req.params;
  
  // Check if product exists
  const product = await Product.findById(productId);
  if (!product) {
    return res.status(404).json({
      success: false,
      message: 'Product not found'
    });
  }
  
  // Retrieve forecast from DB cache
  const forecast = await PriceForecast.findOne({ productId }).populate('productId', 'name product_name image price');
  
  if (!forecast) {
    // Return a default baseline if no forecast exists yet (cold start)
    return res.json({
      success: true,
      message: 'No forecast data available yet for this product',
      data: {
        productId: product._id,
        currentPrice: product.price,
        predictedPrice7Days: product.price,
        changePercent: 0,
        trend: 'stable',
        reason: 'Giá dự kiến ổn định trong những ngày tới.'
      }
    });
  }
  
  res.json({
    success: true,
    message: 'Forecast retrieved successfully',
    data: forecast
  });
}));

module.exports = router;
