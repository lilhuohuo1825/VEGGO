const mongoose = require('mongoose');

const replySchema = new mongoose.Schema({
  customerId: { type: String, default: '' },
  customerName: { type: String, default: 'Khách hàng' },
  customerAvatarUrl: { type: String, default: '' },
  content: { type: String, required: true, trim: true },
  isAdmin: { type: Boolean, default: false },
}, {
  timestamps: true,
});

const helpfulLikeSchema = new mongoose.Schema({
  customerId: { type: String, required: true, trim: true },
  customerName: { type: String, default: 'Khách hàng' },
}, {
  _id: false,
});

const questionSchema = new mongoose.Schema({
  question: { type: String, required: true, trim: true },
  customerId: { type: String, default: 'anonymous' },
  customerName: { type: String, default: 'Khách hàng' },
  customerAvatarUrl: { type: String, default: '' },
  answer: { type: String, default: '' },
  answeredBy: { type: String, default: '' },
  answeredAt: { type: Date, default: null },
  status: { type: String, enum: ['pending', 'answered'], default: 'pending' },
  helpfulLikes: { type: [helpfulLikeSchema], default: [] },
  replies: { type: [replySchema], default: [] },
}, {
  timestamps: true,
});

const consultationSchema = new mongoose.Schema({
  sku: { type: String, required: true, index: true, unique: true },
  productName: { type: String, required: true },
  questions: [questionSchema],
}, {
  timestamps: true,
  collection: 'consultations',
});

module.exports = mongoose.model('Consultation', consultationSchema);
