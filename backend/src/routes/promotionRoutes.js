const express = require('express');
const mongoose = require('mongoose');
const crypto = require('crypto');
const multer = require('multer');
const { v2: cloudinary } = require('cloudinary');
const admin = require('../config/firebaseAdmin');
const Promotion = require('../models/Promotion');
const PromotionTarget = require('../models/PromotionTarget');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

function getServerRoot(req) {
  const configuredBaseUrl = process.env.PUBLIC_API_BASE_URL || process.env.API_PUBLIC_BASE_URL;
  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/$/, '');
  }
  if (process.env.SERVER_HOST) {
    return `http://${process.env.SERVER_HOST}`;
  }
  const hostIp = process.env.HOST_IP || req.get('host');
  const port = process.env.PORT || 5001;
  if (hostIp && hostIp.includes(':')) {
    return `http://${hostIp}`;
  }
  return `http://${hostIp}:${port}`;
}

function buildPublicUrl(imagePath, serverRoot) {
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
      .replace(/http:\/\/localhost:\d+/, serverRoot)
      .replace(/http:\/\/127\.0\.0\.1:\d+/, serverRoot);
  }
  const path = imagePath.startsWith('/') ? imagePath : `/${imagePath}`;
  return `${serverRoot}${path}`;
}

function normalizePromotionBanner(promotion, serverRoot) {
  const imagePath = promotion.banner_data?.imageUrl || promotion.imageUrl;
  if (!imagePath) return promotion;

  promotion.imageUrl = imagePath;
  promotion.banner_data = {
    ...(promotion.banner_data || {}),
    imageUrl: imagePath,
    src: buildPublicUrl(imagePath, serverRoot),
    showOnApp: promotion.banner_data?.showOnApp ?? promotion.show_on_app ?? true,
  };
  return promotion;
}

const getFirebaseStorageBucketNames = () => {
  if (!admin.apps || admin.apps.length === 0) {
    throw new Error('Firebase Admin is not configured');
  }

  const projectId = admin.app().options.projectId;
  const bucketNames = [
    process.env.FIREBASE_STORAGE_BUCKET,
    admin.app().options.storageBucket,
    projectId ? `${projectId}.firebasestorage.app` : undefined,
    projectId ? `${projectId}.appspot.com` : undefined,
  ].filter(Boolean);

  const uniqueBucketNames = [...new Set(bucketNames)];
  if (!uniqueBucketNames.length) {
    throw new Error('FIREBASE_STORAGE_BUCKET is missing');
  }

  return uniqueBucketNames;
};

const sanitizeFileName = (fileName = 'banner') => {
  return String(fileName)
    .trim()
    .replace(/[^a-zA-Z0-9._-]/g, '-')
    .replace(/-+/g, '-')
    .slice(0, 80) || 'banner';
};

const buildFirebaseDownloadUrl = (bucketName, filePath, downloadToken) => {
  return `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodeURIComponent(filePath)}?alt=media&token=${downloadToken}`;
};

const isCloudinaryConfigured = () => {
  return Boolean(
    process.env.CLOUDINARY_CLOUD_NAME
    && process.env.CLOUDINARY_API_KEY
    && process.env.CLOUDINARY_API_SECRET
  );
};

const uploadBannerToCloudinary = (file) => {
  if (!isCloudinaryConfigured()) {
    throw new Error('Cloudinary credentials are missing');
  }

  cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET,
    secure: true
  });

  const publicId = `${Date.now()}-${crypto.randomBytes(6).toString('base64url')}`;

  return new Promise((resolve, reject) => {
    const stream = cloudinary.uploader.upload_stream(
      {
        folder: 'veggo/promotion-banners',
        public_id: publicId,
        resource_type: 'image',
        overwrite: false,
      },
      (error, result) => {
        if (error) {
          reject(error);
          return;
        }

        resolve({
          token: result.public_id,
          imageUrl: result.secure_url,
          publicId: result.public_id,
          storageProvider: 'cloudinary'
        });
      }
    );

    stream.end(file.buffer);
  });
};

