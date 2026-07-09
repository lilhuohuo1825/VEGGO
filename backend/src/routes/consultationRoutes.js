const express = require('express');
const mongoose = require('mongoose');
const Consultation = require('../models/Consultation');
const Product = require('../models/Product');
const User = require('../models/User');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();
const ADMIN_DISPLAY_NAME = 'VEGGO Admin';

function sortQuestionsByCreatedAt(consultation) {
  if (!consultation || !consultation.questions) {
    return consultation;
  }
  consultation.questions.sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );
  return consultation;
}

async function findUserByCustomerId(customerId) {
  if (!customerId || customerId === 'anonymous') {
    return null;
  }
  return User.findOne({ CustomerID: customerId }).select('FullName avatarUrl name').lean();
}

function resolvePublicBaseUrl(req) {
  if (req) {
    return (process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`).replace(/\/$/, '');
  }
  return String(process.env.PUBLIC_BASE_URL || '').replace(/\/$/, '');
}

function toAbsoluteAvatarUrl(req, avatarUrl) {
  const value = String(avatarUrl || '').trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  const base = resolvePublicBaseUrl(req);
  if (!base) return value;
  return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
}

async function resolveAvatarUrl(req, customerId, fallbackUrl) {
  const trimmedFallback = typeof fallbackUrl === 'string' ? fallbackUrl.trim() : '';
  if (trimmedFallback) {
    return toAbsoluteAvatarUrl(req, trimmedFallback);
  }
  const user = await findUserByCustomerId(customerId);
  return toAbsoluteAvatarUrl(req, user?.avatarUrl || '');
}

function sanitizeQuestion(question) {
  const plain = question.toObject ? question.toObject() : { ...question };
  if (plain.answer && plain.answer.trim()) {
    plain.answeredBy = ADMIN_DISPLAY_NAME;
  }
  plain.helpfulCount = Array.isArray(plain.helpfulLikes) ? plain.helpfulLikes.length : 0;
  if (Array.isArray(plain.replies)) {
    plain.replies = plain.replies.map((reply) => ({
      ...reply,
      customerName: reply.isAdmin ? ADMIN_DISPLAY_NAME : (reply.customerName || 'Khách hàng'),
      customerAvatarUrl: reply.isAdmin ? '' : (reply.customerAvatarUrl || ''),
    }));
  }
  return plain;
}

async function enrichConsultation(consultation, req) {
  if (!consultation) {
    return consultation;
  }
  const plain = consultation.toObject ? consultation.toObject() : { ...consultation };
  if (!Array.isArray(plain.questions)) {
    return plain;
  }

  const enrichedQuestions = [];
  for (const question of plain.questions) {
    const sanitized = sanitizeQuestion(question);
    sanitized.customerAvatarUrl = await resolveAvatarUrl(
      req,
      sanitized.customerId,
      sanitized.customerAvatarUrl || ''
    );
    if (Array.isArray(sanitized.replies)) {
      for (const reply of sanitized.replies) {
        if (!reply.isAdmin) {
          reply.customerAvatarUrl = await resolveAvatarUrl(
            req,
            reply.customerId,
            reply.customerAvatarUrl || ''
          );
        }
      }
    }
    enrichedQuestions.push(sanitized);
  }
  plain.questions = enrichedQuestions;
  return plain;
}

async function consultationResponse(consultation, req) {
  const sorted = sortQuestionsByCreatedAt(consultation);
  return enrichConsultation(sorted, req);
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
    title: `${ADMIN_DISPLAY_NAME} đã phản hồi câu hỏi`,
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

async function createConsultationLikeNotification({
  customerId,
  sku,
  productName,
  questionId,
  questionText,
  likerName,
}) {
  if (!customerId || customerId === 'anonymous') {
    return;
  }

  await mongoose.connection.db.collection('notifications').insertOne({
    CustomerID: customerId,
    category: 'qa',
    type: 'consultation_like',
    title: 'Câu hỏi của bạn được đánh dấu hữu ích',
    body: `${likerName} thấy câu hỏi về "${productName}" rất hữu ích.`,
    action: 'Xem câu hỏi',
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

async function createConsultationReplyNotification({
  customerId,
  sku,
  productName,
  questionId,
  questionText,
  replierName,
}) {
  if (!customerId || customerId === 'anonymous') {
    return;
  }

  await mongoose.connection.db.collection('notifications').insertOne({
    CustomerID: customerId,
    category: 'qa',
    type: 'consultation_reply',
    title: 'Có phản hồi mới cho câu hỏi của bạn',
    body: `${replierName} đã trả lời câu hỏi về "${productName}".`,
    action: 'Xem phản hồi',
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
  const enriched = [];
  for (const item of allConsultations) {
    enriched.push(await consultationResponse(item, req));
  }
  res.json(enriched);
}));

// GET /api/consultations/:sku
router.get('/:sku', asyncHandler(async (req, res) => {
  const result = await Consultation.findOne({ sku: req.params.sku });
  if (!result) {
    return res.json({
      sku: req.params.sku,
      productName: '',
      questions: [],
    });
  }
  res.json(await consultationResponse(result, req));
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
    : '';

  if (!customerId || customerId === 'anonymous') {
    return res.status(401).json({ message: 'Vui lòng đăng nhập để gửi câu hỏi tư vấn' });
  }

  const customerAvatarUrl = await resolveAvatarUrl(req, customerId, req.body.customerAvatarUrl);

  consultation.questions.push({
    question,
    customerName,
    customerId,
    customerAvatarUrl,
    answer: '',
    status: 'pending',
    helpfulLikes: [],
    replies: [],
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

  res.status(201).json(await consultationResponse(consultation, req));
}));

// POST /api/consultations/:sku/questions/:questionId/like
router.post('/:sku/questions/:questionId/like', asyncHandler(async (req, res) => {
  const customerId = typeof req.body.customerId === 'string' ? req.body.customerId.trim() : '';
  const customerName = typeof req.body.customerName === 'string' && req.body.customerName.trim()
    ? req.body.customerName.trim()
    : 'Khách hàng';

  if (!customerId || customerId === 'anonymous') {
    return res.status(401).json({ message: 'Vui lòng đăng nhập để đánh dấu hữu ích' });
  }

  const consultation = await Consultation.findOne({ sku: req.params.sku });
  if (!consultation) {
    return res.status(404).json({ success: false, message: 'No consultations found for this SKU' });
  }

  const question = consultation.questions.id(req.params.questionId);
  if (!question) {
    return res.status(404).json({ success: false, message: 'Question not found' });
  }

  if (!Array.isArray(question.helpfulLikes)) {
    question.helpfulLikes = [];
  }

  const existingIndex = question.helpfulLikes.findIndex((like) => like.customerId === customerId);
  let liked = false;
  if (existingIndex >= 0) {
    question.helpfulLikes.splice(existingIndex, 1);
  } else {
    question.helpfulLikes.push({ customerId, customerName });
    liked = true;
    if (question.customerId && question.customerId !== customerId) {
      await createConsultationLikeNotification({
        customerId: question.customerId,
        sku: consultation.sku,
        productName: consultation.productName,
        questionId: question._id.toString(),
        questionText: question.question,
        likerName: customerName,
      });
    }
  }

  await consultation.save();
  res.json(await consultationResponse(consultation, req));
}));

// POST /api/consultations/:sku/questions/:questionId/replies
router.post('/:sku/questions/:questionId/replies', asyncHandler(async (req, res) => {
  const content = typeof req.body.content === 'string' ? req.body.content.trim() : '';
  const customerId = typeof req.body.customerId === 'string' ? req.body.customerId.trim() : '';
  const customerName = typeof req.body.customerName === 'string' && req.body.customerName.trim()
    ? req.body.customerName.trim()
    : 'Khách hàng';

  if (!content) {
    return res.status(400).json({ success: false, message: 'Reply cannot be empty' });
  }
  if (!customerId || customerId === 'anonymous') {
    return res.status(401).json({ message: 'Vui lòng đăng nhập để trả lời' });
  }

  const consultation = await Consultation.findOne({ sku: req.params.sku });
  if (!consultation) {
    return res.status(404).json({ success: false, message: 'No consultations found for this SKU' });
  }

  const question = consultation.questions.id(req.params.questionId);
  if (!question) {
    return res.status(404).json({ success: false, message: 'Question not found' });
  }

  if (question.customerId && question.customerId === customerId) {
    return res.status(400).json({ success: false, message: 'Không thể trả lời câu hỏi của chính bạn' });
  }

  const customerAvatarUrl = await resolveAvatarUrl(req, customerId, req.body.customerAvatarUrl);
  if (!Array.isArray(question.replies)) {
    question.replies = [];
  }

  question.replies.push({
    customerId,
    customerName,
    customerAvatarUrl,
    content,
    isAdmin: false,
  });

  await consultation.save();

  if (question.status === 'pending') {
    await createConsultationAdminNotification({
      sku: consultation.sku,
      productName: consultation.productName,
      customerName: `${customerName} (phản hồi)`,
      customerId,
      questionId: question._id.toString(),
    });
  }

  if (question.customerId && question.customerId !== customerId) {
    await createConsultationReplyNotification({
      customerId: question.customerId,
      sku: consultation.sku,
      productName: consultation.productName,
      questionId: question._id.toString(),
      questionText: question.question,
      replierName: customerName,
    });
  }

  res.status(201).json(await consultationResponse(consultation, req));
}));

// POST /api/consultations/:sku/answer/:questionId
router.post('/:sku/answer/:questionId', asyncHandler(async (req, res) => {
  const answer = typeof req.body.answer === 'string' ? req.body.answer.trim() : '';

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
  question.answeredBy = ADMIN_DISPLAY_NAME;
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

  res.json({ success: true, data: await consultationResponse(consultation, req) });
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
