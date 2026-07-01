const ChatConversation = require('../models/ChatConversation');
const User = require('../models/User');
const geminiService = require('./ai/geminiService');
const productService = require('./productService');
const healthService = require('./healthService');
const recipeService = require('./recipeService');
const nutritionService = require('./nutritionService');
const { INTENTS, AI_CONFIG } = require('../config/aiConfig');
const { enforceRateLimit } = require('../utils/chatRateLimiter');
const { detectIntentByRules } = require('../utils/intentRules');
const {
  buildTemplateResponse,
  canBuildTemplateResponse,
} = require('../utils/chatResponseTemplates');
const {
  validateChatInput,
  buildConversationTitle,
  toHistoryMessages,
  createEmptyContext,
  markServiceUsed,
  trimContextForGemini,
  buildAssistantMetadata,
  buildProcessMessageResponse,
  resolveSearchQuery,
  resolveRecipeIngredient,
  resolveNameList,
} = require('../utils/chatHelpers');

/**
 * ChatService – orchestrator trung tâm của chatbot VEGGO.
 *
 * Flow:
 * 1. Nhận message
 * 2. Gửi message sang Gemini phân tích yêu cầu
 * 3. Gemini trả intent + parameters
 * 4. ChatService quyết định gọi Product/Health/Recipe/Nutrition Service
 * 5. Các service lấy dữ liệu từ MongoDB
 * 6. Gửi message gốc + context data sang Gemini
 * 7. Gemini sinh câu trả lời tự nhiên
 * 8. Lưu ChatConversation
 * 9. Trả response
 *
 * Gemini tuyệt đối KHÔNG truy cập MongoDB.
 */

async function loadUserContext(customerId) {
  const user = await User.findOne({ CustomerID: customerId })
    .select({ CustomerID: 1, FullName: 1, name: 1, tastePreferences: 1, CustomerTiering: 1 })
    .lean();

  if (!user) {
    return { customerId, fullName: '', tastePreferences: null, customerTier: '' };
  }

  return {
    customerId,
    fullName: user.FullName || user.name || '',
    tastePreferences: user.tastePreferences || null,
    customerTier: user.CustomerTiering || '',
  };
}

async function getOrCreateConversation(customerId, conversationId) {
  if (conversationId) {
    const existing = await ChatConversation.findOne({
      _id: conversationId,
      customerId,
      isActive: true,
    });

    if (existing) {
      return existing;
    }
  }

  return new ChatConversation({ customerId, messages: [] });
}

async function fetchHealthAdviceContext(context, parameters) {
  const healthGoal = parameters.healthGoal
    || parameters.searchQuery
    || 'vitamin_boost';

  markServiceUsed(context, 'HealthService');
  context.healthAdvice = await healthService.getProductsForGoal(healthGoal, {
    limit: AI_CONFIG.maxContextProducts,
  });
  context.products = context.healthAdvice.products || [];
}

async function fetchRecipeContext(context, parameters, customerId, userMessage = '') {
  markServiceUsed(context, 'RecipeService');

  if (parameters.useCart) {
    markServiceUsed(context, 'ProductService');
    const result = await recipeService.findRecipesFromCart(customerId, {
      limit: AI_CONFIG.maxContextRecipes,
    });
    context.products = result.products || [];
    context.recipes = result.recipes || [];
    return;
  }

  const ingredient = resolveRecipeIngredient(parameters, userMessage);
  if (ingredient) {
    const result = await recipeService.findRelatedRecipesByProductName(ingredient, {
      limit: AI_CONFIG.maxContextRecipes,
    });
    context.recipeIngredient = ingredient;
    context.recipes = result.recipes || [];

    const products = await productService.getProductsByNames([ingredient], {
      limit: AI_CONFIG.maxContextProducts,
    });
    if (products.length) {
      markServiceUsed(context, 'ProductService');
      context.products = products;
    }
    return;
  }

  const names = resolveNameList(parameters);
  if (names.length) {
    markServiceUsed(context, 'ProductService');
    const products = await productService.getProductsByNames(names, {
      limit: AI_CONFIG.maxContextProducts,
    });
    const result = await recipeService.findRecipesFromProducts(products, {
      limit: AI_CONFIG.maxContextRecipes,
    });
    context.products = result.products || products;
    context.recipes = result.recipes || [];
    return;
  }

  const searchQuery = resolveSearchQuery(parameters, userMessage);
  if (searchQuery) {
    const result = await recipeService.findRelatedRecipesByProductName(searchQuery, {
      limit: AI_CONFIG.maxContextRecipes,
    });
    context.recipes = result.recipes || [];
    return;
  }

  markServiceUsed(context, 'ProductService');
  const result = await recipeService.findRecipesFromCart(customerId, {
    limit: AI_CONFIG.maxContextRecipes,
  });
  context.products = result.products || [];
  context.recipes = result.recipes || [];
}

