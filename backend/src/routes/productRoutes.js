const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');
const Product = require('../models/Product');

const router = express.Router();

// ─────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────

const VALID_TABS = ['popular', 'trending', 'newest', 'best_price', 'price_desc', 'price_asc', 'top_rated'];
const DEFAULT_TAB = 'popular';
const HOME_LIMIT = 24;

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

function parsePromotionDate(value) {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'object' && value.$date) return new Date(value.$date);
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function isPromotionCurrentlyActive(promo, now = new Date()) {
  const status = String(promo.status || '').trim().toLowerCase();
  if (status === 'inactive' || status === 'expired') return false;
  if (promo.isActive === false || promo.show_on_app === false) return false;

  const startDate = parsePromotionDate(promo.start_date);
  const endDate = parsePromotionDate(promo.end_date);
  if (startDate && startDate > now) return false;
  if (endDate && endDate < now) return false;
  return true;
}

function isFlashSalePromotion(promo) {
  return String(promo.promotion_kind || '').trim().toLowerCase() === 'flashsale';
}

function calculateFlashSalePrice(originalPrice, promo) {
  const discountValue = Number(promo.discount_value || promo.discount || 0);
  if (promo.discount_type === 'fixed') {
    return Math.max(0, originalPrice - discountValue);
  }
  if (promo.discount_type === 'buy1get1') {
    return originalPrice;
  }
  return Math.max(0, Math.round(originalPrice * (100 - discountValue) / 100));
}

function getPromotionTargetGroups(target) {
  if (!target) return [];
  if (Array.isArray(target.target_groups) && target.target_groups.length) {
    return target.target_groups
      .map(group => ({
        target_type: group?.target_type,
        target_ref: Array.isArray(group?.target_ref) ? group.target_ref : [],
      }))
      .filter(group => group.target_type);
  }
  if (target.target_type) {
    return [{
      target_type: target.target_type,
      target_ref: Array.isArray(target.target_ref) ? target.target_ref : [],
    }];
  }
  return [];
}

function getTargetRefsByType(target, targetType) {
  return getPromotionTargetGroups(target)
    .filter(group => group.target_type === targetType)
    .flatMap(group => group.target_ref);
}

async function getActiveFlashSaleBySku(skus) {
  const cleanSkus = [...new Set((skus || []).map(sku => String(sku || '').trim()).filter(Boolean))];
  const result = new Map();
  if (!cleanSkus.length) return result;

  const db = mongoose.connection.db;
  const targets = await db.collection('promotion_targets')
    .find({
      $or: [
        { target_type: 'Product', target_ref: { $in: cleanSkus } },
        { 'target_groups.target_type': 'Product', 'target_groups.target_ref': { $in: cleanSkus } },
      ],
    })
    .toArray();
  const promotionIds = [...new Set(targets.map(target => target.promotion_id).filter(Boolean))];
  if (!promotionIds.length) return result;

  const now = new Date();
  const promos = await db.collection('promotions')
    .find({ promotion_id: { $in: promotionIds }, show_on_app: { $ne: false }, status: { $ne: 'Inactive' } })
    .toArray();
  const activePromoById = new Map(
    promos
      .filter(promo => isPromotionCurrentlyActive(promo, now) && isFlashSalePromotion(promo))
      .map(promo => [promo.promotion_id, promo])
  );

  targets.forEach(target => {
    const promo = activePromoById.get(target.promotion_id);
    if (!promo) return;
    getTargetRefsByType(target, 'Product').forEach(sku => {
      const skuKey = String(sku);
      if (cleanSkus.includes(skuKey) && !result.has(skuKey)) {
        result.set(skuKey, promo);
      }
    });
  });

  return result;
}

function applyFlashSalePricing(product, promo) {
  if (!product || !promo) return product;
  const listPrice = Number(product.price || 0);
  const salePrice = calculateFlashSalePrice(listPrice, promo);
  return {
    ...product,
    price: salePrice,
    originalPrice: listPrice,
    activePromotionId: promo.promotion_id,
    activePromotionKind: promo.promotion_kind,
  };
}

function hasDisplayDiscount(product) {
  const price = Number(product?.price || 0);
  const originalPrice = Number(product?.originalPrice || 0);
  return Boolean(product?.activePromotionId) || (originalPrice > price && originalPrice > 0);
}

