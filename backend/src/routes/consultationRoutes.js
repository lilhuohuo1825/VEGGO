const express = require('express');
const Consultation = require('../models/Consultation');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

function sortQuestionsByCreatedAt(consultation) {
  if (!consultation || !consultation.questions) {
    return consultation;
  }
  consultation.questions.sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );
  return consultation;
}

// GET /api/consultations
router.get('/', asyncHandler(async (req, res) => {
  const allConsultations = await Consultation.find({});
  allConsultations.forEach(sortQuestionsByCreatedAt);
  res.json(allConsultations);
}));

// GET /api/consultations/:sku
router.get('/:sku', asyncHandler(async (req, res) => {
  const result = await Consultation.findOne({ sku: req.params.sku });
  if (!result) {
    return res.status(404).json({ message: 'No consultations found for this SKU' });
  }
  res.json(sortQuestionsByCreatedAt(result));
}));

// POST /api/consultations/:sku/questions
router.post('/:sku/questions', asyncHandler(async (req, res) => {
  const question = typeof req.body.question === 'string' ? req.body.question.trim() : '';

  if (!question) {
    return res.status(400).json({ message: 'Question cannot be empty' });
  }

  let consultation = await Consultation.findOne({ sku: req.params.sku });
  if (!consultation) {
    const product = await Product.findOne({ sku: req.params.sku });
    const productName = typeof req.body.productName === 'string' && req.body.productName.trim()
      ? req.body.productName.trim()
      : (product?.name || 'Sản phẩm không xác định');

    consultation = new Consultation({
      sku: req.params.sku,
      productName,
      questions: [],
    });
  }

  const customerName = typeof req.body.customerName === 'string' && req.body.customerName.trim()
    ? req.body.customerName.trim()
    : 'Khách hàng';

  consultation.questions.push({
    question,
    customerName,
    customerId: 'anonymous',
    answer: '',
    status: 'pending',
  });

  await consultation.save();
  res.status(201).json(sortQuestionsByCreatedAt(consultation));
}));

module.exports = router;
