const mongoose = require('mongoose');

const promotionUsageSchema = new mongoose.Schema(
  {
    promotion_id: { type: String, required: true, index: true },
    user_id: [{ type: String }], // List of CustomerIDs who used it
    order_id: [{ type: String }], // List of OrderIDs where it was used
  },
  { timestamps: true }
);

module.exports = mongoose.model('PromotionUsage', promotionUsageSchema);
