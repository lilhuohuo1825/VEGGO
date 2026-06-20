const mongoose = require('mongoose');

const productSchema = new mongoose.Schema(
  {
    sku: { type: String, unique: true, required: true, index: true },
    product_name: { type: String, required: true, trim: true },
    categoryId: { type: String, index: true },
    description: { type: String, default: '' },
    price: { type: Number, required: true, min: 0 },
    unit: { type: String, default: 'kg' },
    image: [{ type: String }], // Mảng các URL ảnh
    WeightOptions: [{ type: Number }], // Ví dụ: [0.25, 0.5, 1]
    CarbonSavingPoint: { type: Number, default: 0 },
    stock: { type: Number, default: 0, min: 0 },
    isActive: { type: Boolean, default: true },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Product', productSchema);
