const mongoose = require('mongoose');

const promotionTargetSchema = new mongoose.Schema(
  {
    promotion_id: { type: String, required: true, index: true },
    target_type: { type: String, required: true }, // e.g., Category, Subcategory, Brand, Product
    target_ref: [{ type: String }], // List of IDs (CategoryIDs, SKUs, etc.)
  },
  { timestamps: true }
);

module.exports = mongoose.model('PromotionTarget', promotionTargetSchema);
