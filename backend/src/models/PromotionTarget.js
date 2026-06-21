const mongoose = require('mongoose');

const promotionTargetSchema = new mongoose.Schema(
  {
    promotion_id: { type: String, required: true },
    target_type: { type: String, required: true },
    target_ref: { type: [String], required: true },
  },
  { collection: 'promotion_targets', timestamps: true, strict: false }
);

module.exports = mongoose.model('PromotionTarget', promotionTargetSchema);