async function fetchNutritionContext(context, parameters, customerId) {
  markServiceUsed(context, 'NutritionService');

  if (parameters.mealItems?.length) {
    context.nutrition = await nutritionService.calculateFromItems(parameters.mealItems);
    return;
  }

  if (parameters.useCart) {
    markServiceUsed(context, 'ProductService');
    context.nutrition = await nutritionService.calculateFromCart(customerId);
    context.products = await productService.getProductsFromCart(customerId);
    return;
  }

  const names = resolveNameList(parameters);
  if (names.length) {
    markServiceUsed(context, 'ProductService');
    context.nutrition = await nutritionService.calculateFromProductNames(names);
    context.products = await productService.getProductsByNames(names, {
      limit: AI_CONFIG.maxContextProducts,
    });
    return;
  }

  const searchQuery = resolveSearchQuery(parameters);
  if (searchQuery) {
    markServiceUsed(context, 'ProductService');
    context.nutrition = await nutritionService.calculateFromProductNames([searchQuery]);
    context.products = await productService.getProductsByNames([searchQuery], {
      limit: AI_CONFIG.maxContextProducts,
    });
  }
}

async function fetchProductSearchContext(context, parameters, customerId, userMessage = '') {
  markServiceUsed(context, 'ProductService');

  const searchQuery = resolveSearchQuery(parameters, userMessage);
  if (searchQuery) {
    context.products = await productService.searchProducts(searchQuery, {
      limit: AI_CONFIG.maxContextProducts,
    });
    return;
  }

  if (parameters.useCart) {
    context.products = await productService.getProductsFromCart(customerId);
  }
}

/**
 * Bước 4–5: Quyết định service cần gọi và lấy dữ liệu từ MongoDB.
 */
async function fetchDomainContext(analysis, customerId) {
  const { intent, parameters = {} } = analysis;
  const context = createEmptyContext(intent);
  context.user = await loadUserContext(customerId);

  switch (intent) {
    case INTENTS.HEALTH_ADVICE:
      await fetchHealthAdviceContext(context, parameters);
      break;

    case INTENTS.RECIPE_SUGGESTION:
      await fetchRecipeContext(context, parameters, customerId, analysis.rawMessage);
      break;

    case INTENTS.NUTRITION_CALCULATION:
      await fetchNutritionContext(context, parameters, customerId);
      break;

    case INTENTS.PRODUCT_SEARCH:
      await fetchProductSearchContext(context, parameters, customerId, analysis.rawMessage);
      break;

    case INTENTS.GENERAL_CHAT:
    default:
      break;
  }

  return context;
}

async function saveConversationTurn({
  conversation,
  userMessage,
  reply,
  analysis,
  context,
}) {
  conversation.messages.push({
    role: 'user',
    content: userMessage,
  });

  conversation.messages.push({
    role: 'assistant',
    content: reply,
    intent: analysis.intent,
    metadata: buildAssistantMetadata(analysis, context),
  });

  if (!conversation.title || conversation.title === 'Cuộc trò chuyện mới') {
    conversation.title = buildConversationTitle(userMessage);
  }

  await conversation.save();
  return conversation;
}

