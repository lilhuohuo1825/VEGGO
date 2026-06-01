const express = require('express');
const Order = require('../models/Order');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/:userId', asyncHandler(async (req, res) => {
  const orders = await Order.find({ userId: req.params.userId }).sort({ createdAt: -1 });
  res.json(orders);
}));

router.post('/', asyncHandler(async (req, res) => {
  const order = await Order.create(req.body);
  res.status(201).json(order);
}));

module.exports = router;
