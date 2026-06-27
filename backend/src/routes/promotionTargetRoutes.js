const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const collection = () => mongoose.connection.db.collection('promotion_targets');

const normalizeTargetGroups = (body = {}) => {
  if (Array.isArray(body.target_groups)) {
    return body.target_groups
      .map(group => ({
        target_type: group?.target_type,
        target_ref: Array.isArray(group?.target_ref) ? group.target_ref : []
      }))
      .filter(group => group.target_type);
  }
  if (body.target_type) {
    return [{
      target_type: body.target_type,
      target_ref: Array.isArray(body.target_ref) ? body.target_ref : []
    }];
  }
  return [];
};

const resolveTargetDetails = async (target) => {
  const refs = Array.isArray(target.target_ref) ? target.target_ref : [];
  const targetType = target.target_type;
  const details = [];

  if (targetType === 'Category') {
    const categories = await mongoose.connection.db
      .collection('categories')
      .find({ CategoryID: { $in: refs } })
      .toArray();

    const categoryMap = new Map(categories.map(cat => [cat.CategoryID, cat]));
    refs.forEach(ref => {
      const category = categoryMap.get(ref);
      details.push({
        ref,
        name: category?.CategoryName || ref,
        type: 'Category'
      });
    });
  } else if (targetType === 'Subcategory') {
    const categories = await mongoose.connection.db.collection('categories').find().toArray();
    const subcategoryMap = new Map();

    categories.forEach(category => {
      if (Array.isArray(category.Subcategories)) {
        category.Subcategories.forEach(subcategory => {
          subcategoryMap.set(subcategory.SubcategoryID, {
            ...subcategory,
            categoryId: category.CategoryID,
            categoryName: category.CategoryName
          });
        });
      }
    });

    refs.forEach(ref => {
      const subcategory = subcategoryMap.get(ref);
      details.push({
        ref,
        name: subcategory?.SubcategoryName || ref,
        type: 'Subcategory',
        categoryId: subcategory?.categoryId,
        categoryName: subcategory?.categoryName
      });
    });
  } else if (targetType === 'Product') {
    const products = await mongoose.connection.db
      .collection('products')
      .find({ sku: { $in: refs } }, { projection: { sku: 1, product_name: 1, name: 1 } })
      .toArray();

    const productMap = new Map(products.map(product => [product.sku, product]));
    refs.forEach(ref => {
      const product = productMap.get(ref);
      details.push({
        ref,
        name: product?.product_name || product?.name || ref,
        type: 'Product'
      });
    });
  } else if (targetType === 'Brand') {
    refs.forEach(ref => {
      details.push({
        ref,
        name: ref,
        type: 'Brand'
      });
    });
  } else if (targetType === 'User') {
    const userTargetLabels = {
      'tier:bronze': 'Thành viên Đồng (Regular)',
      'tier:silver': 'Thành viên Bạc (Premium)',
      'tier:gold': 'Thành viên Vàng (VIP)',
      'certificate:CER001': 'Chứng nhận Green Starter',
      'certificate:CER002': 'Chứng nhận Eco Shopper',
      'certificate:CER003': 'Chứng nhận Carbon Saver',
      'certificate:CER004': 'Chứng nhận Green Hero',
      'certificate:CER005': 'Chứng nhận Earth Guardian',
    };
    refs.forEach(ref => {
      details.push({
        ref,
        name: userTargetLabels[ref] || ref,
        type: 'User'
      });
    });
  }

  return {
    ...target,
    target_groups: Array.isArray(target.target_groups) && target.target_groups.length
      ? target.target_groups
      : normalizeTargetGroups(target),
    target_details: details
  };
};

router.get('/', asyncHandler(async (_req, res) => {
  const targets = await collection().find({}).toArray();
  const resolvedTargets = await Promise.all(targets.map(resolveTargetDetails));
  res.json({ success: true, data: resolvedTargets });
}));

router.get('/:promotionId', asyncHandler(async (req, res) => {
  const target = await collection().findOne({ promotion_id: req.params.promotionId });
  if (!target) {
    return res.status(404).json({ success: false, message: 'Promotion target not found' });
  }
  res.json({ success: true, data: await resolveTargetDetails(target) });
}));

router.post('/', asyncHandler(async (req, res) => {
  const promotionId = req.body.promotion_id;
  if (!promotionId) {
    return res.status(400).json({ success: false, message: 'promotion_id is required' });
  }

  const targetGroups = normalizeTargetGroups(req.body);
  const primaryGroup = targetGroups.find(group => group.target_type !== 'Order' && group.target_type !== 'Shipping') || targetGroups[0] || {};
  const payload = {
    promotion_id: promotionId,
    target_type: primaryGroup.target_type || req.body.target_type,
    target_ref: Array.isArray(primaryGroup.target_ref) ? primaryGroup.target_ref : [],
    target_groups: targetGroups,
    updated_at: new Date()
  };

  const result = await collection().findOneAndUpdate(
    { promotion_id: promotionId },
    { $set: payload, $setOnInsert: { created_at: new Date() } },
    { upsert: true, returnDocument: 'after' }
  );

  res.status(201).json({ success: true, data: result.value || payload });
}));

router.put('/:promotionId', asyncHandler(async (req, res) => {
  const targetGroups = normalizeTargetGroups(req.body);
  const primaryGroup = targetGroups.find(group => group.target_type !== 'Order' && group.target_type !== 'Shipping') || targetGroups[0] || {};
  const payload = {
    promotion_id: req.params.promotionId,
    target_type: primaryGroup.target_type || req.body.target_type,
    target_ref: Array.isArray(primaryGroup.target_ref) ? primaryGroup.target_ref : [],
    target_groups: targetGroups,
    updated_at: new Date()
  };

  const result = await collection().findOneAndUpdate(
    { promotion_id: req.params.promotionId },
    { $set: payload, $setOnInsert: { created_at: new Date() } },
    { upsert: true, returnDocument: 'after' }
  );

  res.json({ success: true, data: result.value || payload });
}));

router.delete('/:promotionId', asyncHandler(async (req, res) => {
  await collection().deleteOne({ promotion_id: req.params.promotionId });
  res.json({ success: true });
}));

module.exports = router;
