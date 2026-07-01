const { AI_CONFIG } = require('../config/aiConfig');
const { extractProductSearchTerms } = require('./searchQueryExtractor');
const { extractRecipeIngredientTerms } = require('./recipeQueryExtractor');

function validateChatInput({ customerId, message }) {
  const trimmedMessage = String(message || '').trim();

  if (!trimmedMessage) {
    const error = new Error('Tin nhắn không được để trống');
    error.status = 400;
    throw error;
  }

  if (!String(customerId || '').trim()) {
    const error = new Error('customerId là bắt buộc');
    error.status = 400;
    throw error;
  }

  return {
    customerId: String(customerId).trim(),
    message: trimmedMessage,
  };
}

function buildConversationTitle(message) {
  const trimmed = String(message || '').trim();
  if (!trimmed) return 'Cuộc trò chuyện mới';
  return trimmed.length > 60 ? `${trimmed.slice(0, 57)}...` : trimmed;
}

function toHistoryMessages(conversation) {
  return (conversation?.messages || []).map((entry) => ({
    role: entry.role,
    content: entry.content,
  }));
}

function createEmptyContext(intent) {
  return {
    intent,
    user: null,
    products: [],
    recipes: [],
    nutrition: null,
    healthAdvice: null,
    servicesUsed: [],
  };
}

function markServiceUsed(context, serviceName) {
  if (!context.servicesUsed.includes(serviceName)) {
    context.servicesUsed.push(serviceName);
  }
}

function trimContextForGemini(context) {
  return {
    intent: context.intent,
    products: (context.products || []).slice(0, AI_CONFIG.maxContextProducts).map((product) => ({
      name: product.name,
      sku: product.sku,
      price: product.price,
      unit: product.unit,
      origin: product.origin,
    })),
    recipes: (context.recipes || []).slice(0, AI_CONFIG.maxContextRecipes).map((recipe) => ({
      title: recipe.title,
      cookingTime: recipe.cookingTime,
      matchedIngredientCount: recipe.matchedIngredientCount,
    })),
    nutrition: context.nutrition?.totals || null,
    healthAdvice: context.healthAdvice
      ? {
          goalLabel: context.healthAdvice.goalLabel,
          tips: context.healthAdvice.tips,
        }
      : null,
  };
}

function buildAssistantMetadata(analysis, context) {
  return {
    confidence: analysis.confidence,
    intent: analysis.intent,
    intentSource: analysis.source || 'unknown',
    responseSource: analysis.responseSource || 'unknown',
    parameters: analysis.parameters || {},
    servicesUsed: context.servicesUsed || [],
    hasProducts: (context.products || []).length > 0,
    hasRecipes: (context.recipes || []).length > 0,
    hasNutrition: Boolean(context.nutrition),
    hasHealthAdvice: Boolean(context.healthAdvice),
  };
}

function buildProcessMessageResponse({
  conversationId,
  reply,
  analysis,
  context,
}) {
  return {
    conversationId,
    reply,
    intent: analysis.intent,
    confidence: analysis.confidence,
    servicesUsed: context.servicesUsed,
    suggestedProducts: (context.products || []).slice(0, 5),
    suggestedRecipes: (context.recipes || []).slice(0, 3),
    nutritionSummary: context.nutrition?.totals || null,
  };
}

function resolveRecipeIngredient(parameters = {}, fallbackMessage = '') {
  const raw = String(
    (parameters.ingredients || []).join(' ')
    || (parameters.productNames || []).join(' ')
    || parameters.searchQuery
    || fallbackMessage
  ).trim();

  return extractRecipeIngredientTerms(raw) || raw;
}

function resolveSearchQuery(parameters = {}, fallbackMessage = '') {
  const raw = String(
    parameters.searchQuery
    || (parameters.productNames || []).join(' ')
    || (parameters.ingredients || []).join(' ')
    || fallbackMessage
  ).trim();

  return extractProductSearchTerms(raw) || raw;
}

function resolveNameList(parameters = {}) {
  return [
    ...(parameters.productNames || []),
    ...(parameters.ingredients || []),
  ].map((name) => String(name || '').trim()).filter(Boolean);
}

module.exports = {
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
};
