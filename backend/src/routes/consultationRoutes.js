const express = require('express');
const mongoose = require('mongoose');
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

async function createConsultationAdminNotification({ sku, productName, customerName, customerId, questionId }) {
  await mongoose.connection.db.collection('admin_notifications').insertOne({
    type: 'consultation',
    sku,
    productName,
    customerName,
    customerId,
    questionId,
    title: 'Câu hỏi tư vấn mới',
    message: `Khách hàng ${customerName} đã đặt câu hỏi về sản phẩm "${productName}".`,
    status: 'pending',
    read: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  });
}

async function createConsultationAnswerNotification({ customerId, sku, productName, questionId, questionText }) {
  if (!customerId || customerId === 'anonymous') {
    return;
  }

  await mongoose.connection.db.collection('notifications').insertOne({
    CustomerID: customerId,
    category: 'qa',
    type: 'consultation_answer',
    title: 'Admin đã phản hồi câu hỏi',
    body: `Câu hỏi về sản phẩm "${productName}" đã được trả lời.`,
    action: 'Xem trả lời',
    iconText: '?',
    targetType: 'support',
    targetId: sku,
    sku,
    productName,
    questionId,
    questionText,
    isRead: false,
    createdAt: new Date(),
  });
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
  const customerId = typeof req.body.customerId === 'string' && req.body.customerId.trim()
    ? req.body.customerId.trim()
    : 'anonymous';

  consultation.questions.push({
    question,
    customerName,
    customerId,
    answer: '',
    status: 'pending',
  });

  await consultation.save();
  const createdQuestion = consultation.questions[consultation.questions.length - 1];
  await createConsultationAdminNotification({
    sku: consultation.sku,
    productName: consultation.productName,
    customerName,
    customerId: createdQuestion.customerId,
    questionId: createdQuestion._id.toString(),
  });

  res.status(201).json(sortQuestionsByCreatedAt(consultation));
}));

// POST /api/consultations/:sku/answer/:questionId
router.post('/:sku/answer/:questionId', asyncHandler(async (req, res) => {
  const answer = typeof req.body.answer === 'string' ? req.body.answer.trim() : '';
  const answeredBy = typeof req.body.answeredBy === 'string' && req.body.answeredBy.trim()
    ? req.body.answeredBy.trim()
    : 'Admin';

  if (!answer) {
    return res.status(400).json({ success: false, message: 'Answer cannot be empty' });
  }

  const consultation = await Consultation.findOne({ sku: req.params.sku });
  if (!consultation) {
    return res.status(404).json({ success: false, message: 'No consultations found for this SKU' });
  }

  const question = consultation.questions.id(req.params.questionId);
  if (!question) {
    return res.status(404).json({ success: false, message: 'Question not found' });
  }

  question.answer = answer;
  question.answeredBy = answeredBy;
  question.answeredAt = new Date();
  question.status = 'answered';

  await consultation.save();

  await createConsultationAnswerNotification({
    customerId: question.customerId,
    sku: consultation.sku,
    productName: consultation.productName,
    questionId: question._id.toString(),
    questionText: question.question,
  });

  await mongoose.connection.db.collection('admin_notifications').updateMany(
    {
      type: 'consultation',
      sku: consultation.sku,
      questionId: question._id.toString(),
    },
    {
      $set: {
        status: 'approved',
        read: true,
        updatedAt: new Date(),
      },
    }
  );

  res.json({ success: true, data: sortQuestionsByCreatedAt(consultation) });
}));

// DELETE /api/consultations/:sku/question/:questionId
router.delete('/:sku/question/:questionId', asyncHandler(async (req, res) => {
  const consultation = await Consultation.findOne({ sku: req.params.sku });
  if (!consultation) {
    return res.status(404).json({ success: false, message: 'No consultations found for this SKU' });
  }

  const question = consultation.questions.id(req.params.questionId);
  if (!question) {
    return res.status(404).json({ success: false, message: 'Question not found' });
  }

  question.deleteOne();

  if (consultation.questions.length === 0) {
    await Consultation.deleteOne({ _id: consultation._id });
  } else {
    await consultation.save();
  }

  await mongoose.connection.db.collection('admin_notifications').updateMany(
    {
      type: 'consultation',
      sku: consultation.sku,
      questionId: req.params.questionId,
    },
    {
      $set: {
        status: 'rejected',
        read: true,
        updatedAt: new Date(),
      },
    }
  );

  res.json({ success: true });
}));

module.exports = router;
