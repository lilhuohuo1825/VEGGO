const mongoose = require('mongoose');

const PriceForecastSchema = new mongoose.Schema(
  {
    productId: { type: String, ref: 'Product', required: true, unique: true },
    sku: { type: String, index: true },
    currentPrice: { type: Number, required: true },
    predictedPrice7Days: { type: Number, required: true },
    changePercent: { type: Number, required: true }, // e.g., 15.3 for +15.3%, -12.4 for -12.4%
    trend: { type: String, enum: ['up', 'down', 'stable'], required: true },
    reason: { type: String, required: true }, // Explanation e.g., "Mưa lớn bão lụt tại Lâm Đồng"
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.model('PriceForecast', PriceForecastSchema);
