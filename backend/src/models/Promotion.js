const mongoose = require('mongoose');

const bannerDataSchema = new mongoose.Schema(
  {
    imageUrl: { type: String, default: '' },
    showOnApp: { type: Boolean, default: false },
  },
  { _id: false }
);

const promotionSchema = new mongoose.Schema(
  {
    promotion_id: { type: String, index: true },
    code: { type: String, trim: true, unique: true, sparse: true },
    name: { type: String, default: '' },
    discount: { type: Number, default: 0, min: 0 },
    description: { type: String, default: '' },
    banner_data: { type: bannerDataSchema, default: undefined },
    isActive: { type: Boolean, default: true },
  },
  { collection: 'promotions', strict: false, timestamps: true }
);

module.exports = mongoose.model('Promotion', promotionSchema);
