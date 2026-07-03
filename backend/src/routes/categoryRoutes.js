const express = require('express');
const mongoose = require('mongoose');

const router = express.Router();

const asyncHandler = (handler) => (req, res, next) => {
  Promise.resolve(handler(req, res, next)).catch(next);
};

router.get('/', asyncHandler(async (_req, res) => {
  const categories = await mongoose.connection.db
    .collection('categories')
    .find()
    .sort({ CategoryName: 1 })
    .toArray();

  res.json(categories);
}));

module.exports = router;
