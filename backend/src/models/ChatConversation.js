const mongoose = require('mongoose');

const chatMessageSchema = new mongoose.Schema(
  {
    role: {
      type: String,
      enum: ['user', 'assistant', 'system'],
      required: true,
    },
    content: { type: String, required: true },
    intent: { type: String, default: null },
    metadata: { type: mongoose.Schema.Types.Mixed, default: null },
  },
  { timestamps: true }
);

const chatConversationSchema = new mongoose.Schema(
  {
    customerId: { type: String, required: true, index: true },
    title: { type: String, default: 'Cuộc trò chuyện mới', trim: true },
    messages: [chatMessageSchema],
    isActive: { type: Boolean, default: true, index: true },
  },
  { timestamps: true, collection: 'chat_conversations' }
);

chatConversationSchema.index({ customerId: 1, updatedAt: -1 });

module.exports = mongoose.model('ChatConversation', chatConversationSchema);
