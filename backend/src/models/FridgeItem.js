const mongoose = require('mongoose');

const fridgeItemSchema = new mongoose.Schema(
  {
    userId: { type: String, required: false, default: '', index: true },
    sku: { type: String, default: '' },
    orderId: { type: String, default: '' },
    name: { type: String, required: true, trim: true },
    image: { type: String, default: '' },
    images: { type: [String], default: [] },
    quantity: { type: Number, required: true, min: 0 },
    unit: { type: String, default: '' },
    source: {
      type: String,
      enum: ['manual', 'scan', 'history'],
      required: true,
    },
    locationCode: { type: String, default: '' },
    remindBeforeExpiry: { type: Boolean, default: true },
    purchaseDate: { type: Date, required: true },
    expiryDate: { type: Date, required: true },
  },
  {
    timestamps: true,
    collection: 'fridge_items'
  }
);

module.exports = mongoose.model('FridgeItem', fridgeItemSchema);
