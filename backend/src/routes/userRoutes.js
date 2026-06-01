const express = require('express');
const User = require('../models/User');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/firebase/:firebaseUid', asyncHandler(async (req, res) => {
  const user = await User.findOne({ firebaseUid: req.params.firebaseUid });
  if (!user) {
    return res.status(404).json({ message: 'User not found' });
  }
  res.json(user);
}));

router.post('/sync', asyncHandler(async (req, res) => {
  const { firebaseUid, name, email, phone, avatarUrl } = req.body;
  const user = await User.findOneAndUpdate(
    { firebaseUid },
    { firebaseUid, name, email, phone, avatarUrl },
    { new: true, upsert: true }
  );
  res.json(user);
}));

module.exports = router;
