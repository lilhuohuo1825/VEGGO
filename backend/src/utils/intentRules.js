const { INTENTS } = require('../config/aiConfig');
const {
  extractProductSearchTerms,
  hasProductSearchIntent,
} = require('./searchQueryExtractor');
const {
  extractRecipeIngredientTerms,
  hasRecipeSuggestionIntent,
} = require('./recipeQueryExtractor');

function normalize(text) {
  return String(text || '')
    .toLowerCase()
    .normalize('NFC')
    .trim();
}

const GREETING_PATTERNS = [
  /^(xin chào|chào bạn|chào|hello|hi|hey)\b/,
  /^(cảm ơn|cam on|thanks|thank you)\b/,
  /^(tạm biệt|tam biet|bye)\b/,
];

const HEALTH_GOAL_RULES = [
  { goal: 'weight_loss', patterns: [/giảm cân/, /giam can/, /lose weight/, /ít calo/] },
  { goal: 'muscle_gain', patterns: [/tăng cơ/, /tang co/, /protein/, /gym/] },
  { goal: 'vitamin_c', patterns: [/vitamin c/, /vit c/, /bổ sung vitamin c/] },
  { goal: 'vitamin_boost', patterns: [/vitamin/, /bổ sung vitamin/, /bo sung vitamin/] },
  { goal: 'digestion', patterns: [/tiêu hóa/, /tieu hoa/, /đầy bụng/, /chất xơ/] },
  { goal: 'immunity', patterns: [/miễn dịch/, /mien dich/, /đề kháng/, /de khang/] },
  { goal: 'energy', patterns: [/năng lượng/, /nang luong/, /mệt mỏi/, /met moi/] },
];

const RECIPE_PATTERNS = [
  /nấu\s+được/, /nau\s+duoc/, /nấu\s+gì/, /nau\s+gi/,
  /món\s+gì/, /mon\s+gi/, /công\s+thức/, /cong\s+thuc/,
  /thực\s+đơn/, /thuc\s+don/, /gợi\s+ý\s+món/, /goi\s+y\s+mon/,
  /nấu\s+món/, /nau\s+mon/, /làm\s+món/, /lam\s+mon/,
  /giỏ\s+hàng/, /gio\s+hang/, /trong\s+giỏ/, /nguyên\s+liệu\s+có/,
];

const NUTRITION_PATTERNS = [
  /calo/, /calories/, /protein/, /carb/, /dinh dưỡng/, /dinh duong/,
  /bao nhiêu calo/, /giá trị dinh dưỡng/,
];

const PRODUCT_PATTERNS = [
  /\btìm\b/, /\btim\b/, /\bmua\b/, /\bgiá\b/, /\bgia\b/,
  /bảo quản/, /bao quan/, /có bán/, /co ban/, /sản phẩm/, /san pham/,
];

function matchesAny(text, patterns) {
  return patterns.some((pattern) => pattern.test(text));
}

function detectHealthGoal(text) {
  for (const rule of HEALTH_GOAL_RULES) {
    if (rule.patterns.some((pattern) => pattern.test(text))) {
      return rule.goal;
    }
  }
  return null;
}

function isSimpleGreeting(text) {
  return matchesAny(text, GREETING_PATTERNS);
}

function isThanks(text) {
  return /^(cảm ơn|cam on|thanks)/.test(text);
}

/**
 * Phân tích intent bằng rules – không tốn API Gemini.
 * Trả null nếu không chắc chắn → mới gọi Gemini.
 */
function detectIntentByRules(message) {
  const text = normalize(message);
  if (!text) {
    return null;
  }

  if (isSimpleGreeting(text) || isThanks(text)) {
    return {
      intent: INTENTS.GENERAL_CHAT,
      parameters: {},
      confidence: 0.95,
      source: 'rules',
    };
  }

  const useCart = /giỏ hàng|gio hang|trong giỏ|đã chọn|da chon/.test(text);

  if (matchesAny(text, NUTRITION_PATTERNS)) {
    return {
      intent: INTENTS.NUTRITION_CALCULATION,
      parameters: { useCart, searchQuery: text },
      confidence: 0.9,
      source: 'rules',
    };
  }

  if (matchesAny(text, RECIPE_PATTERNS) || hasRecipeSuggestionIntent(text)) {
    const ingredient = extractRecipeIngredientTerms(message);
    return {
      intent: INTENTS.RECIPE_SUGGESTION,
      parameters: {
        useCart: useCart && !ingredient,
        searchQuery: ingredient || text,
        ingredients: ingredient ? [ingredient] : [],
        productNames: ingredient ? [ingredient] : [],
      },
      confidence: 0.92,
      source: 'rules',
    };
  }

  const healthGoal = detectHealthGoal(text);
  if (healthGoal || /nên ăn gì|nen an gi|tư vấn|tu van|sức khỏe|suc khoe/.test(text)) {
    return {
      intent: INTENTS.HEALTH_ADVICE,
      parameters: {
        healthGoal: healthGoal || 'vitamin_boost',
        searchQuery: text,
      },
      confidence: healthGoal ? 0.92 : 0.75,
      source: 'rules',
    };
  }

  if (matchesAny(text, PRODUCT_PATTERNS) || hasProductSearchIntent(text)) {
    const terms = extractProductSearchTerms(message) || text;
    return {
      intent: INTENTS.PRODUCT_SEARCH,
      parameters: {
        searchQuery: terms,
        productNames: terms ? [terms] : [],
        useCart,
      },
      confidence: 0.9,
      source: 'rules',
    };
  }

  if (text.length <= 30) {
    return {
      intent: INTENTS.GENERAL_CHAT,
      parameters: {},
      confidence: 0.6,
      source: 'rules',
    };
  }

  return null;
}

module.exports = {
  detectIntentByRules,
  isSimpleGreeting,
  isThanks,
  normalize,
};
