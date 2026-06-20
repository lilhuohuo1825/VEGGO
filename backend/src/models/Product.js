const mongoose = require('mongoose');

/**
 * Product schema – chỉ define các field cần thiết cho filter/sort và hiển thị.
 * strict: false giữ lại toàn bộ field gốc từ MongoDB (ingredients, storage, producer…)
 * mà không cần khai báo hết.
 */
const productSchema = new mongoose.Schema(
  {
    // --- Identity ---
    product_name: { type: String, trim: true },
    brand:        { type: String, default: '' },
    sku:          { type: String, default: '' },

    // --- Pricing ---
    price:      { type: Number, required: true, min: 0 },
    base_price: { type: Number, default: 0, min: 0 },

    // --- Display ---
    image:  { type: [String], default: [] },
    unit:   { type: String, default: '' },
    weight: { type: String, default: '' },
    stock:  { type: Number, default: 0, min: 0 },

    // --- Status (thay cho isActive) ---
    status: { type: String, default: 'Active', index: true },

    // --- Sort fields (cần index) ---
    purchase_count: { type: Number, default: 0, index: true },
    liked:          { type: Number, default: 0 },
    rating:         { type: Number, default: 0, index: true },
    post_date:      { type: mongoose.Schema.Types.Mixed, index: true },

    // --- Categories ---
    CategoryID:    { type: String, index: true },
    SubcategoryID: { type: String, index: true },
  },
  {
    strict: false,   // Giữ lại toàn bộ field gốc từ DB mà không khai báo hết
    timestamps: false,
  }
);

module.exports = mongoose.model('Product', productSchema);