/**
 * Trả về aggregation pipeline hoặc sort object tương ứng với tab.
 *
 * Tab         Logic
 * ─────────── ──────────────────────────────────────────────────────
 * popular     score = purchase_count + liked + (rating * 100)  DESC
 * trending    purchase_count DESC
 * newest      post_date DESC
 * top_rated   rating DESC → purchase_count DESC
 * best_price  price ASC
 */
function getPipelineByTab(tab, limit, skip = 0) {
  const baseMatch = { $match: { status: 'Active' } };
  const skipStage = { $skip: skip };
  const limitStage = { $limit: limit };

  switch (tab) {
    case 'popular':
      // Tính score tổng hợp: purchase_count + liked + rating*100
      // rating (0-5) × 100 để cân bằng với purchase_count/liked
      return [
        baseMatch,
        {
          $addFields: {
            popularScore: {
              $add: [
                { $ifNull: ['$purchase_count', 0] },
                { $ifNull: ['$liked', 0] },
                { $multiply: [{ $ifNull: ['$rating', 0] }, 100] },
              ],
            },
          },
        },
        { $sort: { popularScore: -1 } },
        skipStage,
        limitStage,
        { $project: { popularScore: 0 } },
      ];

    case 'trending':
      return [baseMatch, { $sort: { purchase_count: -1 } }, skipStage, limitStage];

    case 'newest':
      return [baseMatch, { $sort: { post_date: -1 } }, skipStage, limitStage];

    case 'top_rated':
      return [baseMatch, { $sort: { rating: -1, purchase_count: -1 } }, skipStage, limitStage];

    case 'best_price':
    case 'price_desc':
      return [baseMatch, { $sort: { price: -1 } }, skipStage, limitStage];

    case 'price_asc':
      return [baseMatch, { $sort: { price: 1 } }, skipStage, limitStage];

    default:
      return [baseMatch, { $sort: { purchase_count: -1 } }, skipStage, limitStage];
  }
}

// ─────────────────────────────────────────────
// GET /api/products/home?tab=popular
// Dành riêng cho Trang chủ – limit 25, chỉ Active
// ─────────────────────────────────────────────
router.get('/home', asyncHandler(async (req, res) => {
  const rawTab = req.query.tab;
  const tab = VALID_TABS.includes(rawTab) ? rawTab : DEFAULT_TAB;
  
  const limit = parseInt(req.query.limit) || HOME_LIMIT;
  const skip = parseInt(req.query.skip) || 0;

  const pipeline = getPipelineByTab(tab, limit, skip);

  const products = await Product.aggregate(pipeline);

  res.json({
    success: true,
    message: 'Products retrieved successfully',
    count: products.length,
    tab,
    data: products,
  });
}));

// ─────────────────────────────────────────────
// GET /api/products          – Lấy toàn bộ sản phẩm
// GET /api/products/:id      – Chi tiết 1 sản phẩm
// POST /api/products         – Tạo sản phẩm mới
// ─────────────────────────────────────────────

