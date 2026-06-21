const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/', asyncHandler(async (_req, res) => {
  const usages = await mongoose.connection.db.collection('promotion_usages').find({}).toArray();
  res.json({ success: true, data: usages });
}));

module.exports = router;
