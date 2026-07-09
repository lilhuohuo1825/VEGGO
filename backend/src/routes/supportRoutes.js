const express = require('express');
const asyncHandler = require('../middleware/asyncHandler');
const SupportConversation = require('../models/SupportConversation');
const SupportMessage = require('../models/SupportMessage');
const User = require('../models/User');
const { verifyAccessToken } = require('../utils/jwt');
const { formatMessageDto } = require('../sockets/supportSocket');

const router = express.Router();

function getAuthFromRequest(req) {
  const authHeader = req.headers.authorization || '';
  const token = String(authHeader).replace(/^Bearer\s+/i, '').trim();
  if (!token) return null;
  try {
    return verifyAccessToken(token);
  } catch (e) {
    return null;
  }
}

async function ensureConversationForCustomer(customerId) {
  const existing = await SupportConversation.findOne({ customerId }).lean();
  if (existing) return existing;
  const created = await SupportConversation.create({ customerId, status: 'open' });
  return created.toObject();
}

function statusLabel(status) {
  return status === 'closed' ? 'Đã đóng' : 'Đang mở';
}

function resolveCustomerAvatar(req, avatarUrl) {
  const value = String(avatarUrl || '').trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  const base = (process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`).replace(/\/$/, '');
  return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
}

async function enrichConversationsForAdmin(req, conversations) {
  const customerIds = [...new Set(conversations.map((c) => c.customerId).filter(Boolean))];
  const users = customerIds.length
    ? await User.find({ CustomerID: { $in: customerIds } })
      .select('CustomerID FullName name avatarUrl Phone')
      .lean()
    : [];
  const userByCustomerId = new Map(users.map((u) => [u.CustomerID, u]));

  return conversations.map((c) => {
    const user = userByCustomerId.get(c.customerId);
    const customerName = user?.FullName || user?.name || c.customerId;
    return {
      ...c,
      _id: String(c._id),
      customerName,
      customerAvatar: resolveCustomerAvatar(req, user?.avatarUrl || ''),
      customerPhone: user?.Phone || user?.phone || '',
      statusLabel: statusLabel(c.status),
    };
  });
}

function serializeConversation(convo) {
  return {
    ...convo,
    _id: String(convo._id),
    statusLabel: statusLabel(convo.status),
  };
}

// GET /api/support/conversations
router.get('/conversations', asyncHandler(async (req, res) => {
  const auth = getAuthFromRequest(req);
  if (!auth) return res.status(401).json({ success: false, message: 'Unauthorized' });

  if (auth.type === 'user') {
    const customerId = String(req.query.customerId || auth.customerId || '').trim();
    if (!customerId || customerId !== auth.customerId) {
      return res.status(403).json({ success: false, message: 'Forbidden' });
    }
    const convo = await ensureConversationForCustomer(customerId);
    return res.json({ success: true, data: [serializeConversation(convo)] });
  }

  const limit = Math.min(parseInt(req.query.limit || '50', 10) || 50, 200);
  const data = await SupportConversation.find({})
    .sort({ lastMessageAt: -1, updatedAt: -1 })
    .limit(limit)
    .lean();
  const enriched = await enrichConversationsForAdmin(req, data);
  res.json({ success: true, data: enriched });
}));

// GET /api/support/conversations/:id/messages?before=...&limit=...
router.get('/conversations/:id/messages', asyncHandler(async (req, res) => {
  const auth = getAuthFromRequest(req);
  if (!auth) return res.status(401).json({ success: false, message: 'Unauthorized' });

  const conversationId = String(req.params.id || '').trim();
  const convo = await SupportConversation.findById(conversationId).lean();
  if (!convo) return res.status(404).json({ success: false, message: 'Conversation not found' });

  if (auth.type === 'user' && convo.customerId !== auth.customerId) {
    return res.status(403).json({ success: false, message: 'Forbidden' });
  }

  const limit = Math.min(parseInt(req.query.limit || '30', 10) || 30, 200);
  const before = String(req.query.before || '').trim();
  const query = { conversationId: convo._id };
  if (before) {
    const beforeDate = new Date(before);
    if (!Number.isNaN(beforeDate.getTime())) {
      query.createdAt = { $lt: beforeDate };
    }
  }

  const messages = await SupportMessage.find(query)
    .sort({ createdAt: -1 })
    .limit(limit)
    .lean();

  const data = messages.reverse().map((m) => formatMessageDto(m, convo._id));
  res.json({ success: true, data });
}));

// POST /api/support/conversations/:id/mark-read
router.post('/conversations/:id/mark-read', asyncHandler(async (req, res) => {
  const auth = getAuthFromRequest(req);
  if (!auth) return res.status(401).json({ success: false, message: 'Unauthorized' });

  const conversationId = String(req.params.id || '').trim();
  const convo = await SupportConversation.findById(conversationId).lean();
  if (!convo) return res.status(404).json({ success: false, message: 'Conversation not found' });

  if (auth.type === 'user') {
    if (convo.customerId !== auth.customerId) {
      return res.status(403).json({ success: false, message: 'Forbidden' });
    }
    await SupportConversation.updateOne(
      { _id: conversationId },
      { $set: { unreadCountUser: 0 } }
    );
  } else if (auth.type === 'admin') {
    await SupportConversation.updateOne(
      { _id: conversationId },
      { $set: { unreadCountAdmin: 0 } }
    );
  } else {
    return res.status(403).json({ success: false, message: 'Forbidden' });
  }
  res.json({ success: true });
}));

// POST /api/support/conversations/:id/close (admin only)
router.post('/conversations/:id/close', asyncHandler(async (req, res) => {
  const auth = getAuthFromRequest(req);
  if (!auth) return res.status(401).json({ success: false, message: 'Unauthorized' });
  if (auth.type !== 'admin') return res.status(403).json({ success: false, message: 'Forbidden' });

  const conversationId = String(req.params.id || '').trim();
  await SupportConversation.updateOne(
    { _id: conversationId },
    { $set: { status: 'closed' } }
  );
  res.json({ success: true });
}));

module.exports = router;
