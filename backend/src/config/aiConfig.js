const INTENTS = {
  HEALTH_ADVICE: 'HEALTH_ADVICE',
  RECIPE_SUGGESTION: 'RECIPE_SUGGESTION',
  NUTRITION_CALCULATION: 'NUTRITION_CALCULATION',
  PRODUCT_SEARCH: 'PRODUCT_SEARCH',
  GENERAL_CHAT: 'GENERAL_CHAT',
};

// Model lite: quota free tier cao hơn, rẻ hơn
const DEFAULT_GEMINI_MODEL = 'gemini-2.0-flash-lite';
const DEFAULT_GEMINI_VISION_MODEL = 'gemini-2.5-flash';

// Các model cũ không hỗ trợ generateContent vision (gemini-pro, 1.5-flash...)
const UNSUPPORTED_VISION_MODELS = new Set([
  'gemini-pro',
  'gemini-1.5-flash',
  'gemini-1.5-flash-latest',
  'gemini-1.5-pro',
]);

const MODEL_FALLBACK_CHAIN = [
  process.env.GEMINI_MODEL,
  DEFAULT_GEMINI_MODEL,
  'gemini-2.0-flash',
  'gemini-2.5-flash',
  'gemini-2.5-flash-lite',
].filter(Boolean).filter((model, index, list) => list.indexOf(model) === index);

// Scan ảnh: ưu tiên gemini-2.5-flash như nhánh feature/payment (bỏ qua GEMINI_MODEL nếu không hỗ trợ vision)
const VISION_MODEL_FALLBACK_CHAIN = [
  process.env.GEMINI_VISION_MODEL,
  DEFAULT_GEMINI_VISION_MODEL,
  'gemini-2.5-flash-lite',
  'gemini-2.0-flash',
  'gemini-2.0-flash-lite',
]
  .filter(Boolean)
  .filter((model, index, list) => list.indexOf(model) === index)
  .filter((model) => !UNSUPPORTED_VISION_MODELS.has(model));

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
  DEFAULT_GEMINI_VISION_MODEL,
  MODEL_FALLBACK_CHAIN,
  VISION_MODEL_FALLBACK_CHAIN,
};