router.get('/', asyncHandler(async (req, res) => {
  const showAll = req.query.all === 'true';
  const lite = req.query.lite === 'true';
  const query = showAll
    ? {}
    : {
        $or: [
          { isActive: true },
          { isActive: { $exists: false }, status: { $ne: 'Inactive' } },
          { status: 'Active' },
        ],
      };

  const db = mongoose.connection.db;
  const projection = lite
    ? {
        product_name: 1,
        name: 1,
        sku: 1,
        brand: 1,
        unit: 1,
        price: 1,
        base_price: 1,
        originalPrice: 1,
        image: 1,
        imageUrl: 1,
        stock: 1,
        quantity: 1,
        quantity_available: 1,
        Quantity: 1,
        CategoryID: 1,
        SubcategoryID: 1,
        categoryId: 1,
        subcategoryId: 1,
        category: 1,
        subcategory: 1,
        rating: 1,
        reviewCount: 1,
        purchase_count: 1,
        soldCount: 1,
        liked: 1,
        status: 1,
        isActive: 1,
        post_date: 1,
        createdAt: 1,
        updatedAt: 1,
        groups: 1,
        EmissionFactor: 1,
        AllowCustomWeight: 1,
        allowCustomWeight: 1,
        WeightOptions: 1,
      }
    : { image: 0, imageUrl: 0 };
  const products = await db.collection('products')
    .find(query, { projection })
    .sort({ createdAt: -1, post_date: -1 })
    .toArray();

  const [reviewStatsBySku, categories] = await Promise.all([
    lite ? Promise.resolve(new Map()) : getReviewStatsBySku(products.map(product => product.sku)),
    db.collection('categories').find().toArray(),
  ]);
  const categoryMap = {};
  const subcategoryMap = {};

  categories.forEach(cat => {
    categoryMap[cat.CategoryID] = cat.CategoryName;
    if (cat.Subcategories && Array.isArray(cat.Subcategories)) {
      cat.Subcategories.forEach(sub => {
        subcategoryMap[sub.SubcategoryID] = sub.SubcategoryName;
      });
    }
  });

  const flashSaleBySku = await getActiveFlashSaleBySku(products.map(product => product.sku));

  const mappedProducts = products.map(product => {
    const priced = applyFlashSalePricing(product, flashSaleBySku.get(String(product.sku)));
    const normalized = normalizeProduct(priced);
    const reviewStats = reviewStatsBySku.get(String(product.sku)) || {
      rating: normalized.rating,
      reviewCount: normalized.reviewCount,
    };
    return {
      ...priced,
      ...normalized,
      rating: reviewStats.rating,
      reviewCount: reviewStats.reviewCount,
      hasDisplayDiscount: hasDisplayDiscount(priced),
      image: sanitizeListImage(priced.image),
      imageUrl: sanitizeListImage(priced.imageUrl || normalized.imageUrl),
      category: categoryMap[priced.CategoryID || priced.categoryId] || priced.category || normalized.category || '',
      subcategory: subcategoryMap[priced.SubcategoryID || priced.subcategoryId] || priced.subcategory || normalized.subcategory || '',
    };
  });

  res.json(mappedProducts);
}));

router.get('/metadata/categories', asyncHandler(async (_req, res) => {
  const categories = await mongoose.connection.db.collection('categories').find().toArray();
  const data = categories
    .map(cat => ({
      id: cat.CategoryID || cat.id || cat._id?.toString(),
      name: cat.CategoryName || cat.name || cat.category || cat.CategoryID
    }))
    .filter(cat => cat.id || cat.name)
    .sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'vi'));
  res.json({ success: true, data });
}));

router.get('/metadata/subcategories', asyncHandler(async (_req, res) => {
  const categories = await mongoose.connection.db.collection('categories').find().toArray();
  const subcategories = [];

  categories.forEach(cat => {
    if (Array.isArray(cat.Subcategories)) {
      cat.Subcategories.forEach(sub => {
        const value = {
          id: sub.SubcategoryID || sub.id || sub._id?.toString(),
          name: sub.SubcategoryName || sub.name || sub.SubcategoryID,
          categoryId: cat.CategoryID || cat.id || cat._id?.toString()
        };
        if (value.id || value.name) subcategories.push(value);
      });
    }
  });

  const seen = new Set();
  const data = subcategories
    .filter(sub => {
      const key = sub.id || sub.name;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'vi'));

  res.json({ success: true, data });
}));

router.get('/metadata/brands', asyncHandler(async (_req, res) => {
  const brands = await mongoose.connection.db
    .collection('products')
    .distinct('brand', { brand: { $nin: [null, ''] } });
  res.json({ success: true, data: brands.sort() });
}));

router.get('/metadata/products', asyncHandler(async (_req, res) => {
  const products = await mongoose.connection.db
    .collection('products')
    .find({}, { projection: { sku: 1, product_name: 1, name: 1, price: 1, unit: 1, image: 1, rating: 1, stock: 1 } })
    .toArray();

  res.json({
    success: true,
    data: products
      .map(product => ({
        sku: product.sku,
        name: product.product_name || product.name || product.sku,
        price: product.price || 0,
        unit: product.unit || '',
        imageUrl: Array.isArray(product.image) ? product.image[0] : product.image || '',
        rating: product.rating || 0,
        stock: product.stock || 0
      }))
      .filter(product => product.sku)
  });
}));

router.get('/groups', asyncHandler(async (_req, res) => {
  const groups = await mongoose.connection.db
    .collection('products')
    .distinct('groups', { groups: { $exists: true, $ne: [] } });

  res.json({
    success: true,
    data: groups
      .filter(group => typeof group === 'string' && group.trim())
      .sort((a, b) => a.localeCompare(b, 'vi')),
  });
}));

