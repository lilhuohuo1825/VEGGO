const chatService = require('../services/chatService');

exports.sendMessage = async (req, res) => {
  const { customerId, message, conversationId } = req.body;

  const result = await chatService.processMessage({
    customerId,
    message,
    conversationId,
  });

  res.json({
    success: true,
    data: result,
  });
};

exports.listConversations = async (req, res) => {
  const { customerId } = req.params;
  const limit = Number(req.query.limit) || 20;

  const conversations = await chatService.listConversations(customerId, limit);

  res.json({
    success: true,
    data: conversations,
  });
};

exports.getConversation = async (req, res) => {
  const { customerId, conversationId } = req.params;

  const conversation = await chatService.getConversationHistory(customerId, conversationId);

  res.json({
    success: true,
    data: conversation,
  });
};

exports.deleteConversation = async (req, res) => {
  const { customerId, conversationId } = req.params;

  const result = await chatService.deleteConversation(customerId, conversationId);

  res.json({
    success: true,
    data: result,
  });
};
