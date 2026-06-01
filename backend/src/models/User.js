const mongoose = require('mongoose');

const addressSchema = new mongoose.Schema(
  {
    receiverName: String,
    phone: String,
    line1: String,
    ward: String,
    district: String,
    city: String,
    isDefault: { type: Boolean, default: false },
  },
  { _id: false }
);

const userSchema = new mongoose.Schema(
  {
    firebaseUid: { type: String, required: true, unique: true, index: true },
    name: { type: String, default: '' },
    email: { type: String, default: '' },
    phone: { type: String, default: '' },
    avatarUrl: { type: String, default: '' },
    addresses: [addressSchema],
  },
  { timestamps: true }
);

module.exports = mongoose.model('User', userSchema);
