const NutritionProfile = require('../models/NutritionProfile');
const productService = require('./productService');
const { normalizeText } = require('../utils/recipeKeywords');
const {
  EMPTY_TOTALS,
  mergeTotals,
  parseQuantityToGrams,
  normalizePer100g,
  scaleNutrition,
  lookupFallbackNutrition,
  normalizeProductInput,
  roundNutritionValues,
} = require('../utils/nutritionHelpers');

function escapeRegex(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function loadNutritionProfile({ sku, name }) {
  if (sku) {
    const bySku = await NutritionProfile.findOne({ sku, isActive: true }).lean();
    if (bySku) {
      return { profile: bySku, source: 'database' };
    }
  }

  if (name) {
    const normalized = normalizeText(name);
    const byName = await NutritionProfile.findOne({
      isActive: true,
      $or: [
        { foodNameNormalized: normalized },
        { foodName: new RegExp(escapeRegex(normalized), 'i') },
      ],
    }).lean();

    if (byName) {
      return { profile: byName, source: 'database' };
    }

    const fallback = lookupFallbackNutrition(name);
    if (fallback) {
      return {
        profile: {
          foodName: name,
          per100g: fallback,
          vitamins: fallback.vitamins || {},
        },
        source: 'reference',
      };
    }
  }

  return { profile: null, source: 'unknown' };
}

async function buildItemNutrition(productInput = {}) {
  const product = normalizeProductInput(productInput);
  const grams = parseQuantityToGrams(product.quantity, product.unit, productInput);
  const { profile, source } = await loadNutritionProfile({
    sku: product.sku,
    name: product.name,
  });

  if (!profile) {
    return {
      sku: product.sku,
      name: product.name,
      quantity: product.quantity,
      unit: product.unit,
      grams,
      source,
      hasData: false,
      nutrition: null,
    };
  }

  const per100g = normalizePer100g({
    ...profile.per100g,
    vitamins: profile.vitamins || profile.per100g?.vitamins || {},
  });

  return {
    sku: profile.sku || product.sku,
    name: profile.foodName || product.name,
    quantity: product.quantity,
    unit: product.unit,
    grams,
    source,
    hasData: true,
    per100g: roundNutritionValues(per100g),
    nutrition: scaleNutrition(per100g, grams),
  };
}

/**
 * NutritionService – tính dinh dưỡng từ danh sách sản phẩm.
 * Chỉ trả JSON object, không sinh câu trả lời tự nhiên.
 */
async function calculateFromProducts(products = []) {
  const normalizedProducts = (products || []).filter(
    (product) => product && (product.name || product.product_name || product.sku)
  );

  const items = await Promise.all(normalizedProducts.map((product) => buildItemNutrition(product)));
  const totals = mergeTotals(items);

  return {
    items,
    totals,
    meta: {
      itemCount: items.length,
      itemsWithData: items.filter((item) => item.hasData).length,
      itemsMissingData: items.filter((item) => !item.hasData).length,
    },
  };
}

async function calculateFromItems(items = []) {
  return calculateFromProducts(items);
}

async function calculateFromCart(customerId) {
  const products = await productService.getProductsFromCart(customerId);
  return calculateFromProducts(products);
}

async function calculateFromProductNames(names = []) {
  const products = await productService.getProductsByNames(names);
  return calculateFromProducts(products.map((product) => ({
    ...product,
    quantity: 1,
    unit: 'portion',
  })));
}

async function calculateFromSkus(skus = []) {
  const products = await productService.getProductsBySkus(skus);
  return calculateFromProducts(products.map((product) => ({
    ...product,
    quantity: 1,
    unit: 'portion',
  })));
}

module.exports = {
  calculateFromProducts,
  calculateFromItems,
  calculateFromCart,
  calculateFromProductNames,
  calculateFromSkus,
  buildItemNutrition,
  loadNutritionProfile,
  calculateMealNutrition: calculateFromItems,
  calculateNutritionFromCart: calculateFromCart,
  calculateNutritionFromProductNames: calculateFromProductNames,
  getNutritionForItem: buildItemNutrition,
  resolveNutritionProfile: loadNutritionProfile,
  EMPTY_TOTALS,
};
