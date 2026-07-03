const { normalizeText } = require('./recipeKeywords');

const EMPTY_TOTALS = () => ({
  calories: 0,
  protein: 0,
  carbohydrate: 0,
  fat: 0,
  fiber: 0,
  vitamins: {},
});

const COMMON_FOOD_NUTRITION = {
  'cai thao': {
    calories: 16, protein: 1.5, carbohydrate: 3, fat: 0.2, fiber: 2.1,
    vitamins: { vitaminA: 98, vitaminC: 36 },
  },
  'ca chua': {
    calories: 18, protein: 0.9, carbohydrate: 3.9, fat: 0.2, fiber: 1.2,
    vitamins: { vitaminC: 14, vitaminA: 42 },
  },
  'ca rot': {
    calories: 41, protein: 0.9, carbohydrate: 9.6, fat: 0.2, fiber: 2.8,
    vitamins: { vitaminA: 835, vitaminC: 6 },
  },
  'bi do': {
    calories: 26, protein: 1, carbohydrate: 6.5, fat: 0.1, fiber: 0.5,
    vitamins: { vitaminA: 426, vitaminC: 9 },
  },
  'dua leo': {
    calories: 15, protein: 0.7, carbohydrate: 3.6, fat: 0.1, fiber: 0.5,
    vitamins: { vitaminC: 3, vitaminK: 16 },
  },
  'rau muong': {
    calories: 19, protein: 2.6, carbohydrate: 3.1, fat: 0.2, fiber: 2.1,
    vitamins: { vitaminA: 315, vitaminC: 55 },
  },
  'uc ga': {
    calories: 165, protein: 31, carbohydrate: 0, fat: 3.6, fiber: 0,
    vitamins: { vitaminB6: 0.6, vitaminB12: 0.3 },
  },
  'ca hoi': {
    calories: 208, protein: 20, carbohydrate: 0, fat: 13, fiber: 0,
    vitamins: { vitaminD: 11, vitaminB12: 3.2 },
  },
  'chuoi': {
    calories: 89, protein: 1.1, carbohydrate: 22.8, fat: 0.3, fiber: 2.6,
    vitamins: { vitaminC: 9, vitaminB6: 0.4 },
  },
  'cam': {
    calories: 47, protein: 0.9, carbohydrate: 11.8, fat: 0.1, fiber: 2.4,
    vitamins: { vitaminC: 53, vitaminA: 11 },
  },
};

function roundNumber(value, digits = 1) {
  const factor = 10 ** digits;
  return Math.round(Number(value || 0) * factor) / factor;
}

function roundNutritionValues(values = {}) {
  return {
    calories: Math.round(Number(values.calories || 0)),
    protein: roundNumber(values.protein),
    carbohydrate: roundNumber(values.carbohydrate ?? values.carbs),
    fat: roundNumber(values.fat),
    fiber: roundNumber(values.fiber),
    vitamins: roundVitaminValues(values.vitamins || {}),
  };
}

function roundVitaminValues(vitamins = {}) {
  return Object.entries(vitamins).reduce((result, [key, value]) => {
    result[key] = roundNumber(value);
    return result;
  }, {});
}

function addNutritionValues(target, source, multiplier = 1) {
  target.calories += Number(source.calories || 0) * multiplier;
  target.protein += Number(source.protein || 0) * multiplier;
  target.carbohydrate += Number(source.carbohydrate ?? source.carbs ?? 0) * multiplier;
  target.fat += Number(source.fat || 0) * multiplier;
  target.fiber += Number(source.fiber || 0) * multiplier;

  const vitamins = source.vitamins || {};
  for (const [key, value] of Object.entries(vitamins)) {
    target.vitamins[key] = (target.vitamins[key] || 0) + Number(value || 0) * multiplier;
  }
}

function mergeTotals(items = []) {
  const totals = EMPTY_TOTALS();

  for (const item of items) {
    if (!item?.nutrition) continue;
    addNutritionValues(totals, item.nutrition, 1);
  }

  return roundNutritionValues(totals);
}

function parseQuantityToGrams(quantity, unit, product = {}) {
  const value = Number(quantity);
  const normalizedUnit = String(unit || product.unit || 'g').trim().toLowerCase();

  if (Number.isFinite(value) && value > 0) {
    if (normalizedUnit === 'kg') return value * 1000;
    if (normalizedUnit === 'g' || normalizedUnit === 'gr' || normalizedUnit === 'gram') return value;
    if (normalizedUnit === 'portion' || normalizedUnit === 'phan') return value * 150;
    if (normalizedUnit.includes('kg')) return value * 1000;
  }

  const selectedWeight = Number(product.selectedWeight || product.cartQuantity);
  if (Number.isFinite(selectedWeight) && selectedWeight > 0) {
    return selectedWeight * 1000;
  }

  return 100;
}

function normalizePer100g(profile = {}) {
  const per100g = profile.per100g || profile;
  return {
    calories: Number(per100g.calories || 0),
    protein: Number(per100g.protein || 0),
    carbohydrate: Number(per100g.carbohydrate ?? per100g.carbs ?? 0),
    fat: Number(per100g.fat || 0),
    fiber: Number(per100g.fiber || 0),
    vitamins: profile.vitamins || {},
  };
}

function scaleNutrition(per100g, grams) {
  const multiplier = grams / 100;
  const scaled = {
    calories: per100g.calories * multiplier,
    protein: per100g.protein * multiplier,
    carbohydrate: per100g.carbohydrate * multiplier,
    fat: per100g.fat * multiplier,
    fiber: per100g.fiber * multiplier,
    vitamins: {},
  };

  for (const [key, value] of Object.entries(per100g.vitamins || {})) {
    scaled.vitamins[key] = Number(value || 0) * multiplier;
  }

  return roundNutritionValues(scaled);
}

function lookupFallbackNutrition(name) {
  const normalized = normalizeText(name);
  if (COMMON_FOOD_NUTRITION[normalized]) {
    return normalizePer100g(COMMON_FOOD_NUTRITION[normalized]);
  }

  for (const [key, value] of Object.entries(COMMON_FOOD_NUTRITION)) {
    if (normalized.includes(key) || key.includes(normalized)) {
      return normalizePer100g(value);
    }
  }

  return null;
}

function normalizeProductInput(product = {}) {
  return {
    sku: product.sku || product.productSku || '',
    name: product.name || product.product_name || product.productName || '',
    quantity: product.quantity ?? product.cartQuantity ?? product.selectedWeight ?? 1,
    unit: product.unit || 'portion',
    selectedWeight: product.selectedWeight,
    cartQuantity: product.cartQuantity,
  };
}

module.exports = {
  EMPTY_TOTALS,
  COMMON_FOOD_NUTRITION,
  roundNumber,
  roundNutritionValues,
  roundVitaminValues,
  addNutritionValues,
  mergeTotals,
  parseQuantityToGrams,
  normalizePer100g,
  scaleNutrition,
  lookupFallbackNutrition,
  normalizeProductInput,
};
