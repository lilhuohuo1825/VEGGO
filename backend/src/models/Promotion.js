const mongoose = require('mongoose');

const promotionSchema = new mongoose.Schema(
  {
    promotion_id: { type: String, unique: true, required: true, index: true },
    code: { type: String, required: true, unique: true },
    name: { type: String, required: true },
    description: { type: String },
    type: { type: String }, // e.g., User, Admin
    scope: { type: String }, // e.g., Order, Category, Brand, Shipping
    discount_type: { type: String }, // e.g., percent, fixed, buy1get1
    discount_value: { type: Number, default: 0 },
    max_discount_value: { type: Number, default: 0 },
    min_order_value: { type: Number, default: 0 },
    usage_limit: { type: Number, default: 0 },
    user_limit: { type: Number, default: 0 },
    is_first_order_only: { type: Boolean, default: false },
    start_date: { type: Date },
    end_date: { type: Date },
    status: { type: String, default: 'Active' },
    created_by: { type: String },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Promotion', promotionSchema);
