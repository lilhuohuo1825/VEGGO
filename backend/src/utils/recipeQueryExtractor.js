const { normalizeText } = require('./recipeKeywords');

const RECIPE_INTENT_PATTERNS = [
  /nấu\s+được/, /nau\s+duoc/, /nấu\s+gì/, /nau\s+gi/,
  /món\s+gì/, /mon\s+gi/, /công\s+thức/, /cong\s+thuc/,
  /thực\s+đơn/, /thuc\s+don/, /gợi\s+ý\s+món/, /goi\s+y\s+mon/,
  /nấu\s+món/, /nau\s+mon/, /làm\s+món/, /lam\s+mon/,
  /giỏ\s+hàng/, /gio\s+hang/, /trong\s+giỏ/, /nguyên\s+liệu\s+có/,
];

const INGREDIENT_CAPTURE_PATTERNS = [
  /với\s+(.+?)\s+nấu\s+được/i,
  /voi\s+(.+?)\s+nau\s+duoc/i,
  /với\s+(.+?)\s+nấu\b/i,
  /voi\s+(.+?)\s+nau\b/i,
  /(.+?)\s+nấu\s+được\s*(?:món\s*)?gì/i,
  /(.+?)\s+nau\s+duoc\s*(?:mon\s*)?gi/i,
  /nấu\s+(?:được\s+)?(?:món\s+)?gì\s+(?:với|từ)\s+(.+)/i,
  /nau\s+(?:duoc\s+)?(?:mon\s+)?gi\s+(?:voi|tu)\s+(.+)/i,
  /món\s+gì\s+(?:làm|nấu)\s+(?:với|từ)\s+(.+)/i,
  /mon\s+gi\s+(?:lam|nau)\s+(?:voi|tu)\s+(.+)/i,
  /công\s+thức\s+(?:với|từ|cho)\s+(.+)/i,
  /cong\s+thuc\s+(?:voi|tu|cho)\s+(.+)/i,
  /thực\s+đơn\s+(?:với|từ|cho)\s+(.+)/i,
  /thuc\s+don\s+(?:voi|tu|cho)\s+(.+)/i,
  /gợi\s+ý\s+món\s+(?:với|từ|cho)\s+(.+)/i,
  /goi\s+y\s+mon\s+(?:voi|tu|cho)\s+(.+)/i,
];

const INGREDIENT_PREFIX_REMOVALS = [
  /^(với|voi|từ|tu|cho|dùng|dung|bằng|bang)\s+/i,
];

const INGREDIENT_SUFFIX_REMOVALS = [
  /\s+(thì|thi)\s+nấu\s+được.*$/i,
  /\s+(thi|thi)\s+nau\s+duoc.*$/i,
  /\s+nấu\s+được.*$/i,
  /\s+nau\s+duoc.*$/i,
  /\s+nấu\s+gì.*$/i,
  /\s+nau\s+gi.*$/i,
  /\s+(món|mon)\s+gì.*$/i,
  /\s+gì\s*$/i,
  /\s+gi\s*$/i,
];

const RECIPE_STOP_WORDS = new Set([
  'gì', 'gi', 'mon', 'món', 'nấu', 'nau', 'được', 'duoc', 'với', 'voi',
  'từ', 'tu', 'cho', 'làm', 'lam', 'công', 'cong', 'thức', 'thuc',
  'gợi', 'ý', 'goi', 'y', 'thực', 'thuc', 'đơn', 'don', 'ăn', 'an',
  'nào', 'nao', 'mình', 'minh', 'tôi', 'toi', 'bạn', 'ban', 'có', 'co',
  'không', 'khong', 'đó', 'do', 'này', 'nay', 'thì', 'thi', 'là', 'la',
  'một', 'mot', 'vài', 'vai', 'gì', 'gi', 'được', 'duoc',
]);

function cleanIngredientPhrase(phrase) {
  let text = String(phrase || '').trim();
  if (!text) {
    return '';
  }

  for (const pattern of INGREDIENT_PREFIX_REMOVALS) {
    text = text.replace(pattern, '');
  }
  for (const pattern of INGREDIENT_SUFFIX_REMOVALS) {
    text = text.replace(pattern, '');
  }

  const tokens = text
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 2);

  const meaningful = tokens.filter((token) => !RECIPE_STOP_WORDS.has(normalizeText(token)));
  if (meaningful.length) {
    return meaningful.join(' ').trim();
  }

  return text.trim();
}

/**
 * Tách nguyên liệu từ câu hỏi thực đơn.
 * VD: "Với ớt chuông nấu được món gì" → "ớt chuông"
 */
function extractRecipeIngredientTerms(message) {
  const text = String(message || '').trim();
  if (!text) {
    return '';
  }

  for (const pattern of INGREDIENT_CAPTURE_PATTERNS) {
    const match = text.match(pattern);
    if (!match?.[1]) {
      continue;
    }

    const cleaned = cleanIngredientPhrase(match[1]);
    if (cleaned.length >= 2) {
      return cleaned;
    }
  }

  return '';
}

function hasRecipeSuggestionIntent(text) {
  const normalized = normalizeText(text);
  if (!normalized) {
    return false;
  }

  return RECIPE_INTENT_PATTERNS.some((pattern) => pattern.test(normalized));
}

module.exports = {
  extractRecipeIngredientTerms,
  hasRecipeSuggestionIntent,
  cleanIngredientPhrase,
  RECIPE_INTENT_PATTERNS,
};
