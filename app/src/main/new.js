const mongoose = require('mongoose');

const reviewSchema = new mongoose.Schema({
  sku: { type: String, required: true, index: true },
  reviews: [{
    fullname: String,
    content: String,
    rating: Number,
    time: Date,
    images: [String]
  }]
}, { collection: 'reviews' }); // Explicitly use your existing 'reviews' collection

module.exports = mongoose.model('Review', reviewSchema);