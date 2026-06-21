const express = require('express');
const PromoBannerImage = require('../models/PromoBannerImage');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

router.get('/:token', asyncHandler(async (req, res) => {
  const image = await PromoBannerImage.findOne({ token: req.params.token });
  if (!image || !image.data) {
    return res.status(404).json({ message: 'Promotion image not found' });
  }

  const buffer = toImageBuffer(image.data);
  res.set('Content-Type', image.mimeType || 'image/png');
  res.set('Content-Length', buffer.length);
  res.set('Cache-Control', 'public, max-age=86400');
  return res.send(buffer);
}));

function toImageBuffer(data) {
  if (Buffer.isBuffer(data)) {
    return data;
  }

  if (data && data.buffer) {
    return Buffer.from(data.buffer);
  }

  if (typeof data === 'string') {
    const base64 = data.includes(',') ? data.split(',').pop() : data;
    return Buffer.from(base64, 'base64');
  }

  return Buffer.from(data);
}

module.exports = router;
