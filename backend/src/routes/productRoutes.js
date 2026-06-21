const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// Get all products (Active or All)
router.get('/', asyncHandler(async (req, res) => {
  const showAll = req.query.all === 'true';
  const query = showAll ? {} : { status: { $ne: 'Inactive' } };
  
  const products = await mongoose.connection.db
    .collection('products')
    .find(query)
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
    product.category = categoryMap[product.CategoryID] || '';
    product.subcategory = subcategoryMap[product.SubcategoryID] || '';
    return product;
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

  res.json(product);
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
