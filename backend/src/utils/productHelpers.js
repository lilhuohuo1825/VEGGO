const { extractProductSearchTerms } = require('./searchQueryExtractor');

const DEFAULT_LIMIT = 12;
const DEFAULT_SORT = { purchase_count: -1, rating: -1, soldCount: -1 };

const ACTIVE_PRODUCT_FILTER = {
  $or: [
    { isActive: true },
    { isActive: { $exists: false }, status: { $ne: 'Inactive' } },
    { status: 'Active' },
  ],
};

const PRODUCT_LIST_PROJECTION = {
  sku: 1,
  product_name: 1,
  name: 1,
  brand: 1,
  price: 1,
  originalPrice: 1,
  base_price: 1,
  unit: 1,
  weight: 1,
  CategoryID: 1,
  categoryId: 1,
  SubcategoryID: 1,
  subcategoryId: 1,
  stock: 1,
  rating: 1,
  reviewCount: 1,
  purchase_count: 1,
  soldCount: 1,
  ingredients: 1,
  storage: 1,
  usage: 1,
  description: 1,
  image: 1,
  status: 1,
  isActive: 1,
  CarbonSavingPoint: 1,
  origin: 1,
  condition: 1,
  fatContent: 1,
};

const SEARCHABLE_FIELDS = ['product_name', 'name', 'brand', 'ingredients', 'description'];

function escapeRegex(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function parseSearchTokens(keyword) {
  const cleaned = extractProductSearchTerms(keyword) || String(keyword || '').trim();
  return cleaned
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 2);
}

function getProductName(product) {
  return product?.product_name || product?.name || product?.productName || '';
}

function firstImage(image) {
  if (Array.isArray(image)) {
    return image[0] || '';
  }
  return image || '';
}

function formatProductSummary(product) {
  if (!product) {
    return null;
  }

  return {
    id: String(product._id || ''),
    sku: product.sku || '',
    name: getProductName(product),
    brand: product.brand || '',
    price: Number(product.price || 0),
    originalPrice: Number(product.originalPrice || product.base_price || product.price || 0),
    unit: product.unit || '',
    weight: product.weight || '',
    categoryId: product.CategoryID || product.categoryId || '',
    subcategoryId: product.SubcategoryID || product.subcategoryId || '',
    stock: Number(product.stock || 0),
    rating: Number(product.rating || 0),
    reviewCount: Number(product.reviewCount || 0),
    soldCount: Number(product.soldCount || product.purchase_count || 0),
    ingredients: product.ingredients || '',
    storage: product.storage || '',
    usage: product.usage || '',
    description: product.description || '',
    image: firstImage(product.image),
    carbonSavingPoint: Number(product.CarbonSavingPoint || 0),
    origin: product.origin || '',
    isActive: product.isActive ?? product.status !== 'Inactive',
  };
}

function formatProductDetail(product) {
  const summary = formatProductSummary(product);
  if (!summary) {
    return null;
  }

  return {
    ...summary,
    images: Array.isArray(product.image) ? product.image.filter(Boolean) : [summary.image].filter(Boolean),
    emissionFactor: Number(product.EmissionFactor || 0),
    weightOptions: Array.isArray(product.WeightOptions) ? product.WeightOptions : [],
    fatContent: product.fatContent || '',
    condition: product.condition || product.status || '',
    postDate: product.post_date || product.createdAt || null,
  };
}

function buildIdLookupFilter(id) {
  const value = String(id || '').trim();
  if (!value) {
    return null;
  }

  if (mongoose.Types.ObjectId.isValid(value)) {
    return {
      $or: [
        { _id: new mongoose.Types.ObjectId(value) },
        { sku: value },
        { id: value },
      ],
    };
  }

  return {
    $or: [{ sku: value }, { id: value }],
  };
}

function buildExcludeProductFilter(product) {
  const conditions = [];

  if (product?._id) {
    conditions.push({ _id: { $ne: product._id } });
  }
  if (product?.sku) {
    conditions.push({ sku: { $ne: product.sku } });
  }

  return conditions.length ? { $and: conditions } : {};
}

function buildCategoryFilter(category) {
  const value = String(category || '').trim();
  if (!value) {
    return null;
  }

  return {
    $or: [
      { CategoryID: value },
      { categoryId: value },
      { SubcategoryID: value },
      { subcategoryId: value },
    ],
  };
}

function buildFieldRegexFilter(keyword) {
  const pattern = new RegExp(escapeRegex(keyword), 'i');
  return {
    $or: SEARCHABLE_FIELDS.map((field) => ({ [field]: pattern })),
  };
}

function buildTokenAndFilter(tokens) {
  return tokens.map((token) => buildFieldRegexFilter(token));
}

function buildKeywordsOrFilter(keywords = []) {
  const cleanKeywords = [...new Set(
    (keywords || [])
      .map((keyword) => String(keyword || '').trim())
      .filter((keyword) => keyword.length >= 2)
  )];

  if (!cleanKeywords.length) {
    return null;
  }

  return {
    $or: cleanKeywords.flatMap((keyword) => {
      const pattern = new RegExp(escapeRegex(keyword), 'i');
      return SEARCHABLE_FIELDS.map((field) => ({ [field]: pattern }));
    }),
  };
}

