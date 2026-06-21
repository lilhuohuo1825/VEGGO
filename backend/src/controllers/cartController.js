const Cart = require('../models/Cart');
const Product = require('../models/Product');

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

function buildProductSnapshot(product) {
  if (!product) {
    return null;
  }

  return {
    product_name: product.product_name,
    image: getPrimaryImage(product.image),
    price: product.price,
    WeightOptions: normalizeWeightOptions(product.WeightOptions),
    CarbonSavingPoint: product.CarbonSavingPoint,
  };
}

async function populateCart(cart) {
  if (!cart) {
    return null;
  }

  const cartObject = cart.toObject();
  const skus = cartObject.items.map((item) => item.sku);
  const products = await Product.find({ sku: { $in: skus } }).lean();
  const productBySku = new Map(products.map((product) => [product.sku, product]));

  cartObject.items = cartObject.items.map((item) => {
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

exports.getCart = async (req, res) => {
  const { customerId } = req.params;
  const cart = await Cart.findOne({ customerId });
  const populatedCart = await populateCart(cart);

  res.json(populatedCart || { customerId, items: [] });
};

exports.addItem = async (req, res) => {
  const { customerId } = req.params;
  const { sku, quantity, selectedWeight } = req.body;

  if (!sku) {
    return res.status(400).json({ message: 'sku is required' });
  }

  const product = await Product.findOne({ sku }).lean();
  if (!product) {
    return res.status(404).json({ message: 'Product not found' });
  }

  const resolvedQuantity = parseQuantity(quantity);
  const resolvedWeight = parseWeight(selectedWeight, resolveDefaultWeight(product));

  let cart = await Cart.findOne({ customerId });
  if (!cart) {
    cart = new Cart({ customerId, items: [] });
  }

  const itemIndex = cart.items.findIndex((item) => item.sku === sku);
  if (itemIndex > -1) {
    cart.items[itemIndex].quantity += resolvedQuantity;
    cart.items[itemIndex].selectedWeight = resolvedWeight;
  } else {
    cart.items.push({
      sku,
      quantity: resolvedQuantity,
      selectedWeight: resolvedWeight,
    });
  }

  await cart.save();
  const populatedCart = await populateCart(cart);
  res.json(populatedCart);
};

exports.updateItemQuantity = async (req, res) => {
  const { customerId, sku } = req.params;
  const quantity = parseQuantity(req.body.quantity);

  const cart = await Cart.findOne({ customerId });
  if (!cart) {
    return res.status(404).json({ message: 'Cart not found' });
  }

  const item = cart.items.find((entry) => entry.sku === sku);
  if (!item) {
    return res.status(404).json({ message: 'Cart item not found' });
  }

  item.quantity = quantity;
  await cart.save();

  const populatedCart = await populateCart(cart);
  res.json(populatedCart);
};

exports.removeItem = async (req, res) => {
  const { customerId, sku } = req.params;

  const cart = await Cart.findOneAndUpdate(
    { customerId },
    { $pull: { items: { sku } } },
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