const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const collection = () => mongoose.connection.db.collection('admin_notifications');

router.get('/', asyncHandler(async (_req, res) => {
  const notifications = await collection()
    .find({})
    .sort({ createdAt: -1 })
    .limit(50)
    .toArray();

  res.json({ success: true, data: notifications });
}));

router.get('/unread-count', asyncHandler(async (_req, res) => {
  const count = await collection().countDocuments({ read: { $ne: true } });
  res.json({ success: true, count });
}));

router.put('/:notificationId/read', asyncHandler(async (req, res) => {
  const { notificationId } = req.params;
  if (!mongoose.Types.ObjectId.isValid(notificationId)) {
    return res.status(400).json({ success: false, message: 'Valid notificationId is required' });
  }

  const result = await collection().findOneAndUpdate(
    { _id: new mongoose.Types.ObjectId(notificationId) },
    {
      $set: {
        read: true,
        updatedAt: new Date(),
        readAt: new Date(),
      },
    },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ success: false, message: 'Notification not found' });
  }

  res.json({ success: true, data: result });
}));

router.put('/:notificationId/status', asyncHandler(async (req, res) => {
  const { notificationId } = req.params;
  if (!mongoose.Types.ObjectId.isValid(notificationId)) {
    return res.status(400).json({ success: false, message: 'Valid notificationId is required' });
  }

  const action = String(req.body.action || '').trim().toLowerCase();
  const status = action === 'approve' ? 'approved' : action === 'reject' ? 'rejected' : '';
  if (!status) {
    return res.status(400).json({ success: false, message: 'Valid action is required' });
  }

  const result = await collection().findOneAndUpdate(
    { _id: new mongoose.Types.ObjectId(notificationId) },
    {
      $set: {
        status,
        read: true,
        updatedAt: new Date(),
      },
    },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ success: false, message: 'Notification not found' });
  }

  res.json({ success: true, data: result });
}));

module.exports = router;
