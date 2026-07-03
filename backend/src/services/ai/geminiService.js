const { GoogleGenAI } = require('@google/genai');
const { AI_CONFIG, INTENTS } = require('../../config/aiConfig');
const { INTENT_ANALYSIS_PROMPT, RESPONSE_PROMPT } = require('./chatPrompts');

let aiClient = null;
let lastGeminiCallAt = 0;

function getClient() {
  if (!process.env.GEMINI_API_KEY) {
    const error = new Error('GEMINI_API_KEY chưa được cấu hình');
    error.status = 503;
    throw error;
  }

  if (!aiClient) {
    aiClient = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY.trim() });
  }

  return aiClient;
}

function extractJson(text) {
  const raw = String(text || '').trim();
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch (_error) {
    const fenced = raw.match(/```(?:json)?\s*([\s\S]*?)```/i);
    if (fenced) {
      return JSON.parse(fenced[1].trim());
    }

    const start = raw.indexOf('{');
    const end = raw.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return JSON.parse(raw.slice(start, end + 1));
    }
  }

  return null;
}

function isModelAccessDenied(error) {
  const status = error?.status || error?.statusCode;
  const message = String(error?.message || '').toLowerCase();
  return status === 403
    || message.includes('permission_denied')
    || message.includes('denied access');
}

function isQuotaExceeded(error) {
  const status = error?.status || error?.statusCode;
  const message = String(error?.message || '').toLowerCase();
  return status === 429
    || message.includes('quota')
    || message.includes('rate limit')
    || message.includes('too many requests')
    || message.includes('resource exhausted');
}

async function waitForGlobalCooldown() {
  const elapsed = Date.now() - lastGeminiCallAt;
  const minGap = AI_CONFIG.minRequestIntervalMs;
  if (elapsed < minGap) {
    await new Promise((resolve) => setTimeout(resolve, minGap - elapsed));
  }
}

async function generateWithModel(client, model, prompt, responseMimeType) {
  await waitForGlobalCooldown();
  const response = await client.models.generateContent({
    model,
    contents: prompt,
    config: responseMimeType ? { responseMimeType } : undefined,
  });
  lastGeminiCallAt = Date.now();
  return response.text;
}

async function callGeminiWithRetry(prompt, options = {}) {
  const client = getClient();
  const {
    responseMimeType = 'application/json',
    retries = AI_CONFIG.maxRetries,
    delay = AI_CONFIG.retryDelayMs,
  } = options;

  const models = AI_CONFIG.modelFallbacks?.length
    ? AI_CONFIG.modelFallbacks
    : [AI_CONFIG.model];

  let lastError = null;

  for (const model of models) {
    for (let attempt = 0; attempt < retries; attempt += 1) {
      try {
        return await generateWithModel(client, model, prompt, responseMimeType);
      } catch (error) {
        lastError = error;

        if (isModelAccessDenied(error)) {
          console.warn(`[Gemini] Model ${model} bị từ chối, thử model khác...`);
          break;
        }

        if (isQuotaExceeded(error) && attempt < retries - 1) {
          const waitMs = AI_CONFIG.quotaRetryDelayMs * (attempt + 1);
          console.warn(`[Gemini] 429 quota – đợi ${waitMs}ms rồi thử lại...`);
          await new Promise((resolve) => setTimeout(resolve, waitMs));
          continue;
        }

        const isRetryable = error.status === 503;
        if (isRetryable && attempt < retries - 1) {
          await new Promise((resolve) => setTimeout(resolve, delay * (attempt + 1)));
          continue;
        }

        throw mapGeminiError(error);
      }
    }
  }

  throw mapGeminiError(lastError || new Error('Không thể gọi Gemini API'));
}

function mapGeminiError(error) {
  const status = error?.status || error?.statusCode || 500;
  const wrapped = new Error('Không thể kết nối dịch vụ AI. Vui lòng thử lại sau.');
  wrapped.status = status;

  if (isQuotaExceeded(error)) {
    wrapped.message = 'Đã vượt quota Gemini free tier. Hệ thống sẽ dùng câu trả lời mẫu. Vui lòng đợi 1–2 phút rồi thử lại.';
    wrapped.isQuotaExceeded = true;
    wrapped.status = 503;
    return wrapped;
  }

  if (isModelAccessDenied(error)) {
    wrapped.message = 'Model Gemini không khả dụng. Đặt GEMINI_MODEL=gemini-2.0-flash-lite trong .env';
    wrapped.status = 503;
    return wrapped;
  }

  if (status === 429) {
    wrapped.message = 'Quá nhiều yêu cầu AI. Vui lòng đợi vài giây.';
    wrapped.isQuotaExceeded = true;
    wrapped.status = 503;
    return wrapped;
  }

  return error;
}

function buildHistoryText(messages = []) {
  return messages
    .slice(-AI_CONFIG.maxHistoryMessages)
    .map((message) => `${message.role}: ${message.content}`)
    .join('\n');
}

function compactContextForPrompt(contextData = {}) {
  return {
    intent: contextData.intent,
    products: (contextData.products || []).slice(0, AI_CONFIG.maxContextProducts).map((p) => ({
      name: p.name,
      price: p.price,
      unit: p.unit,
      sku: p.sku,
    })),
    recipes: (contextData.recipes || []).slice(0, AI_CONFIG.maxContextRecipes).map((r) => ({
      title: r.title,
      cookingTime: r.cookingTime,
    })),
    nutrition: contextData.nutrition?.totals || null,
    healthTips: contextData.healthAdvice?.tips || null,
  };
}

async function analyzeIntent(userMessage, conversationHistory = []) {
  const historyText = buildHistoryText(conversationHistory);
  const prompt = `${INTENT_ANALYSIS_PROMPT}

Lịch sử:
${historyText || '(trống)'}

Câu hỏi: ${userMessage}`;

  const text = await callGeminiWithRetry(prompt, { responseMimeType: 'application/json' });
  const parsed = extractJson(text);

  if (!parsed || !parsed.intent) {
    return {
      intent: INTENTS.GENERAL_CHAT,
      parameters: {},
      confidence: 0,
      source: 'gemini',
    };
  }

  return {
    intent: Object.values(INTENTS).includes(parsed.intent)
      ? parsed.intent
      : INTENTS.GENERAL_CHAT,
    parameters: parsed.parameters || {},
    confidence: Number(parsed.confidence) || 0,
    source: 'gemini',
  };
}

async function generateResponse({
  userMessage,
  intent,
  contextData,
  conversationHistory = [],
}) {
  const historyText = buildHistoryText(conversationHistory);
  const compactContext = compactContextForPrompt(contextData);
  const prompt = `${RESPONSE_PROMPT}

Intent: ${intent}
Câu hỏi: ${userMessage}
Lịch sử: ${historyText || '(trống)'}
Context: ${JSON.stringify(compactContext)}

Trả lời ngắn gọn:`;

  const text = await callGeminiWithRetry(prompt, { responseMimeType: null });
  return String(text || '').trim()
    || 'Xin lỗi, mình chưa thể trả lời câu hỏi này. Bạn thử hỏi lại nhé!';
}

module.exports = {
  analyzeIntent,
  generateResponse,
  extractJson,
  isQuotaExceeded,
  mapGeminiError,
};
