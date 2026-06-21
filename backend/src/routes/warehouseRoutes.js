const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// Get all warehouses
router.get('/', asyncHandler(async (req, res) => {
  const warehouses = await mongoose.connection.db
    .collection('warehouses')
    .find({ isActive: { $ne: false } })
    .toArray();
  res.json(warehouses);
}));

// Create warehouse
router.post('/', asyncHandler(async (req, res) => {
  const newWarehouse = { ...req.body, isActive: true };
  const result = await mongoose.connection.db.collection('warehouses').insertOne(newWarehouse);
  res.status(201).json({ _id: result.insertedId, ...newWarehouse });
}));

module.exports = router;
