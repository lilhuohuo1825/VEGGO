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
  const query = showAll
    ? {}
    : {
        $or: [
          { isActive: true },
          { isActive: { $exists: false }, status: { $ne: 'Inactive' } },
          { status: 'Active' },
        ],
      };

  const products = await mongoose.connection.db.collection('products')
    .find(query)
    .sort({ createdAt: -1, post_date: -1 })
    .toArray();

  // Tải danh mục từ collection 'categories' để map tên
  const categories = await mongoose.connection.db.collection('categories').find().toArray();
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

  const mappedProducts = products.map(product => {
    const normalized = normalizeProduct(product);
    return {
      ...product,
      ...normalized,
      category: categoryMap[product.CategoryID || product.categoryId] || '',
      subcategory: subcategoryMap[product.SubcategoryID || product.subcategoryId] || '',
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

// Get single product (supports ObjectId, sku, or ID fallback)
router.get('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ sku: req.params.id }, { id: req.params.id }] };
  }

  const product = await mongoose.connection.db.collection('products').findOne(query);
  if (!product) {
    return res.status(404).json({ message: 'Product not found' });
  }

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
    originalPrice: product.originalPrice || product.base_price || product.price || 0,
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

function firstImage(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}