router.post('/groups', asyncHandler(async (req, res) => {
  const groupName = typeof req.body.groupName === 'string' ? req.body.groupName.trim() : '';
  const skus = Array.isArray(req.body.skus)
    ? req.body.skus.map(sku => String(sku).trim()).filter(Boolean)
    : [];

  if (!groupName) {
    return res.status(400).json({ success: false, message: 'Group name is required' });
  }

  if (skus.length === 0) {
    return res.status(400).json({ success: false, message: 'At least one SKU is required' });
  }

  const result = await mongoose.connection.db.collection('products').updateMany(
    { sku: { $in: skus } },
    { $addToSet: { groups: groupName } }
  );

  res.json({
    success: true,
    message: `Đã tạo nhóm "${groupName}"`,
    matchedCount: result.matchedCount,
    modifiedCount: result.modifiedCount,
  });
}));

router.patch('/:sku/groups', asyncHandler(async (req, res) => {
  const groupName = typeof req.body.groupName === 'string' ? req.body.groupName.trim() : '';
  const action = String(req.body.action || '').trim().toLowerCase();

  if (!groupName) {
    return res.status(400).json({ success: false, message: 'Group name is required' });
  }

  if (!['add', 'remove'].includes(action)) {
    return res.status(400).json({ success: false, message: 'Valid action is required' });
  }

  const update = action === 'add'
    ? { $addToSet: { groups: groupName } }
    : { $pull: { groups: groupName } };

  const result = await mongoose.connection.db.collection('products').findOneAndUpdate(
    { sku: req.params.sku },
    update,
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ success: false, message: 'Product not found' });
  }

  res.json({ success: true, data: result });
}));

router.patch('/:id/field', asyncHandler(async (req, res) => {
  const field = typeof req.body.field === 'string' ? req.body.field.trim() : '';

  if (!field || field === '_id' || field.includes('$') || field.includes('.')) {
    return res.status(400).json({ success: false, message: 'Valid field is required' });
  }

  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ sku: req.params.id }, { id: req.params.id }] };
  }

  let value = req.body.value;
  if (field === 'EmissionFactor') {
    value = Number(value) || 0;
  }
  if (field === 'allowCustomWeight' || field === 'AllowCustomWeight') {
    value = value === true || value === 'true';
  }

  const result = await mongoose.connection.db.collection('products').findOneAndUpdate(
    query,
    { $set: { [field]: value, updatedAt: new Date() } },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ success: false, message: 'Product not found' });
  }

  res.json({ success: true, data: result });
}));

// Get single product (supports ObjectId, sku, or ID fallback)
router.get('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ sku: req.params.id }, { id: req.params.id }] };
  }

  const rawProduct = await mongoose.connection.db.collection('products').findOne(query);
  if (!rawProduct) {
    return res.status(404).json({ message: 'Product not found' });
  }

  const flashSaleBySku = await getActiveFlashSaleBySku([rawProduct.sku]);
  const product = applyFlashSalePricing(rawProduct, flashSaleBySku.get(String(rawProduct.sku)));

  const reviewStats = await getReviewStats(product.sku);

  // Lấy thêm tên danh mục và danh mục con
  const catDoc = await mongoose.connection.db.collection('categories').findOne({ CategoryID: product.CategoryID });
  product.category = catDoc ? catDoc.CategoryName : '';
  if (catDoc && catDoc.Subcategories) {
    const sub = catDoc.Subcategories.find(s => s.SubcategoryID === product.SubcategoryID);
    product.subcategory = sub ? sub.SubcategoryName : '';
  } else {
    product.subcategory = '';
  }

  res.json({
    ...product,
    ...normalizeProduct(product),
    rating: reviewStats.rating,
    reviewCount: reviewStats.reviewCount,
    category: product.category,
    subcategory: product.subcategory,
  });
}));

// Create product
router.post('/', asyncHandler(async (req, res) => {
  const newProduct = {
    ...req.body,
    status: req.body.status || 'Active',
    EmissionFactor: req.body.EmissionFactor !== undefined ? Number(req.body.EmissionFactor) : 0,
    allowCustomWeight: req.body.allowCustomWeight === true || req.body.allowCustomWeight === 'true'
  };
  const result = await mongoose.connection.db.collection('products').insertOne(newProduct);
  res.status(201).json({ _id: result.insertedId, ...newProduct });
}));

