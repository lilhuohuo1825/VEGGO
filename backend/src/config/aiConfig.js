const INTENTS = {
  HEALTH_ADVICE: 'HEALTH_ADVICE',
  RECIPE_SUGGESTION: 'RECIPE_SUGGESTION',
  NUTRITION_CALCULATION: 'NUTRITION_CALCULATION',
  PRODUCT_SEARCH: 'PRODUCT_SEARCH',
  GENERAL_CHAT: 'GENERAL_CHAT',
};

// Model lite: quota free tier cao hơn, rẻ hơn
const DEFAULT_GEMINI_MODEL = 'gemini-2.0-flash-lite';

const MODEL_FALLBACK_CHAIN = [
  process.env.GEMINI_MODEL,
  DEFAULT_GEMINI_MODEL,
  'gemini-2.0-flash',
  'gemini-1.5-flash',
].filter(Boolean).filter((model, index, list) => list.indexOf(model) === index);

const useGeminiForResponses = String(process.env.CHAT_USE_GEMINI_RESPONSE || 'false')
  .trim()
  .toLowerCase() === 'true';

const AI_CONFIG = {
  model: process.env.GEMINI_MODEL || DEFAULT_GEMINI_MODEL,
  modelFallbacks: MODEL_FALLBACK_CHAIN,
  useGeminiForResponses,
  useRuleBasedIntent: true,
  maxRetries: 2,
  retryDelayMs: 3000,
  quotaRetryDelayMs: 8000,
  maxHistoryMessages: 4,
  maxContextProducts: 5,
  maxContextRecipes: 3,
  minRequestIntervalMs: Number(process.env.CHAT_MIN_INTERVAL_MS) || 4000,
  maxRequestsPerWindow: Number(process.env.CHAT_MAX_REQUESTS_PER_MIN) || 6,
  rateLimitWindowMs: 60_000,
};

module.exports = {
  INTENTS,
  AI_CONFIG,
  DEFAULT_GEMINI_MODEL,
  MODEL_FALLBACK_CHAIN,
};
