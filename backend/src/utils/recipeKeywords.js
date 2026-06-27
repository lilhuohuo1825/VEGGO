const PACKAGING_PATTERN = /\d+([.,]\d+)?\s*(kg|g|gr|gram|grams|ml|l|lit|litre|lít|pack|hộp|túi|gói|chai|lon|piece|pcs|viên|vỉ|hũ|bịch|bich)\b/gi;

const STOP_WORDS = new Set([
  'co.op', 'coop', 'co', 'op', 'finest', 'sinh thái', 'sinh', 'thai', 'organic', 'hữu cơ',
  'tmcp', 'thương mại', 'premium', 'natural', 'fresh', 'vietnam', 'việt nam',
  'st25', 'st24', 'st25i', 'combo', 'set', 'bộ', 'loại', 'hàng',
  'chính hãng', 'thương hiệu', 'size', 'mini', 'maxi', 'plus', 'pro', 'new', 'hot', 'sale',
]);

const VIETNAMESE_DIACRITICS = {
  à: 'a', á: 'a', ả: 'a', ã: 'a', ạ: 'a',
  ă: 'a', ằ: 'a', ắ: 'a', ẳ: 'a', ẵ: 'a', ặ: 'a',
  â: 'a', ầ: 'a', ấ: 'a', ẩ: 'a', ẫ: 'a', ậ: 'a',
  è: 'e', é: 'e', ẻ: 'e', ẽ: 'e', ẹ: 'e',
  ê: 'e', ề: 'e', ế: 'e', ể: 'e', ễ: 'e', ệ: 'e',
  ì: 'i', í: 'i', ỉ: 'i', ĩ: 'i', ị: 'i',
  ò: 'o', ó: 'o', ỏ: 'o', õ: 'o', ọ: 'o',
  ô: 'o', ồ: 'o', ố: 'o', ổ: 'o', ỗ: 'o', ộ: 'o',
  ơ: 'o', ờ: 'o', ớ: 'o', ở: 'o', ỡ: 'o', ợ: 'o',
  ù: 'u', ú: 'u', ủ: 'u', ũ: 'u', ụ: 'u',
  ư: 'u', ừ: 'u', ứ: 'u', ử: 'u', ữ: 'u', ự: 'u',
  ỳ: 'y', ý: 'y', ỷ: 'y', ỹ: 'y', ỵ: 'y',
  đ: 'd',
};

function removeDiacritics(text) {
  return text.replace(/./g, (char) => VIETNAMESE_DIACRITICS[char] || char);
}

function normalizeText(text) {
  return removeDiacritics(String(text || '').toLowerCase().trim());
}

function extractKeywords(productName) {
  if (!productName || !productName.trim()) {
    return [];
  }

  let text = normalizeText(productName);
  text = text.replace(PACKAGING_PATTERN, ' ');
  text = text.replace(/[^\p{L}\p{N}\s-]/gu, ' ');

  const rawTokens = text.split(/\s+/).filter(Boolean);
  const keywords = [];
  const seen = new Set();

  for (let i = 0; i < rawTokens.length; i += 1) {
    const token = rawTokens[i];
    if (token.length < 2 || STOP_WORDS.has(token)) {
      continue;
    }

    // Merge common two-word food phrases (e.g. "gao lut" from product name)
    if (i + 1 < rawTokens.length) {
      const next = rawTokens[i + 1];
      const pair = `${token} ${next}`;
      if (!STOP_WORDS.has(next)
          && !STOP_WORDS.has(pair)
          && pair.length >= 4
          && !seen.has(pair)) {
        seen.add(pair);
        keywords.push(pair);
      }
    }

    if (!seen.has(token)) {
      seen.add(token);
      keywords.push(token);
    }
  }

  return keywords.sort((a, b) => b.length - a.length);
}

function ingredientsToSearchText(dish) {
  const raw = dish.ingredients ?? dish.Ingredients;
  if (Array.isArray(raw)) {
    return raw.map((item) => String(item)).join(' ');
  }
  return String(raw || '');
}

function dishNameText(dish, instruction) {
  if (dish.dishName) return dish.dishName;
  if (instruction) {
    return instruction.title || instruction.DishName || '';
  }
  return '';
}

function scoreDishAgainstKeywords(dish, keywords, instruction) {
  if (!keywords.length) return 0;

  const ingredientsText = normalizeText(ingredientsToSearchText(dish));
  if (!ingredientsText.trim()) return 0;

  const nameText = normalizeText(dishNameText(dish, instruction));
  let score = 0;

  for (const keyword of keywords) {
    const normalizedKeyword = normalizeText(keyword);
    if (!normalizedKeyword) continue;

    if (ingredientsText.includes(normalizedKeyword)) {
      score += 3;
    } else if (nameText.includes(normalizedKeyword)) {
      score += 1;
    }
  }

  return score;
}

function normalizeMatchScore(rawScore, keywordCount) {
  if (rawScore <= 0 || keywordCount <= 0) return 0;
  const maxPossible = keywordCount * 3;
  return Math.round((rawScore / maxPossible) * 100) / 100;
}

function getInstructionKey(instruction) {
  return instruction._id.toString();
}

function resolveInstructionDisplay(instruction, dishDescription) {
  const title = instruction.title || instruction.DishName || '';
  const image = instruction.image || instruction.Image || '';
  const description = dishDescription
    || instruction.description
    || instruction.Ingredient
    || '';
  return { title, image, description };
}

function buildCookingSteps(dish) {
  if (dish.cookingSteps) return dish.cookingSteps;

  const parts = [
    dish.Preparation ? `Sơ chế:\n${dish.Preparation}` : '',
    dish.Cooking ? `Nấu:\n${dish.Cooking}` : '',
  ].filter(Boolean);

  return parts.join('\n\n');
}

function buildUsage(dish) {
  const usage = dish.usage || dish.Usage || dish.Serving || '';
  return String(usage)
    .replace(/^Cách\s*Dùng:\s*/i, '')
    .trim();
}

function parseIngredientsList(dish) {
  const raw = dish.ingredients ?? dish.Ingredients;
  if (Array.isArray(raw)) {
    return raw.map((item) => String(item).trim()).filter(Boolean);
  }
  if (!raw || !String(raw).trim()) return [];
  return String(raw)
    .split(/[;\n]/)
    .map((part) => part.trim())
    .filter(Boolean);
}

module.exports = {
  extractKeywords,
  scoreDishAgainstKeywords,
  normalizeMatchScore,
  getInstructionKey,
  resolveInstructionDisplay,
  buildCookingSteps,
  buildUsage,
  parseIngredientsList,
  ingredientsToSearchText,
  dishNameText,
  normalizeText,
};
