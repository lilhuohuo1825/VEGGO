const Cart = require('../models/Cart');
const Product = require('../models/Product');
const mongoose = require('mongoose');

function normalizeWeightOptions(weightOptions) {
  if (!Array.isArray(weightOptions)) {
    return [];
  }

  return weightOptions
    .map((weight) => Number(weight))
    .filter((weight) => Number.isFinite(weight));
}

function getPrimaryImage(image) {
  if (Array.isArray(image) && image.length > 0) {
    return image;
  }

  return [];
}

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

  const getPromotionTargetGroups = (target) => {
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
  };

  const getTargetRefsByType = (target, targetType) => getPromotionTargetGroups(target)
    .filter(group => group.target_type === targetType)
    .flatMap(group => group.target_ref);

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
  return {
    ...product,
    price: calculateFlashSalePrice(listPrice, promo),
    originalPrice: listPrice,
    activePromotionId: promo.promotion_id,
    activePromotionKind: promo.promotion_kind,
  };
}

function buildProductSnapshot(product) {
  if (!product) {
    return null;
  }

  return {
    _id: String(product._id),
    id: String(product._id),
    sku: product.sku != null ? String(product.sku) : '',
    product_name: product.product_name,
    brand: product.brand || '',
    CategoryID: product.CategoryID || product.categoryId || '',
    SubcategoryID: product.SubcategoryID || product.subcategoryId || '',
    image: getPrimaryImage(product.image),
    price: product.price,
    originalPrice: product.originalPrice || product.base_price || product.price,
    base_price: product.base_price || product.originalPrice || product.price,
    unit: product.unit || '',
    weight: product.weight || product.unit || '',
    WeightOptions: normalizeWeightOptions(product.WeightOptions),
    CarbonSavingPoint: product.CarbonSavingPoint,
    EmissionFactor: product.EmissionFactor,
  };
}

async function populateCart(cart) {
  if (!cart) {
    return null;
  }

  const cartObject = cart.toObject();
  const skus = cartObject.items.map((item) => item.sku);
  const numericSkus = skus
    .map((sku) => Number(sku))
    .filter((sku) => Number.isFinite(sku));
  const products = await Product.collection
    .find({ $or: [{ sku: { $in: skus } }, { sku: { $in: numericSkus } }] })
    .toArray();
  const flashSaleBySku = await getActiveFlashSaleBySku(skus);
  const productBySku = new Map(products.map((product) => [
    product.sku,
    applyFlashSalePricing(product, flashSaleBySku.get(String(product.sku)))
  ]));

  cartObject.items = sortCartItemsByRecent(cartObject.items).map((item) => {
    const product = productBySku.get(item.sku) || null;
    const productSnapshot = buildProductSnapshot(product);

    return {
      ...item,
      ...(productSnapshot || {}),
      product: productSnapshot,
    };
  });

  return cartObject;
}

function resolveDefaultWeight(product) {
  const weightOptions = normalizeWeightOptions(product?.WeightOptions);
  if (weightOptions.length > 0) {
    return weightOptions[0];
  }

  return 1;
}

function parseQuantity(quantity) {
  const parsed = Number(quantity);
  if (!Number.isFinite(parsed)) {
    return 1;
  }

  return Math.max(1, Math.trunc(parsed));
}

function parseWeight(selectedWeight, fallbackWeight) {
  const parsed = Number(selectedWeight);
  if (Number.isFinite(parsed) && parsed > 0) {
    return parsed;
  }

  return fallbackWeight;
}

function weightsEqual(left, right) {
  return Math.abs(Number(left || 0) - Number(right || 0)) < 0.0001;
}

function findCartItemIndex(items, sku, selectedWeight) {
  return items.findIndex((item) => item.sku === sku && weightsEqual(item.selectedWeight, selectedWeight));
}

function findCartItem(items, sku, selectedWeight) {
  return items.find((item) => item.sku === sku && weightsEqual(item.selectedWeight, selectedWeight));
}

function touchCartItem(item) {
  if (item) {
    item.updatedAt = new Date();
  }
}

