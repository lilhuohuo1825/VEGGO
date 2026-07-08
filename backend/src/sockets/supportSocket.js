const SupportConversation = require('../models/SupportConversation');
const SupportMessage = require('../models/SupportMessage');

function normalizeText(value) {
  return String(value || '').trim();
}

function formatMessageDto(messageDoc, convoId) {
  return {
    id: String(messageDoc._id),
    conversationId: String(convoId),
    senderType: messageDoc.senderType,
    senderId: messageDoc.senderId,
    text: messageDoc.text,
    attachments: messageDoc.attachments || [],
    clientMessageId: messageDoc.clientMessageId,
    createdAt: messageDoc.createdAt,
  };
}

async function ensureConversationForCustomer(customerId) {
  const normalized = normalizeText(customerId);
  if (!normalized) throw new Error('Missing customerId');

  const existing = await SupportConversation.findOne({ customerId: normalized }).lean();
  if (existing) return existing;

  const created = await SupportConversation.create({
    customerId: normalized,
    status: 'open',
    lastMessageAt: null,
    lastMessageText: '',
    unreadCountUser: 0,
    unreadCountAdmin: 0,
  });
  return created.toObject();
}

function createSupportSocket(io) {
  io.on('connection', (socket) => {
    const auth = socket.data && socket.data.auth ? socket.data.auth : null;

    if (auth && auth.type === 'admin') {
      socket.join('support:admin');
    }

    socket.on('conversation:join', async (payload, cb) => {
      try {
        const conversationId = normalizeText(payload && payload.conversationId);
        const requestedCustomerId = normalizeText(payload && payload.customerId);

        let convo = null;

        if (auth && auth.type === 'user') {
          const customerId = auth.customerId;
          convo = await ensureConversationForCustomer(customerId);
          await SupportConversation.updateOne(
            { _id: convo._id },
            { $set: { unreadCountUser: 0 } }
          );
          convo.unreadCountUser = 0;
        } else if (auth && auth.type === 'admin') {
          if (conversationId) {
            convo = await SupportConversation.findById(conversationId).lean();
          } else if (requestedCustomerId) {
            convo = await ensureConversationForCustomer(requestedCustomerId);
          }
        }

        if (!convo) {
          const err = new Error('Unauthorized or conversation not found');
          err.status = 403;
          throw err;
        }

        const room = `support:${String(convo._id)}`;
        socket.join(room);
        if (typeof cb === 'function') cb({ ok: true, conversationId: String(convo._id) });
      } catch (err) {
        if (typeof cb === 'function') cb({ ok: false, message: err.message || 'Join failed' });
      }
    });

    socket.on('message:send', async (payload, cb) => {
      try {
        const conversationId = normalizeText(payload && payload.conversationId);
        const text = normalizeText(payload && payload.text);
        const clientMessageId = normalizeText(payload && payload.clientMessageId) || null;

        if (!conversationId) return cb && cb({ ok: false, message: 'Missing conversationId' });
        if (!text) return cb && cb({ ok: false, message: 'Missing text' });

        const convo = await SupportConversation.findById(conversationId).lean();
        if (!convo) return cb && cb({ ok: false, message: 'Conversation not found' });

        if (!auth) return cb && cb({ ok: false, message: 'Unauthorized' });
        if (auth.type === 'user' && convo.customerId !== auth.customerId) {
          return cb && cb({ ok: false, message: 'Forbidden' });
        }

        const messageDoc = await SupportMessage.create({
          conversationId: convo._id,
          senderType: auth.type === 'admin' ? 'admin' : 'user',
          senderId: auth.type === 'admin' ? auth.adminId : auth.customerId,
          text,
          attachments: [],
          clientMessageId,
          deliveredAt: new Date(),
          readAt: null,
        });

        const now = new Date();
        const update = {
          lastMessageAt: now,
          lastMessageText: text,
        };
        if (auth.type === 'user') {
          update.unreadCountAdmin = (Number(convo.unreadCountAdmin || 0) || 0) + 1;
        } else {
          update.unreadCountUser = (Number(convo.unreadCountUser || 0) || 0) + 1;
        }
        await SupportConversation.updateOne({ _id: convo._id }, { $set: update });

        const dto = formatMessageDto(messageDoc, convo._id);

        const room = `support:${String(convo._id)}`;
        io.to(room).emit('message:new', dto);
        io.to('support:admin').emit('conversation:updated', {
          conversationId: String(convo._id),
          customerId: convo.customerId,
          lastMessageAt: now,
          lastMessageText: text,
          unreadCountAdmin: update.unreadCountAdmin ?? convo.unreadCountAdmin ?? 0,
          unreadCountUser: update.unreadCountUser ?? convo.unreadCountUser ?? 0,
          status: convo.status,
        });

        if (typeof cb === 'function') cb({ ok: true, message: dto });

        socket.to(room).emit('typing:update', {
          conversationId: String(convo._id),
          senderType: auth.type === 'admin' ? 'admin' : 'user',
          isTyping: false,
        });
      } catch (err) {
        if (typeof cb === 'function') cb({ ok: false, message: err.message || 'Send failed' });
      }
    });

    socket.on('typing:update', async (payload) => {
      try {
        const conversationId = normalizeText(payload && payload.conversationId);
        const isTyping = Boolean(payload && payload.isTyping);
        if (!conversationId || !auth) return;

        const convo = await SupportConversation.findById(conversationId).lean();
        if (!convo) return;
        if (auth.type === 'user' && convo.customerId !== auth.customerId) return;

        const room = `support:${conversationId}`;
        socket.to(room).emit('typing:update', {
          conversationId,
          senderType: auth.type === 'admin' ? 'admin' : 'user',
          isTyping,
        });
      } catch (err) {
        // ignore typing errors
      }
    });
  });
}

module.exports = {
  createSupportSocket,
  formatMessageDto,
};
