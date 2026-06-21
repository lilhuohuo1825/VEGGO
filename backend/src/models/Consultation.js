const mongoose = require('mongoose');

const questionSchema = new mongoose.Schema({
  question: { type: String, required: true, trim: true },
  customerId: { type: String, default: 'anonymous' },
  customerName: { type: String, default: 'Khách hàng' },
  answer: { type: String, default: '' },
  answeredBy: { type: String, default: '' },
  answeredAt: { type: Date, default: null },
  status: { type: String, enum: ['pending', 'answered'], default: 'pending' }
}, {
  timestamps: true
});

const consultationSchema = new mongoose.Schema({
  sku: { type: String, required: true, index: true, unique: true },
  productName: { type: String, required: true },
  questions: [questionSchema]
}, {
  timestamps: true,
  collection: 'consultations'
});

module.exports = mongoose.model('Consultation', consultationSchema);