function sortCartItemsByRecent(items) {
  return [...(items || [])].sort((left, right) => {
    const leftTime = left?.updatedAt ? new Date(left.updatedAt).getTime() : 0;
    const rightTime = right?.updatedAt ? new Date(right.updatedAt).getTime() : 0;
    return rightTime - leftTime;
  });
}

exports.getCart = async (req, res) => {
  const { customerId } = req.params;
  const cart = await Cart.findOne({ customerId });
  const populatedCart = await populateCart(cart);

  res.json(populatedCart || { customerId, items: [] });
};

exports.addItem = async (req, res) => {
  const { customerId } = req.params;
  const { sku, productId, quantity, selectedWeight } = req.body;
  const cleanSku = String(sku || '').trim();
  const cleanProductId = String(productId || '').trim();

  if (!cleanSku && !cleanProductId) {
    return res.status(400).json({ message: 'sku or productId is required' });
  }

  const productQuery = [];
  if (cleanSku) {
    productQuery.push({ sku: cleanSku });
    const numericSku = Number(cleanSku);
    if (Number.isFinite(numericSku)) {
      productQuery.push({ sku: numericSku });
    }
  }
  if (mongoose.Types.ObjectId.isValid(cleanProductId)) {
    productQuery.push({ _id: new mongoose.Types.ObjectId(cleanProductId) });
  }
  if (!productQuery.length) {
    return res.status(400).json({ message: 'Valid sku or productId is required' });
  }

  let product = await Product.findOne({ $or: productQuery }).lean();
  if (!product && cleanSku) {
    const numericSku = Number(cleanSku);
    if (Number.isFinite(numericSku)) {
      product = await Product.collection.findOne({ sku: numericSku });
    }
  }
  if (!product) {
    return res.status(404).json({ message: 'Product not found' });
  }

  const resolvedSku = String(product.sku || cleanSku).trim();
  if (!resolvedSku) {
    return res.status(400).json({ message: 'Product sku is required' });
  }

  const resolvedQuantity = parseQuantity(quantity);
  const resolvedWeight = parseWeight(selectedWeight, resolveDefaultWeight(product));

  let cart = await Cart.findOne({ customerId });
  if (!cart) {
    cart = new Cart({ customerId, items: [] });
  }

  const itemIndex = findCartItemIndex(cart.items, resolvedSku, resolvedWeight);
  if (itemIndex > -1) {
    cart.items[itemIndex].quantity += resolvedQuantity;
    touchCartItem(cart.items[itemIndex]);
  } else {
    cart.items.push({
      sku: resolvedSku,
      quantity: resolvedQuantity,
      selectedWeight: resolvedWeight,
      updatedAt: new Date(),
    });
  }

  await cart.save();
  const populatedCart = await populateCart(cart);
  res.json(populatedCart);
};

exports.updateItemQuantity = async (req, res) => {
  const { customerId, sku } = req.params;
  const quantity = parseQuantity(req.body.quantity);
  const selectedWeight = parseWeight(req.body.selectedWeight || req.query.selectedWeight, 1);

  const cart = await Cart.findOne({ customerId });
  if (!cart) {
    return res.status(404).json({ message: 'Cart not found' });
  }

  const item = findCartItem(cart.items, sku, selectedWeight);
  if (!item) {
    return res.status(404).json({ message: 'Cart item not found' });
  }

  item.quantity = quantity;
  touchCartItem(item);
  await cart.save();

  const populatedCart = await populateCart(cart);
  res.json(populatedCart);
};

exports.removeItem = async (req, res) => {
  const { customerId, sku } = req.params;
  const selectedWeight = parseWeight(req.query.selectedWeight || req.body?.selectedWeight, 1);

  const cart = await Cart.findOneAndUpdate(
    { customerId },
    { $pull: { items: { sku, selectedWeight } } },
    { new: true }
  );

  const populatedCart = await populateCart(cart);
  res.json(populatedCart || { customerId, items: [] });
};

exports.clearCart = async (req, res) => {
  const { customerId } = req.params;

  await Cart.findOneAndUpdate(
    { customerId },
    { $set: { items: [] } },
    { new: true, upsert: true }
  );

  res.json({ customerId, items: [] });
};