const uploadBannerToFirebaseStorage = async (file) => {
  const bucketNames = getFirebaseStorageBucketNames();
  const uploadToken = crypto.randomBytes(8).toString('base64url');
  const downloadToken = crypto.randomUUID();
  const safeName = sanitizeFileName(file.originalname);
  const storagePath = `promotion-banners/${Date.now()}-${uploadToken}-${safeName}`;
  const errors = [];

  for (const bucketName of bucketNames) {
    try {
      const bucket = admin.storage().bucket(bucketName);
      const storageFile = bucket.file(storagePath);

      await storageFile.save(file.buffer, {
        resumable: false,
        contentType: file.mimetype || 'image/jpeg',
        metadata: {
          cacheControl: 'public, max-age=31536000',
          metadata: {
            firebaseStorageDownloadTokens: downloadToken,
            originalName: file.originalname || '',
          },
        },
      });

      return {
        token: uploadToken,
        imageUrl: buildFirebaseDownloadUrl(bucket.name, storagePath, downloadToken),
        storagePath,
        bucket: bucket.name,
        storageProvider: 'firebase'
      };
    } catch (error) {
      errors.push(`${bucketName}: ${error.message}`);
      console.error(`[Firebase Storage] Upload failed for bucket ${bucketName}:`, error.message);
    }
  }

  throw new Error(errors.join(' | '));
};

const uploadBannerToMongo = async (req, file, reason) => {
  const token = crypto.randomBytes(6).toString('base64url');

  await mongoose.connection.db.collection('promoBannerImages').insertOne({
    token,
    mimeType: file.mimetype || 'image/jpeg',
    data: file.buffer,
    size: file.size,
    originalName: file.originalname || '',
    storageProvider: 'mongodb',
    fallbackReason: reason || '',
    createdAt: new Date().toISOString()
  });

  const imagePath = `/api/promo-images/${token}`;
  const imageUrl = `${getServerRoot(req)}${imagePath}`;
  return { token, imageUrl, imagePath, storageProvider: 'mongodb' };
};

const getNextPromotionId = async () => {
  const latest = await mongoose.connection.db
    .collection('promotions')
    .find({ promotion_id: /^PROMO\d+$/ })
    .project({ promotion_id: 1 })
    .toArray();

  const maxNumber = latest.reduce((max, promo) => {
    const match = String(promo.promotion_id || '').match(/^PROMO(\d+)$/);
    return match ? Math.max(max, Number(match[1])) : max;
  }, 0);

  return `PROMO${String(maxNumber + 1).padStart(3, '0')}`;
};

const normalizePromotionKind = (payload = {}) => {
  const promotionKind = payload.promotion_kind === 'FlashSale' || payload.promotionKind === 'FlashSale'
    ? 'FlashSale'
    : 'Promotion';

  return {
    promotion_kind: promotionKind,
    display_section: payload.display_section || payload.displaySection || (promotionKind === 'FlashSale' ? 'flash_sale' : 'promotion')
  };
};

const normalizePromotionPayload = (payload = {}, existing = {}) => {
  const source = { ...existing, ...payload };
  const normalized = {
    ...payload,
    ...normalizePromotionKind(source)
  };

  if (source.show_on_app !== undefined || source.showOnApp !== undefined) {
    normalized.show_on_app = source.show_on_app ?? source.showOnApp;
  }

  delete normalized.promotionKind;
  delete normalized.displaySection;
  delete normalized.showOnApp;

  return normalized;
};

const matchesUserPromotionTarget = (target, user) => {
  if (!target) return true;
  const userGroups = getPromotionTargetGroups(target).filter(group => group.target_type === 'User');
  if (!userGroups.length) return true;
  if (!user) return false;

  return userGroups.every((group) => matchesUserTargetRefs(group.target_ref, user));
};

const getPromotionTargetGroups = (target) => {
  if (!target) return [];
  if (Array.isArray(target.target_groups) && target.target_groups.length) {
    return target.target_groups
      .map(group => ({
        target_type: group?.target_type,
        target_ref: Array.isArray(group?.target_ref) ? group.target_ref : []
      }))
      .filter(group => group.target_type);
  }
  if (target.target_type) {
    return [{
      target_type: target.target_type,
      target_ref: Array.isArray(target.target_ref) ? target.target_ref : []
    }];
  }
  return [];
};

