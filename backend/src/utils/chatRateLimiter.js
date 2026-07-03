const { AI_CONFIG } = require('../config/aiConfig');

const userState = new Map();

function cleanupExpired(now) {
  const windowMs = AI_CONFIG.rateLimitWindowMs;
  for (const [key, state] of userState.entries()) {
    if (now - state.windowStart > windowMs * 2) {
      userState.delete(key);
    }
  }
}

function enforceRateLimit(customerId) {
  const key = String(customerId || 'anonymous');
  const now = Date.now();
  cleanupExpired(now);

  const state = userState.get(key) || {
    windowStart: now,
    requestCount: 0,
    lastRequestAt: 0,
  };

  if (now - state.windowStart > AI_CONFIG.rateLimitWindowMs) {
    state.windowStart = now;
    state.requestCount = 0;
  }

  if (state.lastRequestAt
      && now - state.lastRequestAt < AI_CONFIG.minRequestIntervalMs) {
    const waitSec = Math.ceil(
      (AI_CONFIG.minRequestIntervalMs - (now - state.lastRequestAt)) / 1000
    );
    const error = new Error(`Vui lòng đợi ${waitSec} giây trước khi gửi tin nhắn tiếp theo.`);
    error.status = 429;
    throw error;
  }

  if (state.requestCount >= AI_CONFIG.maxRequestsPerWindow) {
    const error = new Error(
      `Bạn đã gửi quá nhiều tin nhắn (${AI_CONFIG.maxRequestsPerWindow}/${AI_CONFIG.rateLimitWindowMs / 60000} phút). `
      + 'Vui lòng đợi một lát để tiết kiệm quota AI miễn phí.'
    );
    error.status = 429;
    throw error;
  }

  state.requestCount += 1;
  state.lastRequestAt = now;
  userState.set(key, state);
}

module.exports = {
  enforceRateLimit,
};
