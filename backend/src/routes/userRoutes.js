const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// Traditional Auth (SSOT - MongoDB Atlas)
router.post('/register', asyncHandler(async (req, res) => {
  const { phone, password, fullName, email } = req.body;

  const existingUser = await User.findOne({ Phone: phone });
  if (existingUser) {
    return res.status(400).json({ message: 'So dien thoai nay da duoc dang ky' });
  }

  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(password, salt);
  const customerId = await User.generateNextCustomerId();

  const newUser = new User({
    CustomerID: customerId,
    Phone: phone,
    Password: hashedPassword,
    FullName: fullName || 'Nguoi dung moi',
    Email: email || '',
    PasswordVersion: 3,
    tastePreferences: defaultTastePreferences()
  });

  await newUser.save();

  const userResponse = newUser.toObject();
  delete userResponse.Password;

  res.status(201).json(userResponse);
}));

router.post('/login', asyncHandler(async (req, res) => {
  const { phone, password } = req.body;

  const user = await User.findOne({ Phone: phone });
  if (!user || !user.Password) {
    return res.status(401).json({ message: 'So dien thoai hoac mat khau khong dung' });
  }

  const isMatch = await bcrypt.compare(password, user.Password);
  if (!isMatch) {
    return res.status(401).json({ message: 'So dien thoai hoac mat khau khong dung' });
  }

  const userResponse = user.toObject();
  delete userResponse.Password;

  res.json(userResponse);
}));

router.post('/forgot-password', asyncHandler(async (req, res) => {
  const { phone } = req.body;

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'So dien thoai chua duoc dang ky' });
  }

  res.json({
    message: 'Ma xac thuc da duoc gui den so dien thoai cua ban',
    otp: '123456'
  });
}));

router.post('/reset-password', asyncHandler(async (req, res) => {
  const { phone, otp, newPassword } = req.body;

  if (otp !== '123456') {
    return res.status(400).json({ message: 'Ma xac thuc khong dung' });
  }

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'Khong tim thay nguoi dung' });
  }

  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(newPassword, salt);

  user.Password = hashedPassword;
  user.PasswordVersion = 3;
  user.LastPasswordReset = new Date();

  await user.save();

  res.json({ message: 'Dat lai mat khau thanh cong' });
}));

router.get('/phone/:phone', asyncHandler(async (req, res) => {
  const user = await User.findOne({ Phone: req.params.phone });
  if (!user) {
    return res.status(404).json({ message: 'Khong tim thay nguoi dung' });
  }
  const userResponse = user.toObject();
  delete userResponse.Password;
  res.json(userResponse);
}));

router.get('/:customerId/taste-preferences', asyncHandler(async (req, res) => {
  const user = await User.findOne({ CustomerID: req.params.customerId });
  if (!user) {
    return res.status(404).json({ message: 'Khong tim thay nguoi dung' });
  }
  if (!user.tastePreferences) {
    user.tastePreferences = defaultTastePreferences();
    await User.collection.updateOne(
      { CustomerID: req.params.customerId },
      { $set: { tastePreferences: user.tastePreferences } }
    );
  }
  res.json(user.tastePreferences);
}));

router.put('/:customerId/taste-preferences', asyncHandler(async (req, res) => {
  const user = await User.findOne({ CustomerID: req.params.customerId });
  if (!user) {
    return res.status(404).json({ message: 'Khong tim thay nguoi dung' });
  }
  const tastePreferences = normalizeTastePreferences(req.body, user.tastePreferences);
  await User.collection.updateOne(
    { CustomerID: req.params.customerId },
    { $set: { tastePreferences } }
  );
  res.json(tastePreferences);
}));

// Firebase Auth
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
    { $set: { firebaseUid, name, email, phone, avatarUrl }, $setOnInsert: { tastePreferences: defaultTastePreferences() } },
    { new: true, upsert: true }
  );
  res.json(user);
}));

function defaultTastePreferences() {
  return {
    dietMode: 'vegan',
    catalogBehavior: 'hide',
    suggestMenu: true,
    alertIngredients: [],
    replacementSuggestions: [],
    tags: [
      { label: 'ca cao', action: 'Kh\u00f4ng \u0103n', keywords: ['ca cao', 'cacao', 'socola', 'chocolate'], enabled: true },
      { label: 'tr\u00e1i c\u00e2y nhi\u1ec7t \u0111\u1edbi', action: 'Kh\u00f4ng \u0103n', keywords: ['xo\u00e0i', 'd\u01b0a h\u1ea5u', '\u0111u \u0111\u1ee7', 's\u1ea7u ri\u00eang', 'thanh long', 'chu\u1ed1i', 'd\u1ee9a', 'th\u01a1m'], enabled: true },
      { label: '\u0111\u00e0o', action: 'D\u1ecb \u1ee9ng', keywords: ['\u0111\u00e0o', 'peach'], enabled: true },
      { label: '\u0111\u1eadu ph\u1ed9ng', action: 'D\u1ecb \u1ee9ng', keywords: ['\u0111\u1eadu ph\u1ed9ng', 'l\u1ea1c'], enabled: true },
      { label: 'c\u00e0 chua', action: 'C\u1ea3nh b\u00e1o', keywords: ['c\u00e0 chua', 'c\u00e0 chua bi'], enabled: true }
    ]
  };
}

function normalizeTastePreferences(body = {}, existing = {}) {
  const fallback = defaultTastePreferences();
  const catalogBehavior = body.catalogBehavior === 'badge' || body.badgeCatalog === true
    ? 'badge'
    : 'hide';
  const tags = Array.isArray(body.tags)
    ? body.tags.map(normalizeTasteTag).filter(Boolean)
    : fallback.tags;

  return {
    dietMode: typeof body.dietMode === 'string' && body.dietMode.trim()
      ? body.dietMode.trim()
      : fallback.dietMode,
    catalogBehavior,
    suggestMenu: body.suggestMenu !== false,
    tags,
    alertIngredients: Array.isArray(body.alertIngredients)
      ? body.alertIngredients.map(normalizeTasteAlertItem).filter(Boolean)
      : (Array.isArray(existing.alertIngredients) ? existing.alertIngredients : []),
    replacementSuggestions: Array.isArray(body.replacementSuggestions)
      ? body.replacementSuggestions.map(item => String(item).trim()).filter(Boolean)
      : (Array.isArray(existing.replacementSuggestions) ? existing.replacementSuggestions : [])
  };
}

function normalizeTasteTag(tag) {
  if (!tag || typeof tag.label !== 'string' || typeof tag.action !== 'string') {
    return null;
  }
  const keywords = Array.isArray(tag.keywords)
    ? tag.keywords
    : String(tag.keywords || '').split(',');

  return {
    label: tag.label.trim(),
    action: tag.action.trim(),
    keywords: keywords.map(item => String(item).trim()).filter(Boolean),
    enabled: tag.enabled !== false
  };
}

function normalizeTasteAlertItem(item) {
  if (!item || typeof item.label !== 'string') {
    return null;
  }
  return {
    label: item.label.trim(),
    action: typeof item.action === 'string' ? item.action.trim() : '',
    status: typeof item.status === 'string' ? item.status.trim() : '',
    description: typeof item.description === 'string' ? item.description.trim() : ''
  };
}

module.exports = router;
