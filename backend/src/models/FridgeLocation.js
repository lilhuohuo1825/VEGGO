const mongoose = require('mongoose');

const fridgeLocationSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    locationCode: { type: String, required: true },
    name: { type: String, required: true, trim: true },
  },
  {
    timestamps: true,
  }
);

// Optional: ensure unique locationCode per user
fridgeLocationSchema.index({ userId: 1, locationCode: 1 }, { unique: true });

module.exports = mongoose.model('FridgeLocation', fridgeLocationSchema);
