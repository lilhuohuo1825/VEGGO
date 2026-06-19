const express = require('express');
const mongoose = require('mongoose');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/', asyncHandler(async (req, res) => {
  const products = await mongoose.connection.db.collection('products')
    .find({
      $or: [
        { isActive: true },
        { isActive: { $exists: false }, status: { $ne: 'Inactive' } },
        { status: 'Active' },
      ],
    })
    .sort({ createdAt: -1, post_date: -1 })
    .toArray();
  res.json(products.map(normalizeProduct));
}));

router.get('/:id', asyncHandler(async (req, res) => {
  if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
    return res.status(400).json({ message: 'Invalid product id' });
  }

  const product = await mongoose.connection.db.collection('products').findOne({
    _id: new mongoose.Types.ObjectId(req.params.id),
  });
  if (!product) {
    return res.status(404).json({ message: 'Product not found' });
  }
  res.json(normalizeProduct(product));
}));

router.post('/', asyncHandler(async (req, res) => {
  const product = await Product.create(req.body);
  res.status(201).json(product);
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