async function resolveIntent(message, history) {
  if (AI_CONFIG.useRuleBasedIntent) {
    const ruleResult = detectIntentByRules(message);
    if (ruleResult && ruleResult.confidence >= 0.75) {
      return ruleResult;
    }
  }

  return geminiService.analyzeIntent(message, history);
}

async function resolveReply({ message, analysis, domainContext, history }) {
  const canUseTemplate = canBuildTemplateResponse(
    analysis.intent,
    domainContext,
    message
  );

  if (!AI_CONFIG.useGeminiForResponses && canUseTemplate) {
    return {
      reply: buildTemplateResponse(analysis.intent, domainContext, message),
      responseSource: 'template',
    };
  }

  if (analysis.intent === INTENTS.GENERAL_CHAT && canUseTemplate) {
    return {
      reply: buildTemplateResponse(analysis.intent, domainContext, message),
      responseSource: 'template',
    };
  }

  try {
    const reply = await geminiService.generateResponse({
      userMessage: message,
      intent: analysis.intent,
      contextData: trimContextForGemini(domainContext),
      conversationHistory: history,
    });
    return { reply, responseSource: 'gemini' };
  } catch (error) {
    if (geminiService.isQuotaExceeded(error) && canUseTemplate) {
      return {
        reply: buildTemplateResponse(analysis.intent, domainContext, message),
        responseSource: 'template-fallback',
      };
    }
    throw error;
  }
}

/**
 * Entry point – điều phối toàn bộ flow chatbot.
 */
async function processMessage({ customerId, message, conversationId }) {
  const input = validateChatInput({ customerId, message });
  enforceRateLimit(input.customerId);

  const conversation = await getOrCreateConversation(input.customerId, conversationId);
  const history = toHistoryMessages(conversation);

  const analysis = await resolveIntent(input.message, history);
  analysis.rawMessage = input.message;
  const domainContext = await fetchDomainContext(analysis, input.customerId);
  const { reply, responseSource } = await resolveReply({
    message: input.message,
    analysis,
    domainContext,
    history,
  });

  analysis.responseSource = responseSource;

  await saveConversationTurn({
    conversation,
    userMessage: input.message,
    reply,
    analysis,
    context: domainContext,
  });

  return buildProcessMessageResponse({
    conversationId: conversation._id.toString(),
    reply,
    analysis,
    context: domainContext,
  });
}

async function listConversations(customerId, limit = 20) {
  const conversations = await ChatConversation.find({ customerId, isActive: true })
    .sort({ updatedAt: -1 })
    .limit(limit)
    .select('_id title createdAt updatedAt messages')
    .lean();

  return conversations.map((conversation) => ({
    id: conversation._id.toString(),
    title: conversation.title,
    createdAt: conversation.createdAt,
    updatedAt: conversation.updatedAt,
    messageCount: conversation.messages?.length || 0,
    lastMessage: conversation.messages?.length
      ? conversation.messages[conversation.messages.length - 1].content
      : '',
  }));
}

async function getConversationHistory(customerId, conversationId) {
  const conversation = await ChatConversation.findOne({
    _id: conversationId,
    customerId,
    isActive: true,
  }).lean();

  if (!conversation) {
    const error = new Error('Không tìm thấy cuộc trò chuyện');
    error.status = 404;
    throw error;
  }

  return {
    id: conversation._id.toString(),
    title: conversation.title,
    createdAt: conversation.createdAt,
    updatedAt: conversation.updatedAt,
    messages: (conversation.messages || []).map((entry) => ({
      role: entry.role,
      content: entry.content,
      intent: entry.intent || null,
      metadata: entry.metadata || null,
      createdAt: entry.createdAt,
    })),
  };
}

async function deleteConversation(customerId, conversationId) {
  const conversation = await ChatConversation.findOneAndUpdate(
    { _id: conversationId, customerId, isActive: true },
    { $set: { isActive: false } },
    { new: true }
  );

  if (!conversation) {
    const error = new Error('Không tìm thấy cuộc trò chuyện');
    error.status = 404;
    throw error;
  }

  return { success: true };
}

module.exports = {
  processMessage,
  listConversations,
  getConversationHistory,
  deleteConversation,
  fetchDomainContext,
};
