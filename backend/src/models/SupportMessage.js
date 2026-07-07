const mongoose = require('mongoose');

const supportAttachmentSchema = new mongoose.Schema(
  {
    type: { type: String, default: 'file' },
    url: { type: String, required: true },
    name: { type: String, default: null },
    mimeType: { type: String, default: null },
    size: { type: Number, default: null },
  },
  { _id: false }
);

const supportMessageSchema = new mongoose.Schema(
  {
    conversationId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'SupportConversation',
      required: true,
      index: true,
    },
    senderType: { type: String, enum: ['user', 'admin'], required: true, index: true },
    senderId: { type: String, required: true, index: true },
    text: { type: String, required: true, trim: true },
    attachments: { type: [supportAttachmentSchema], default: [] },
    clientMessageId: { type: String, default: null, index: true },
    deliveredAt: { type: Date, default: null },
    readAt: { type: Date, default: null },
  },
  { timestamps: true, collection: 'support_messages' }
);

supportMessageSchema.index({ conversationId: 1, createdAt: 1 });

module.exports = mongoose.model('SupportMessage', supportMessageSchema);

