const express = require('express');
const mongoose = require('mongoose');
const crypto = require('crypto');
const multer = require('multer');
const { v2: cloudinary } = require('cloudinary');
const admin = require('../config/firebaseAdmin');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

const getPublicApiBaseUrl = (req) => {
  const configuredBaseUrl = process.env.PUBLIC_API_BASE_URL || process.env.API_PUBLIC_BASE_URL;
  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/$/, '');
  }

  return `${req.protocol}://${req.get('host')}`;
};

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
  const imageUrl = `${getPublicApiBaseUrl(req)}${imagePath}`;
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

// Get all promotions (Active)
router.get('/', asyncHandler(async (req, res) => {
  const promos = await mongoose.connection.db
    .collection('promotions')
    .find({ status: { $ne: 'Inactive' } })
    .toArray();
  res.json(promos.map(promo => normalizePromotionPayload(promo)));
}));

// POST /upload-banner-image - upload ảnh banner khuyến mãi lên Firebase Storage
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

// GET /api/promotions/flash-sales - danh sách sản phẩm flash sale đang hiển thị trên app
router.get('/flash-sales', asyncHandler(async (_req, res) => {
  const now = new Date();
  const promos = await mongoose.connection.db
    .collection('promotions')
    .find({
      promotion_kind: 'FlashSale',
      show_on_app: { $ne: false },
      status: { $ne: 'Inactive' },
      start_date: { $lte: now },
      end_date: { $gte: now }
    })
    .toArray();

  if (!promos.length) {
    return res.json({ success: true, data: [] });
  }

  const promotionIds = promos.map(promo => promo.promotion_id).filter(Boolean);
  const targets = await mongoose.connection.db
    .collection('promotion_targets')
    .find({ promotion_id: { $in: promotionIds }, target_type: 'Product' })
    .toArray();

  const skus = [...new Set(targets.flatMap(target => Array.isArray(target.target_ref) ? target.target_ref : []))];
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

    (target.target_ref || []).forEach(sku => {
      const product = productMap.get(sku);
      if (!product) return;

      const originalPrice = Number(product.price || 0);
      const discountValue = Number(promo.discount_value || 0);
      const salePrice = promo.discount_type === 'fixed'
        ? Math.max(0, originalPrice - discountValue)
        : Math.max(0, Math.round(originalPrice * (100 - discountValue) / 100));

      data.push({
        id: product._id?.toString() || product.sku,
        sku: product.sku,
        name: product.product_name || product.name || product.sku,
        price: salePrice,
        originalPrice,
        unit: product.unit || '',
        discount: promo.discount_type === 'fixed' ? `-${discountValue.toLocaleString('vi-VN')}đ` : `-${discountValue}%`,
        imageUrl: Array.isArray(product.image) ? product.image[0] : product.image || '',
        rating: Number(product.rating || 0),
        promotionId: promo.promotion_id,
        endsAt: promo.end_date
      });
    });
  });

  res.json({ success: true, data });
}));

// Create a promotion
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

// Update a promotion
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

// Delete a promotion (soft delete)
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { $or: [{ promotion_id: req.params.id }, { code: req.params.id }] };
  }

  await mongoose.connection.db.collection('promotions').updateOne(query, { $set: { status: 'Inactive' } });
  res.json({ success: true });
}));

module.exports = router;
