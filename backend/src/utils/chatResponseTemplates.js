const { INTENTS } = require('../config/aiConfig');

function formatVnd(price) {
  return `${Number(price || 0).toLocaleString('vi-VN')}đ`;
}

function formatProductLine(product) {
  const unit = product.unit ? `/${product.unit}` : '';
  return `• ${product.name} – ${formatVnd(product.price)}${unit}`;
}

function buildGreetingResponse() {
  return 'Xin chào! 👋 Mình là Trợ lý AI của VEGGO.\n\n'
    + 'Bạn có thể hỏi mình:\n'
    + '• Gợi ý rau củ theo mục tiêu sức khỏe\n'
    + '• Công thức nấu từ giỏ hàng\n'
    + '• Calories và dinh dưỡng bữa ăn\n'
    + '• Tìm kiếm sản phẩm';
}

function buildThanksResponse() {
  return 'Không có gì! Nếu cần tư vấn thêm về rau củ hoặc món ăn, cứ hỏi mình nhé. 😊';
}

function buildHealthResponse(context) {
  const advice = context.healthAdvice;
  const products = context.products || advice?.products || [];

  let reply = `🥗 **${advice?.goalLabel || 'Gợi ý sức khỏe'}**\n`;
  if (advice?.tips) {
    reply += `${advice.tips}\n\n`;
  }

  if (!products.length) {
    reply += 'Hiện chưa tìm thấy sản phẩm phù hợp trong cửa hàng. Bạn thử hỏi cụ thể hơn nhé.';
    return reply;
  }

  reply += '**Sản phẩm gợi ý:**\n';
  reply += products.slice(0, 5).map(formatProductLine).join('\n');
  return reply;
}

function buildProductSearchResponse(context) {
  const products = context.products || [];
  if (!products.length) {
    return 'Mình chưa tìm thấy sản phẩm phù hợp. Bạn thử đổi từ khóa hoặc gõ tên rau củ cụ thể nhé.';
  }

  let reply = `🔍 Tìm thấy ${products.length} sản phẩm:\n`;
  reply += products.slice(0, 5).map(formatProductLine).join('\n');
  return reply;
}

function buildRecipeResponse(context) {
  const recipes = context.recipes || [];
  const ingredient = context.recipeIngredient || '';

  if (!recipes.length) {
    if (ingredient) {
      return `Chưa tìm thấy công thức phù hợp với **${ingredient}**. Bạn thử hỏi tên nguyên liệu khác hoặc thêm sản phẩm vào giỏ hàng nhé.`;
    }
    return 'Chưa có công thức phù hợp với nguyên liệu hiện tại. Bạn thử thêm sản phẩm vào giỏ hàng hoặc nêu tên nguyên liệu cụ thể nhé.';
  }

  let reply = ingredient
    ? `🍳 **Món có thể nấu với ${ingredient}:**\n`
    : '🍳 **Gợi ý món ăn:**\n';
  for (const recipe of recipes.slice(0, 3)) {
    const time = recipe.cookingTime ? ` (${recipe.cookingTime})` : '';
    const match = recipe.matchedIngredientCount
      ? ` – khớp ${recipe.matchedIngredientCount} nguyên liệu`
      : '';
    reply += `• ${recipe.title}${time}${match}\n`;
  }
  return reply.trim();
}

function buildNutritionResponse(context) {
  const totals = context.nutrition?.totals;
  if (!totals) {
    return 'Chưa đủ dữ liệu dinh dưỡng cho các sản phẩm này. Bạn thử nêu tên món hoặc thêm vào giỏ hàng nhé.';
  }

  return '📊 **Tổng dinh dưỡng ước tính:**\n'
    + `• Calories: ${totals.calories || 0} kcal\n`
    + `• Protein: ${totals.protein || 0}g\n`
    + `• Carbohydrate: ${totals.carbohydrate || 0}g\n`
    + `• Fat: ${totals.fat || 0}g\n`
    + `• Fiber: ${totals.fiber || 0}g`;
}

function buildGeneralFallback() {
  return 'Mình đã nhận câu hỏi của bạn. Bạn có thể hỏi về sản phẩm, dinh dưỡng, công thức nấu hoặc mục tiêu sức khỏe nhé.';
}

function canBuildTemplateResponse(intent, context, message) {
  if (intent === INTENTS.GENERAL_CHAT) {
    return true;
  }
  if (intent === INTENTS.HEALTH_ADVICE) {
    return Boolean(context.healthAdvice || context.products?.length);
  }
  if (intent === INTENTS.PRODUCT_SEARCH) {
    return true;
  }
  if (intent === INTENTS.RECIPE_SUGGESTION) {
    return true;
  }
  if (intent === INTENTS.NUTRITION_CALCULATION) {
    return Boolean(context.nutrition);
  }
  return false;
}

function buildTemplateResponse(intent, context, message) {
  const text = String(message || '').toLowerCase();

  if (intent === INTENTS.GENERAL_CHAT) {
    if (/cảm ơn|cam on|thanks/.test(text)) {
      return buildThanksResponse();
    }
    if (/tạm biệt|tam biet|bye/.test(text)) {
      return 'Tạm biệt! Hẹn gặp lại bạn tại VEGGO. 👋';
    }
    return buildGreetingResponse();
  }

  switch (intent) {
    case INTENTS.HEALTH_ADVICE:
      return buildHealthResponse(context);
    case INTENTS.PRODUCT_SEARCH:
      return buildProductSearchResponse(context);
    case INTENTS.RECIPE_SUGGESTION:
      return buildRecipeResponse(context);
    case INTENTS.NUTRITION_CALCULATION:
      return buildNutritionResponse(context);
    default:
      return buildGeneralFallback();
  }
}

module.exports = {
  buildTemplateResponse,
  canBuildTemplateResponse,
  buildGreetingResponse,
};
