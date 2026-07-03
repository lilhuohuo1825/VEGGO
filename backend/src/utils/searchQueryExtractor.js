const { normalizeText } = require('./recipeKeywords');

const CHAT_STOP_WORDS = new Set([
  'gợi', 'ý', 'goi', 'y', 'san', 'sản', 'phẩm', 'pham', 'tìm', 'tim', 'mua',
  'cho', 'mình', 'minh', 'tôi', 'toi', 'bạn', 'ban', 'về', 've', 'loai', 'loại',
  'nào', 'nao', 'gì', 'gi', 'có', 'co', 'không', 'khong', 'giúp', 'giup',
  'tư', 'vấn', 'tu', 'van', 'hãy', 'hay', 'muốn', 'muon', 'cần', 'can',
  'xin', 'hỏi', 'hoi', 'bán', 'ban', 'kiếm', 'kiem', 'này', 'nay', 'đó', 'do',
  'thì', 'thi', 'là', 'la', 'của', 'cua', 'một', 'mot', 'vài', 'vai', 'some',
  'any', 'the', 'a', 'an', 'please', 'help', 'recommend', 'suggestion',
]);

const PHRASE_REMOVALS = [
  /gợi ý sản phẩm/gi,
  /goi y san pham/gi,
  /gợi ý món/gi,
  /gợi ý/gi,
  /goi y/gi,
  /tìm kiếm/gi,
  /tim kiem/gi,
  /sản phẩm/gi,
  /san pham/gi,
  /cho mình/gi,
  /cho minh/gi,
  /cho tôi/gi,
  /cho toi/gi,
  /tư vấn/gi,
  /tu van/gi,
  /có bán/gi,
  /co ban/gi,
  /bạn có/gi,
  /ban co/gi,
  /muốn mua/gi,
  /muon mua/gi,
];

function normalizeQuery(text) {
  return normalizeText(String(text || '').trim());
}

/**
 * Tách từ khóa sản phẩm thực sự khỏi câu chat.
 * VD: "gợi ý sản phẩm cà phê" → "cà phê"
 */
function extractProductSearchTerms(rawQuery) {
  let text = String(rawQuery || '').trim();
  if (!text) {
    return '';
  }

  for (const pattern of PHRASE_REMOVALS) {
    text = text.replace(pattern, ' ');
  }

  const tokens = text
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 2);

  const meaningful = tokens.filter((token) => {
    const normalized = normalizeQuery(token);
    return normalized.length >= 2 && !CHAT_STOP_WORDS.has(normalized);
  });

  if (meaningful.length) {
    return meaningful.join(' ').trim();
  }

  const normalizedFull = normalizeQuery(text);
  return normalizedFull.length >= 2 ? text.trim() : '';
}

function hasProductSearchIntent(text) {
  const normalized = normalizeQuery(text);
  if (!normalized) {
    return false;
  }

  const productSignals = [
    /gợi ý/, /goi y/, /sản phẩm/, /san pham/, /\btìm\b/, /\btim\b/, /\bmua\b/,
    /có bán/, /co ban/, /bảo quản/, /bao quan/, /\bgiá\b/, /\bgia\b/,
    /loại nào/, /loai nao/, /nào ngon/, /nao ngon/,
  ];

  if (productSignals.some((pattern) => pattern.test(normalized))) {
    return true;
  }

  const extracted = extractProductSearchTerms(text);
  return extracted.length >= 2 && extracted.length <= 40;
}

module.exports = {
  extractProductSearchTerms,
  hasProductSearchIntent,
  CHAT_STOP_WORDS,
};