const hasTargetGroupType = (target, targetType) => {
  return getPromotionTargetGroups(target).some(group => group.target_type === targetType);
};

const getTargetRefsByType = (target, targetTypes) => {
  const types = Array.isArray(targetTypes) ? targetTypes : [targetTypes];
  return getPromotionTargetGroups(target)
    .filter(group => types.includes(group.target_type))
    .flatMap(group => Array.isArray(group.target_ref) ? group.target_ref : []);
};

const matchesUserTargetRefs = (refs, user) => {
  if (!refs.length) return true;

  return refs.some((ref) => {
    const value = String(ref || '').trim();
    if (value.startsWith('tier:')) {
      const tier = value.slice('tier:'.length).toLowerCase();
      const tiering = String(user.CustomerTiering || user.CustomerType || '').trim().toLowerCase();
      return (
        (tier === 'bronze' && ['đồng', 'dong', 'bronze', 'regular'].includes(tiering)) ||
        (tier === 'silver' && ['bạc', 'bac', 'silver', 'premium'].includes(tiering)) ||
        (tier === 'gold' && ['vàng', 'vang', 'gold', 'vip'].includes(tiering))
      );
    }
    if (value.startsWith('certificate:')) {
      return String(user.CertificateID || '').trim() === value.slice('certificate:'.length);
    }
    return false;
  });
};

router.get('/', asyncHandler(async (req, res) => {
  const serverRoot = getServerRoot(req);
  const promos = await Promotion.find({
    isActive: { $ne: false },
    status: { $ne: 'Inactive' },
  }).sort({ promotion_id: 1 });

  const promotionIds = promos.map((promo) => promo.promotion_id).filter(Boolean);
  const targets = promotionIds.length
    ? await PromotionTarget.find({
      promotion_id: { $in: promotionIds },
      $or: [
        { target_type: 'User' },
        { 'target_groups.target_type': 'User' }
      ]
    }).lean()
    : [];
  const userTargets = new Map(targets.map((target) => [target.promotion_id, target]));
  const customerId = String(req.query.customerId || '').trim();
  const surface = String(req.query.surface || '').trim().toLowerCase();
  const code = String(req.query.code || '').trim().toLowerCase();
  const user = customerId
    ? await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId })
    : null;

  let filtered = promos.filter((promo) => {
    const target = userTargets.get(promo.promotion_id);
    if (surface === 'home' && target) return false;
    if (surface === 'carbon') {
      const refs = getTargetRefsByType(target, 'User');
      const hasCertificateTarget = refs.some((ref) => String(ref || '').startsWith('certificate:'));
      return hasCertificateTarget;
    }
    if (!customerId) return true;
    return !target || matchesUserPromotionTarget(target, user);
  });

  if (code) {
    filtered = filtered.filter((promo) => {
      const promoCode = String(promo.code || '').trim().toLowerCase();
      const promoId = String(promo.promotion_id || '').trim().toLowerCase();
      return promoCode === code || promoId === code;
    });
  }

  res.json(filtered.map((promo) => normalizePromotionBanner(normalizePromotionPayload(promo.toObject()), serverRoot)));
}));

router.post('/upload-banner-image', upload.single('file'), asyncHandler(async (req, res) => {
  const file = req.file;
  if (!file) return res.status(400).json({ success: false, message: 'Missing file' });

  try {
    const cloudinaryUpload = await uploadBannerToCloudinary(file);
    return res.status(201).json({
      success: true,
      ...cloudinaryUpload
    });
  } catch (error) {
    console.error('[Cloudinary] Upload failed:', error.message);
  }

  try {
    const firebaseUpload = await uploadBannerToFirebaseStorage(file);
    return res.status(201).json({
      success: true,
      ...firebaseUpload
    });
  } catch (error) {
    console.error('[POST /api/promotions/upload-banner-image] Firebase Storage upload failed:', error.message);
    const fallback = await uploadBannerToMongo(req, file, error.message);
    return res.status(201).json({
      success: true,
      ...fallback,
      firebaseError: error.message,
      warning: 'Firebase Storage upload failed; saved banner in MongoDB fallback storage'
    });
  }
}));

