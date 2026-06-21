const mongoose = require('mongoose');

const reviewItemSchema = new mongoose.Schema({
  fullname: { type: String, required: true },
  customer_id: String,
  content: { type: String, default: '' },
  rating: { type: Number, required: true, min: 1, max: 5 },
  time: { type: Date, default: Date.now },
  images: [String],
  likes: [String],
  order_id: String,
  replies: [mongoose.Schema.Types.Mixed]
});

const productReviewsSchema = new mongoose.Schema({
  sku: { type: String, required: true, index: true, unique: true },
  reviews: [reviewItemSchema]
}, {
  timestamps: true,
  collection: 'reviews'
});

module.exports = mongoose.model('Review', productReviewsSchema);