// Update product
router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { sku: req.params.id };
  }

  const updateData = { ...req.body };
  delete updateData._id;

  if (updateData.EmissionFactor !== undefined) {
    updateData.EmissionFactor = Number(updateData.EmissionFactor);
  }
  if (updateData.allowCustomWeight !== undefined) {
    updateData.allowCustomWeight = updateData.allowCustomWeight === true || updateData.allowCustomWeight === 'true';
  }

  await mongoose.connection.db.collection('products').updateOne(query, { $set: updateData });
  const updatedProduct = await mongoose.connection.db.collection('products').findOne(query);
  res.json(updatedProduct);
}));

// Soft delete / Toggle active status of product (setting status to 'Inactive' or 'Active')
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { sku: req.params.id };
  }

  const product = await mongoose.connection.db.collection('products').findOne(query);
  if (!product) {
    return res.status(404).json({ message: 'Product not found' });
  }

  const newStatus = product.status === 'Inactive' ? 'Active' : 'Inactive';
  await mongoose.connection.db.collection('products').updateOne(query, { $set: { status: newStatus } });
  res.json({ success: true, status: newStatus });
}));

module.exports = router;

function normalizeProduct(product) {
  return {
    _id: String(product._id),
    name: product.name || product.product_name || product.productName || '',
    sku: product.sku || '',
    categoryId: product.categoryId || product.CategoryID || '',
    subcategoryId: product.subcategoryId || product.SubcategoryID || '',
    description: product.description || product.usage || '',
    price: product.price || 0,
    originalPrice: product.originalPrice || product.price || 0,
    unit: product.unit || '',
    imageUrl: product.imageUrl || firstImage(product.image) || '',
    stock: product.stock || 0,
    rating: product.rating || 0,
    isActive: product.isActive ?? product.status !== 'Inactive',
    weight: product.weight || product.unit || '',
    reviewCount: product.reviewCount || 0,
    soldCount: product.soldCount || product.purchase_count || 0,
    origin: product.origin || '',
    condition: product.condition || product.status || '',
    fatContent: product.fatContent || product.brand || '',
  };
}

async function getReviewStats(sku) {
  if (!sku) {
    return { rating: 0, reviewCount: 0 };
  }

  const reviewDoc = await mongoose.connection.db.collection('reviews').findOne({ sku: String(sku) });
  const reviews = Array.isArray(reviewDoc?.reviews) ? reviewDoc.reviews : [];
  const validReviews = reviews.filter(review => Number.isFinite(Number(review.rating)));
  if (validReviews.length === 0) {
    return { rating: 0, reviewCount: 0 };
  }

  const total = validReviews.reduce((sum, review) => sum + Number(review.rating), 0);
  return {
    rating: Math.round((total / validReviews.length) * 10) / 10,
    reviewCount: validReviews.length,
  };
}

async function getReviewStatsBySku(skus) {
  const cleanSkus = [...new Set((skus || [])
    .map(sku => sku == null ? '' : String(sku))
    .filter(Boolean))];
  const stats = new Map();
  if (cleanSkus.length === 0) {
    return stats;
  }

  let reviewDocs = [];
  try {
    reviewDocs = await mongoose.connection.db.collection('reviews')
      .find(
        { sku: { $in: cleanSkus } },
        { projection: { sku: 1, reviews: 1 }, maxTimeMS: 1500 }
      )
      .toArray();
  } catch (error) {
    console.warn('[GET /api/products] Review stats skipped:', error.message);
    return stats;
  }

  reviewDocs.forEach(doc => {
    const reviews = Array.isArray(doc.reviews) ? doc.reviews : [];
    const validReviews = reviews.filter(review => Number.isFinite(Number(review.rating)));
    if (validReviews.length === 0) {
      stats.set(String(doc.sku), { rating: 0, reviewCount: 0 });
      return;
    }

    const total = validReviews.reduce((sum, review) => sum + Number(review.rating), 0);
    stats.set(String(doc.sku), {
      rating: Math.round((total / validReviews.length) * 10) / 10,
      reviewCount: validReviews.length,
    });
  });

  return stats;
}

function firstImage(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}

function sanitizeListImage(value) {
  const image = firstImage(value);
  if (!image) return '';
  return String(image);
}
