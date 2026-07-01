const Product = require('../models/Product');
const Cart = require('../models/Cart');
const { extractKeywords } = require('../utils/recipeKeywords');
const { getHealthGoalKeywords, getHealthGoalKeywordGroups, resolveHealthGoal, HEALTH_GOALS } = require('../config/healthGoals');
const {
  DEFAULT_LIMIT,
  ACTIVE_PRODUCT_FILTER,
  PRODUCT_LIST_PROJECTION,
  buildIdLookupFilter,
  buildExcludeProductFilter,
  buildCategoryFilter,
  buildTokenAndFilter,
  buildKeywordsOrFilter,
  rankProductsByKeywordGroups,
  pickProductsAcrossKeywordGroups,
  buildTextSearchFilter,
  buildFieldRegexFilter,
  buildSubcategoryFilter,
  buildCategoryOnlyFilter,
  parseSearchTokens,
  extractProductSearchTerms,
  formatProductSummary,
  formatProductDetail,
  dedupeBySku,
  mergeCartItem,
  normalizeQueryOptions,
} = require('../utils/productHelpers');

async function executeProductQuery(filter, options = {}) {
  const { limit, sort, projection } = normalizeQueryOptions(options);

  return Product.find(filter)
    .select(projection)
    .sort(sort)
    .limit(limit)
    .lean();
}

async function executeTextProductQuery(keyword, options = {}) {
  const { limit, projection } = normalizeQueryOptions(options);
  const textFilter = buildTextSearchFilter(keyword);
  if (!textFilter) {
    return [];
  }

  try {
    return await Product.find({
      $and: [ACTIVE_PRODUCT_FILTER, textFilter],
    })
      .select({ ...projection, score: { $meta: 'textScore' } })
      .sort({ score: { $meta: 'textScore' }, purchase_count: -1, rating: -1 })
      .limit(limit)
      .lean();
  } catch (_error) {
    return [];
  }
}

async function findRawProductById(id) {
  const idFilter = buildIdLookupFilter(id);
  if (!idFilter) {
    return null;
  }

  return Product.findOne({
    $and: [ACTIVE_PRODUCT_FILTER, idFilter],
  }).lean();
}

async function searchProducts(keyword, options = {}) {
  const cleanedKeyword = extractProductSearchTerms(keyword) || String(keyword || '').trim();
  const tokens = parseSearchTokens(cleanedKeyword);
  if (!tokens.length) {
    return [];
  }

  let products = await executeTextProductQuery(cleanedKeyword, options);

  if (!products.length) {
    products = await executeProductQuery(
      {
        $and: [ACTIVE_PRODUCT_FILTER, ...buildTokenAndFilter(tokens)],
      },
      options
    );
  }

  if (!products.length && tokens.length > 1) {
    const orFilter = buildKeywordsOrFilter(tokens);
    if (orFilter) {
      products = await executeProductQuery(
        {
          $and: [ACTIVE_PRODUCT_FILTER, orFilter],
        },
        options
      );
    }
  }

  if (!products.length && cleanedKeyword.length >= 2) {
    products = await executeProductQuery(
      {
        $and: [ACTIVE_PRODUCT_FILTER, buildFieldRegexFilter(cleanedKeyword)],
      },
      options
    );
  }

  return dedupeBySku(products.map(formatProductSummary).filter(Boolean));
}

async function getProductById(id) {
  const idFilter = buildIdLookupFilter(id);
  if (!idFilter) {
    return null;
  }

  const product = await Product.findOne(idFilter).lean();
  return formatProductDetail(product);
}

async function getProductsByCategory(category, options = {}) {
  const categoryFilter = buildCategoryFilter(category);
  if (!categoryFilter) {
    return [];
  }

  const products = await executeProductQuery(
    {
      $and: [ACTIVE_PRODUCT_FILTER, categoryFilter],
    },
    options
  );

  return dedupeBySku(products.map(formatProductSummary).filter(Boolean));
}

async function getProductsByHealthGoal(goal, options = {}) {
  return searchByKeywordGroups(getHealthGoalKeywordGroups(goal), options);
}

async function getProductsByRecipeKeyword(keyword, options = {}) {
  const recipeKeywords = extractKeywords(keyword);
  const keywords = recipeKeywords.length
    ? recipeKeywords
    : parseSearchTokens(keyword);

  const keywordFilter = buildKeywordsOrFilter(keywords);
  if (!keywordFilter) {
    return [];
  }

  const products = await executeProductQuery(
    {
      $and: [ACTIVE_PRODUCT_FILTER, keywordFilter],
    },
    options
  );

  return dedupeBySku(products.map(formatProductSummary).filter(Boolean));
}

