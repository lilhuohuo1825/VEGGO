const mongoose = require('mongoose');

const productSchema = new mongoose.Schema(
  {
    name: { type: String, required: true, trim: true },
    sku: { type: String, index: true, unique: true },
    categoryId: { type: String, index: true },
    subcategoryId: { type: String, index: true },
    description: { type: String, default: '' },
    price: { type: Number, required: true, min: 0 },
    originalPrice: { type: Number, default: 0 },
    unit: { type: String, default: 'kg' },
    imageUrl: { type: String, default: '' },
    stock: { type: Number, default: 0, min: 0 },
    rating: { type: Number, default: 0 },
    reviewCount: { type: Number, default: 0 },
    soldCount: { type: Number, default: 0 },
    weight: { type: String, default: '' },
    isActive: { type: Boolean, default: true },
    origin: String,
    condition: String,
    fatContent: String
  },
  { timestamps: true }
);

module.exports = mongoose.model('Product', productSchema);
