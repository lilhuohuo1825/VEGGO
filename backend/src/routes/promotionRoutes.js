const express = require('express');
const Promotion = require('../models/Promotion');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// Lấy host thực của server (IP:PORT) từ env hoặc fallback sang req.get('host')
// Ưu tiên SERVER_HOST (đặt trong .env), nếu không thì dùng HOST_IP + PORT
function getServerRoot(req) {
  if (process.env.SERVER_HOST) {
    return `http://${process.env.SERVER_HOST}`;
  }
  const hostIp = process.env.HOST_IP || req.get('host');
  const port = process.env.PORT || 5001;
  // Nếu req.get('host') đã chứa port thì dùng luôn, không thêm nữa
  if (hostIp && hostIp.includes(':')) {
    return `http://${hostIp}`;
  }
  return `http://${hostIp}:${port}`;
}

router.get('/', asyncHandler(async (req, res) => {
  const promotions = await Promotion.find({
    isActive: { $ne: false },
    show_on_app: true,
    promotion_id: { $in: ['PROMO010', 'PROMO011', 'PROMO012', 'PROMO013', 'PROMO014'] },
  }).sort({ promotion_id: 1 });

  const serverRoot = getServerRoot(req);
  res.json(promotions.map((p) => normalizePromotionBanner(p.toObject(), serverRoot)));
}));

function normalizePromotionBanner(promotion, serverRoot) {
  const imagePath = promotion.banner_data?.imageUrl || promotion.imageUrl;
  if (!imagePath) return promotion;

  const publicUrl = buildPublicUrl(imagePath, serverRoot);
  promotion.imageUrl = imagePath;
  promotion.banner_data = {
    ...(promotion.banner_data || {}),
    imageUrl: imagePath,
    src: publicUrl,          // URL có thể truy cập từ mạng nội bộ
    showOnApp: true,
  };
  return promotion;
}

function buildPublicUrl(imagePath, serverRoot) {
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    // Thay localhost/127.0.0.1 bằng serverRoot
    return imagePath
      .replace(/http:\/\/localhost:\d+/, serverRoot)
      .replace(/http:\/\/127\.0\.0\.1:\d+/, serverRoot);
  }
  const path = imagePath.startsWith('/') ? imagePath : `/${imagePath}`;
  return `${serverRoot}${path}`;
}

router.get('/:id', asyncHandler(async (req, res) => {
  const promotion = await Promotion.findOne({ 
    promotion_id: req.params.id, 
    isActive: { $ne: false } 
  });
  
  if (!promotion) {
    return res.status(404).json({ message: 'Promotion not found' });
  }
  
  const serverRoot = getServerRoot(req);
  res.json(normalizePromotionBanner(promotion.toObject(), serverRoot));
}));

const PromotionTarget = require('../models/PromotionTarget');
const Product = require('../models/Product');

router.get('/:id/products', asyncHandler(async (req, res) => {
  const { id: promotion_id } = req.params;
  const limit = parseInt(req.query.limit) || 0;
  const skip = parseInt(req.query.skip) || 0;

  if (['PROMO010', 'PROMO011'].includes(promotion_id)) {
    let dbQuery = Product.find({ status: 'Active' });
    if (skip > 0) dbQuery = dbQuery.skip(skip);
    if (limit > 0) dbQuery = dbQuery.limit(limit);
    else dbQuery = dbQuery.limit(30);
    const products = await dbQuery;
    return res.json({ success: true, count: products.length, data: products });
  }

  const target = await PromotionTarget.findOne({ promotion_id });
  if (!target) {
    return res.json({ success: true, count: 0, data: [] });
  }

  const { target_type, target_ref } = target;
  let query = { status: 'Active' };

  if (target_type === 'Brand') {
    query.brand = { $in: target_ref };
  } else if (target_type === 'Subcategory') {
    query.SubcategoryID = { $in: target_ref };
  } else if (target_type === 'Category') {
    query.CategoryID = { $in: target_ref };
  } else if (target_type === 'Product') {
    query.sku = { $in: target_ref };
  }

  let dbQuery = Product.find(query);
  if (skip > 0) {
    dbQuery = dbQuery.skip(skip);
  }
  if (limit > 0) {
    dbQuery = dbQuery.limit(limit);
  }

  const products = await dbQuery;
  res.json({ success: true, count: products.length, data: products });
}));

module.exports = router;
