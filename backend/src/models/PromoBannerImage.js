const mongoose = require('mongoose');

const promoBannerImageSchema = new mongoose.Schema(
  {
    token: { type: String, required: true, unique: true, index: true },
    mimeType: { type: String, default: 'image/png' },
    data: { type: String, required: true },
  },
  { collection: 'promoBannerImages', strict: false, timestamps: true }
);

module.exports = mongoose.model('PromoBannerImage', promoBannerImageSchema);
