const mongoose = require('mongoose');

const productSchema = new mongoose.Schema(
  {
    name: { type: String, required: true, trim: true },
    sku: { type: String, unique: true, required: true, index: true },
    product_name: { type: String, required: true, trim: true },
    categoryId: { type: String, index: true },
    subcategoryId: { type: String, index: true },
    description: { type: String, default: '' },
    price: { type: Number, required: true, min: 0 },
    originalPrice: { type: Number, default: 0 },
    unit: { type: String, default: 'kg' },
    image: [{ type: String }], // Mảng các URL ảnh
    WeightOptions: [{ type: Number }], // Ví dụ: [0.25, 0.5, 1]
    CarbonSavingPoint: { type: Number, default: 0 },
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

productSchema.pre('validate', function syncProductNames(next) {
  if (!this.name && this.product_name) {
    this.name = this.product_name;
  }
  if (!this.product_name && this.name) {
    this.product_name = this.name;
  }
  next();
});

module.exports = mongoose.model('Product', productSchema);
