const mongoose = require('mongoose');

const supportConversationSchema = new mongoose.Schema(
  {
    customerId: { type: String, required: true, index: true, unique: true },
    status: { type: String, enum: ['open', 'closed'], default: 'open', index: true },
    lastMessageAt: { type: Date, default: null, index: true },
    lastMessageText: { type: String, default: '' },
    unreadCountUser: { type: Number, default: 0 },
    unreadCountAdmin: { type: Number, default: 0 },
  },
  { timestamps: true, collection: 'support_conversations' }
);

supportConversationSchema.index({ status: 1, lastMessageAt: -1 });

module.exports = mongoose.model('SupportConversation', supportConversationSchema);

