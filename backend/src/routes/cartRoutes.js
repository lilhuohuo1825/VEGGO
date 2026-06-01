const express = require('express');
const Cart = require('../models/Cart');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/:userId', asyncHandler(async (req, res) => {
  const cart = await Cart.findOne({ userId: req.params.userId }).populate('items.productId');
  res.json(cart || { userId: req.params.userId, items: [] });
}));

router.post('/:userId/items', asyncHandler(async (req, res) => {
  const { productId, quantity } = req.body;
  const cart = await Cart.findOneAndUpdate(
    { userId: req.params.userId },
    { $setOnInsert: { userId: req.params.userId } },
    { new: true, upsert: true }
  );

  const item = cart.items.find((entry) => entry.productId.toString() === productId);
  if (item) {
    item.quantity += quantity || 1;
  } else {
    cart.items.push({ productId, quantity: quantity || 1 });
  }

  await cart.save();
  const updatedCart = await Cart.findOne({ userId: req.params.userId }).populate('items.productId');
  res.json(updatedCart);
}));

router.delete('/:userId/items/:productId', asyncHandler(async (req, res) => {
  const cart = await Cart.findOneAndUpdate(
    { userId: req.params.userId },
    { $pull: { items: { productId: req.params.productId } } },
    { new: true }
  ).populate('items.productId');

  res.json(cart || { userId: req.params.userId, items: [] });
}));

router.delete('/:userId', asyncHandler(async (req, res) => {
  await Cart.findOneAndUpdate(
    { userId: req.params.userId },
    { $set: { items: [] } },
    { new: true, upsert: true }
  );

  res.json({ userId: req.params.userId, items: [] });
}));

module.exports = router;
