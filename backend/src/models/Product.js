const mongoose = require('mongoose');

/**
 * Product schema – chỉ define các field cần thiết cho filter/sort và hiển thị.
 * strict: false giữ lại toàn bộ field gốc từ MongoDB (ingredients, storage, producer…)
 * mà không cần khai báo hết.
 */
const productSchema = new mongoose.Schema(
  {
    _id: { type: String },
    // --- Identity ---
    name: { type: String, trim: true },
    product_name: { type: String, trim: true },
    brand: { type: String, default: '' },
    sku: { type: String, unique: true, sparse: true, index: true },

    // --- Categories ---
    categoryId: { type: String, index: true },
    subcategoryId: { type: String, index: true },
    CategoryID: { type: String, index: true },
    SubcategoryID: { type: String, index: true },

    // --- Product content ---
    description: { type: String, default: '' },
    price: { type: Number, required: true, min: 0 },
    originalPrice: { type: Number, default: 0 },
    base_price: { type: Number, default: 0, min: 0 },
    unit: { type: String, default: 'kg' },
    weight: { type: String, default: '' },
    image: [{ type: String }], // Mảng các URL ảnh
    WeightOptions: [{ type: Number }], // Ví dụ: [0.25, 0.5, 1]
    CarbonSavingPoint: { type: Number, default: 0 },
    stock: { type: Number, default: 0, min: 0 },

    // --- Rating and sort fields ---
    rating: { type: Number, default: 0 },
    reviewCount: { type: Number, default: 0 },
    soldCount: { type: Number, default: 0 },
    purchase_count: { type: Number, default: 0, index: true },
    liked: { type: Number, default: 0 },
    post_date: { type: mongoose.Schema.Types.Mixed, index: true },

    // --- Status ---
    isActive: { type: Boolean, default: true },
    status: { type: String, default: 'Active', index: true },
    origin: String,
    condition: String,
    fatContent: String
  },
  {
    strict: false,   // Giữ lại toàn bộ field gốc từ DB mà không khai báo hết
    timestamps: false,
  }
);

productSchema.pre('validate', function syncProductNames(next) {
  if (!this.name && this.product_name) {
    this.name = this.product_name;
  }
  if (!this.product_name && this.name) {
    this.product_name = this.name;
  }
  if (!this.CategoryID && this.categoryId) {
    this.CategoryID = this.categoryId;
  }
  if (!this.categoryId && this.CategoryID) {
    this.categoryId = this.CategoryID;
  }
  if (!this.SubcategoryID && this.subcategoryId) {
    this.SubcategoryID = this.subcategoryId;
  }
  if (!this.subcategoryId && this.SubcategoryID) {
    this.subcategoryId = this.SubcategoryID;
  }
  if (!this.base_price && this.originalPrice) {
    this.base_price = this.originalPrice;
  }
  if (!this.originalPrice && this.base_price) {
    this.originalPrice = this.base_price;
  }
  if (!this.purchase_count && this.soldCount) {
    this.purchase_count = this.soldCount;
  }
  if (!this.soldCount && this.purchase_count) {
    this.soldCount = this.purchase_count;
  }
  next();
});

productSchema.index({ status: 1, purchase_count: -1 });
productSchema.index({ CategoryID: 1, status: 1, purchase_count: -1 });
productSchema.index({ categoryId: 1, status: 1, purchase_count: -1 });
productSchema.index({ SubcategoryID: 1, status: 1, purchase_count: -1 });
productSchema.index({ subcategoryId: 1, status: 1, purchase_count: -1 });
productSchema.index(
  {
    product_name: 'text',
    name: 'text',
    brand: 'text',
    ingredients: 'text',
    description: 'text',
  },
  {
    weights: {
      product_name: 10,
      name: 10,
      brand: 3,
      ingredients: 5,
      description: 2,
    },
    name: 'product_text_search',
  }
);

module.exports = mongoose.model('Product', productSchema);