async function getRelatedProducts(productId, options = {}) {
  const { limit } = normalizeQueryOptions(options);
  const sourceProduct = await findRawProductById(productId);

  if (!sourceProduct) {
    return [];
  }

  const excludeFilter = buildExcludeProductFilter(sourceProduct);
  const subcategoryId = sourceProduct.SubcategoryID || sourceProduct.subcategoryId;
  const categoryId = sourceProduct.CategoryID || sourceProduct.categoryId;
  const related = [];

  if (subcategoryId) {
    const subcategoryFilter = buildSubcategoryFilter(subcategoryId);
    const subcategoryProducts = await executeProductQuery(
      {
        $and: [ACTIVE_PRODUCT_FILTER, excludeFilter, subcategoryFilter],
      },
      { ...options, limit }
    );
    related.push(...subcategoryProducts);
  }

  if (related.length < limit && categoryId) {
    const existingSkus = new Set(related.map((product) => product.sku));
    const categoryFilter = buildCategoryOnlyFilter(categoryId);
    const categoryProducts = await executeProductQuery(
      {
        $and: [ACTIVE_PRODUCT_FILTER, excludeFilter, categoryFilter],
      },
      { ...options, limit: limit * 2 }
    );

    for (const product of categoryProducts) {
      if (related.length >= limit) {
        break;
      }
      if (!existingSkus.has(product.sku)) {
        existingSkus.add(product.sku);
        related.push(product);
      }
    }
  }

  return dedupeBySku(related.map(formatProductSummary).filter(Boolean)).slice(0, limit);
}

async function getProductsFromCart(customerId, options = {}) {
  const cleanCustomerId = String(customerId || '').trim();
  if (!cleanCustomerId) {
    return [];
  }

  const cart = await Cart.findOne({ customerId: cleanCustomerId })
    .select({ customerId: 1, items: 1 })
    .lean();

  if (!cart?.items?.length) {
    return [];
  }

  const skus = [...new Set(cart.items.map((item) => String(item.sku || '').trim()).filter(Boolean))];
  if (!skus.length) {
    return [];
  }

  const products = await Product.find({
    $and: [ACTIVE_PRODUCT_FILTER, { sku: { $in: skus } }],
  })
    .select(PRODUCT_LIST_PROJECTION)
    .lean();

  const productBySku = new Map(
    products.map((product) => [String(product.sku), formatProductSummary(product)])
  );

  return cart.items
    .map((item) => mergeCartItem(productBySku.get(String(item.sku)), item))
    .filter(Boolean);
}

async function getProductsBySkus(skus = [], options = {}) {
  const cleanSkus = [...new Set(skus.map((sku) => String(sku || '').trim()).filter(Boolean))];
  if (!cleanSkus.length) {
    return [];
  }

  const products = await executeProductQuery(
    { sku: { $in: cleanSkus } },
    options
  );

  return dedupeBySku(products.map(formatProductSummary).filter(Boolean));
}

async function getProductsByNames(names = [], options = {}) {
  const cleanNames = [...new Set(names.map((name) => String(name || '').trim()).filter(Boolean))];
  if (!cleanNames.length) {
    return [];
  }

  const keywordFilter = buildKeywordsOrFilter(cleanNames);
  if (!keywordFilter) {
    return [];
  }

  const products = await executeProductQuery(
    {
      $and: [ACTIVE_PRODUCT_FILTER, keywordFilter],
    },
    options
  );

  return dedupeBySku(products.map(formatProductSummary).filter(Boolean));
}

async function searchByKeywordGroups(keywordGroups = [], options = {}) {
  const { limit } = normalizeQueryOptions(options);
  const groups = (keywordGroups || []).filter((group) => Array.isArray(group) && group.length);

  if (!groups.length) {
    return [];
  }

  const perGroupLimit = Math.min(Math.max(limit, 4), 20);
  const groupedProducts = await Promise.all(groups.map(async (group) => {
    const keywordFilter = buildKeywordsOrFilter(group);
    if (!keywordFilter) {
      return [];
    }

    const products = await executeProductQuery(
      {
        $and: [ACTIVE_PRODUCT_FILTER, keywordFilter],
      },
      { ...options, limit: perGroupLimit }
    );

    return rankProductsByKeywordGroups(products, [group]);
  }));

  const selected = pickProductsAcrossKeywordGroups(groupedProducts, limit);
  return dedupeBySku(selected.map(formatProductSummary).filter(Boolean));
}

async function searchByKeywords(keywords = [], options = {}) {
  const cleanKeywords = [...new Set(
    (keywords || [])
      .map((keyword) => String(keyword || '').trim())
      .filter((keyword) => keyword.length >= 2)
  )];

  if (!cleanKeywords.length) {
    return [];
  }

  return searchByKeywordGroups([cleanKeywords], options);
}

module.exports = {
  searchProducts,
  getProductById,
  getProductsByCategory,
  getProductsByHealthGoal,
  getProductsByRecipeKeyword,
  getRelatedProducts,
  getProductsFromCart,
  getProductsBySkus,
  getProductsByNames,
  searchByKeywords,
  searchByKeywordGroups,
  getCartProducts: getProductsFromCart,
  formatProduct: formatProductSummary,
  resolveHealthGoal,
  HEALTH_GOALS,
};
