const express = require('express');
const chatController = require('../controllers/chatController');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.post('/message', asyncHandler(chatController.sendMessage));
router.get('/conversations/:customerId', asyncHandler(chatController.listConversations));
router.get('/conversations/:customerId/:conversationId', asyncHandler(chatController.getConversation));
router.delete('/conversations/:customerId/:conversationId', asyncHandler(chatController.deleteConversation));

module.exports = router;
