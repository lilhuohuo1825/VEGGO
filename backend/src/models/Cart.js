const mongoose = require('mongoose');

const cartItemSchema = new mongoose.Schema(
  {
    sku: { type: String, required: true },
    quantity: { type: Number, required: true, min: 1 },
    selectedWeight: { type: Number, required: true }, // Lưu 0.25, 0.5, 1
  },
  { _id: false }
);

const cartSchema = new mongoose.Schema(
  {
    customerId: { type: String, required: true, index: true, unique: true }, // CUS900025
    items: [cartItemSchema],
  },
  { timestamps: true }
);

module.exports = mongoose.model('Cart', cartSchema);