const parsePromotionDate = (value) => {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'object' && value.$date) return new Date(value.$date);
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const isPromotionCurrentlyActive = (promo, now = new Date()) => {
  const status = String(promo.status || '').trim().toLowerCase();
  if (status === 'inactive' || status === 'expired') return false;

  const startDate = parsePromotionDate(promo.start_date);
  const endDate = parsePromotionDate(promo.end_date);
  if (startDate && startDate > now) return false;
  if (endDate && endDate < now) return false;
  return promo.isActive !== false && promo.show_on_app !== false;
};

const isFlashSalePromotion = (promo) => {
  return String(promo.promotion_kind || '').trim().toLowerCase() === 'flashsale';
};

router.get('/flash-sales', asyncHandler(async (_req, res) => {
  const now = new Date();
  const allPromos = await mongoose.connection.db
    .collection('promotions')
    .find({
      show_on_app: { $ne: false },
      status: { $ne: 'Inactive' }
    })
    .toArray();
  const promos = allPromos.filter((promo) => isPromotionCurrentlyActive(promo, now) && isFlashSalePromotion(promo));

  if (!promos.length) {
    return res.json({ success: true, data: [] });
  }

  const promotionIds = promos.map(promo => promo.promotion_id).filter(Boolean);
  const targets = await mongoose.connection.db
    .collection('promotion_targets')
    .find({
      promotion_id: { $in: promotionIds },
      $or: [
        { target_type: 'Product' },
        { 'target_groups.target_type': 'Product' }
      ]
    })
    .toArray();

  const skus = [...new Set(targets.flatMap(target => getTargetRefsByType(target, 'Product')))];
  const products = await mongoose.connection.db
    .collection('products')
    .find({ sku: { $in: skus } })
    .toArray();
  const productMap = new Map(products.map(product => [product.sku, product]));
  const promoMap = new Map(promos.map(promo => [promo.promotion_id, promo]));

  const data = [];
  targets.forEach(target => {
    const promo = promoMap.get(target.promotion_id);
    if (!promo) return;

    getTargetRefsByType(target, 'Product').forEach(sku => {
      const product = productMap.get(sku);
      if (!product) return;

      const originalPrice = Number(product.price || 0);
      const discountValue = Number(promo.discount_value || promo.discount || 0);
      const salePrice = promo.discount_type === 'fixed'
        ? Math.max(0, originalPrice - discountValue)
        : promo.discount_type === 'buy1get1'
          ? originalPrice
          : Math.max(0, Math.round(originalPrice * (100 - discountValue) / 100));

      data.push({
        id: product._id?.toString() || product.sku,
        sku: product.sku,
        name: product.product_name || product.name || product.sku,
        price: salePrice,
        originalPrice,
        unit: product.unit || '',
        discount: promo.discount_type === 'fixed'
          ? `-${discountValue.toLocaleString('vi-VN')}đ`
          : promo.discount_type === 'buy1get1'
            ? 'Mua 1 tặng 1'
            : `-${discountValue}%`,
        imageUrl: Array.isArray(product.image) ? product.image[0] : product.image || '',
        rating: Number(product.rating || 0),
        promotionId: promo.promotion_id,
        endsAt: promo.end_date
      });
    });
  });

  res.json({ success: true, data });
}));

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

  let query = { status: 'Active' };
  const productRefs = getTargetRefsByType(target, 'Product');
  const brandRefs = getTargetRefsByType(target, 'Brand');
  const subcategoryRefs = getTargetRefsByType(target, 'Subcategory');
  const categoryRefs = getTargetRefsByType(target, 'Category');

  if (brandRefs.length) {
    query.brand = { $in: brandRefs };
  }
  if (subcategoryRefs.length) {
    query.SubcategoryID = { $in: subcategoryRefs };
  }
  if (categoryRefs.length) {
    query.CategoryID = { $in: categoryRefs };
  }
  if (productRefs.length) {
    query.sku = { $in: productRefs };
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

router.get('/:id/banner-image', asyncHandler(async (req, res) => {
  const promotion = await Promotion.findOne({
    $or: [{ promotion_id: req.params.id }, { code: req.params.id }],
    isActive: { $ne: false },
    status: { $ne: 'Inactive' }
  });

  if (!promotion) {
    return res.status(404).send('Promotion not found');
  }

  const imageUrl = promotion.banner_data?.src
    || promotion.banner_data?.imageUrl
    || promotion.imageUrl;
  if (!imageUrl) {
    return res.status(404).send('Promotion banner not found');
  }

  if (!/^https?:\/\//i.test(imageUrl)) {
    return res.redirect(buildPublicUrl(imageUrl, getServerRoot(req)));
  }

  const upstream = await fetch(imageUrl);
  if (!upstream.ok) {
    return res.status(upstream.status).send('Unable to load promotion banner');
  }

  const contentType = upstream.headers.get('content-type') || 'image/jpeg';
  const arrayBuffer = await upstream.arrayBuffer();
  res.set('Content-Type', contentType);
  res.set('Cache-Control', 'public, max-age=3600');
  res.send(Buffer.from(arrayBuffer));
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const promotion = await Promotion.findOne({
    $or: [{ promotion_id: req.params.id }, { code: req.params.id }],
    isActive: { $ne: false },
    status: { $ne: 'Inactive' }
  });

  if (!promotion) {
    return res.status(404).json({ message: 'Promotion not found' });
  }

  const serverRoot = getServerRoot(req);
  res.json(normalizePromotionBanner(normalizePromotionPayload(promotion.toObject()), serverRoot));
}));

router.post('/', asyncHandler(async (req, res) => {
  const newPromo = {
    ...normalizePromotionPayload(req.body),
    promotion_id: req.body.promotion_id || await getNextPromotionId(),
    code: req.body.code || `FS${Date.now()}`,
    status: req.body.status || 'Active',
    created_at: req.body.created_at || new Date(),
    updated_at: req.body.updated_at || new Date()
  };
  const result = await mongoose.connection.db.collection('promotions').insertOne(newPromo);
  res.status(201).json({ success: true, data: { _id: result.insertedId, ...newPromo } });
}));

router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ promotion_id: req.params.id }, { code: req.params.id }] };
  }

  const updateData = { ...req.body };
  delete updateData._id;
  const existingPromo = await mongoose.connection.db.collection('promotions').findOne(query);
  const normalizedUpdateData = normalizePromotionPayload(updateData, existingPromo || {});

  await mongoose.connection.db.collection('promotions').updateOne(query, {
    $set: normalizedUpdateData,
    $unset: {
      promotionKind: '',
      displaySection: '',
      showOnApp: ''
    }
  });
  const updatedPromo = await mongoose.connection.db.collection('promotions').findOne(query);
  res.json({ success: true, data: updatedPromo });
}));

router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ promotion_id: req.params.id }, { code: req.params.id }] };
  }

  const promotion = await mongoose.connection.db.collection('promotions').findOne(query);
  if (!promotion) {
    return res.status(404).json({ success: false, message: 'Promotion not found' });
  }

  const targetIds = [
    promotion.promotion_id,
    promotion.code,
    promotion._id?.toString(),
    req.params.id
  ].filter(Boolean);

  const deletePromotionResult = await mongoose.connection.db.collection('promotions').deleteOne({ _id: promotion._id });
  const deleteTargetsResult = targetIds.length
    ? await mongoose.connection.db.collection('promotion_targets').deleteMany({ promotion_id: { $in: targetIds } })
    : { deletedCount: 0 };

  res.json({
    success: true,
    deletedPromotionCount: deletePromotionResult.deletedCount || 0,
    deletedPromotionTargetCount: deleteTargetsResult.deletedCount || 0
  });
}));

module.exports = router;