function scoreKeywordMatch(product, keyword) {
  const keywordLower = String(keyword || '').trim().toLowerCase();
  if (!keywordLower) {
    return 0;
  }

  const name = getProductName(product).toLowerCase();
  if (name.includes(keywordLower)) {
    return 3;
  }

  const secondaryText = [
    product.brand,
    product.ingredients,
    product.description,
  ].filter(Boolean).join(' ').toLowerCase();

  if (secondaryText.includes(keywordLower)) {
    return 1;
  }

  return 0;
}

function rankProductsByKeywordGroups(products = [], keywordGroups = []) {
  const groups = (keywordGroups || []).filter((group) => Array.isArray(group) && group.length);
  if (!groups.length) {
    return products;
  }

  return products
    .map((product) => {
      let bestGroupIndex = Infinity;
      let bestScore = 0;

      groups.forEach((group, groupIndex) => {
        for (const keyword of group) {
          const score = scoreKeywordMatch(product, keyword);
          if (score <= 0) {
            continue;
          }

          if (
            groupIndex < bestGroupIndex
            || (groupIndex === bestGroupIndex && score > bestScore)
          ) {
            bestGroupIndex = groupIndex;
            bestScore = score;
          }
        }
      });

      return { product, bestGroupIndex, bestScore };
    })
    .filter(({ bestGroupIndex }) => bestGroupIndex !== Infinity)
    .sort((left, right) => {
      if (left.bestGroupIndex !== right.bestGroupIndex) {
        return left.bestGroupIndex - right.bestGroupIndex;
      }
      if (left.bestScore !== right.bestScore) {
        return right.bestScore - left.bestScore;
      }

      const leftPopularity = Number(left.product.purchase_count || left.product.soldCount || 0);
      const rightPopularity = Number(right.product.purchase_count || right.product.soldCount || 0);
      return rightPopularity - leftPopularity;
    })
    .map(({ product }) => product);
}

function pickProductsAcrossKeywordGroups(groupedProducts = [], limit = DEFAULT_LIMIT) {
  const selected = [];
  const seenSkus = new Set();
  const maxRound = Math.max(...groupedProducts.map((products) => products.length), 0);

  for (let round = 0; round < maxRound && selected.length < limit; round += 1) {
    for (const products of groupedProducts) {
      const product = products[round];
      if (!product) {
        continue;
      }

      const sku = String(product.sku || product._id || '');
      if (!sku || seenSkus.has(sku)) {
        continue;
      }

      seenSkus.add(sku);
      selected.push(product);
      if (selected.length >= limit) {
        break;
      }
    }
  }

  return selected;
}

function buildTextSearchFilter(keyword) {
  const cleaned = extractProductSearchTerms(keyword) || String(keyword || '').trim();
  const tokens = parseSearchTokens(cleaned);
  if (!tokens.length) {
    return null;
  }

  return { $text: { $search: tokens.join(' ') } };
}

function buildSubcategoryFilter(subcategoryId) {
  const value = String(subcategoryId || '').trim();
  if (!value) {
    return null;
  }

  return {
    $or: [{ SubcategoryID: value }, { subcategoryId: value }],
  };
}

function buildCategoryOnlyFilter(categoryId) {
  const value = String(categoryId || '').trim();
  if (!value) {
    return null;
  }

  return {
    $or: [{ CategoryID: value }, { categoryId: value }],
  };
}

function dedupeBySku(products = []) {
  const seen = new Set();
  return products.filter((product) => {
    const sku = String(product?.sku || '').trim();
    if (!sku || seen.has(sku)) {
      return false;
    }
    seen.add(sku);
    return true;
  });
}

function mergeCartItem(product, cartItem) {
  if (!product || !cartItem) {
    return null;
  }

  return {
    ...product,
    cartQuantity: Number(cartItem.quantity || 1),
    selectedWeight: Number(cartItem.selectedWeight || 1),
    lineTotal: Number(product.price || 0) * Number(cartItem.quantity || 1),
  };
}

function normalizeQueryOptions(options = {}) {
  return {
    limit: Math.max(1, Math.min(Number(options.limit) || DEFAULT_LIMIT, 50)),
    sort: options.sort || DEFAULT_SORT,
    projection: options.projection || PRODUCT_LIST_PROJECTION,
  };
}

module.exports = {
  DEFAULT_LIMIT,
  DEFAULT_SORT,
  ACTIVE_PRODUCT_FILTER,
  PRODUCT_LIST_PROJECTION,
  SEARCHABLE_FIELDS,
  escapeRegex,
  parseSearchTokens,
  getProductName,
  firstImage,
  formatProductSummary,
  formatProductDetail,
  buildIdLookupFilter,
  buildExcludeProductFilter,
  buildCategoryFilter,
  buildFieldRegexFilter,
  buildTokenAndFilter,
  buildKeywordsOrFilter,
  scoreKeywordMatch,
  rankProductsByKeywordGroups,
  pickProductsAcrossKeywordGroups,
  buildTextSearchFilter,
  buildSubcategoryFilter,
  buildCategoryOnlyFilter,
  dedupeBySku,
  mergeCartItem,
  normalizeQueryOptions,
  extractProductSearchTerms,
};
