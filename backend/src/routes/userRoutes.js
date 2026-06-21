const express = require('express');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const User = require('../models/User');
const asyncHandler = require('../middleware/asyncHandler');
const { uploadAvatar } = require('../utils/avatarStorage');
const { validateProfileInput, formatProfileResponse } = require('../utils/profileValidation');

const router = express.Router();

const AVATAR_MAX_BYTES = 5 * 1024 * 1024;
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: AVATAR_MAX_BYTES },
  fileFilter: (req, file, cb) => {
    if (!file || file.mimetype.startsWith('image/')) {
      cb(null, true);
      return;
    }
    cb(new Error('Chỉ chấp nhận file ảnh'));
  },
});

// --- Traditional Auth (SSOT - MongoDB Atlas) ---

/**
 * 1. Đăng ký tài khoản
 * POST /api/users/register
 */
router.post('/register', asyncHandler(async (req, res) => {
  const { phone, password, fullName, email } = req.body;

  // Kiểm tra phone đã tồn tại chưa
  const existingUser = await User.findOne({ Phone: phone });
  if (existingUser) {
    return res.status(400).json({ message: 'Số điện thoại này đã được đăng ký' });
  }

  // Hash password
  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(password, salt);

  // Tạo CustomerID mới (dùng static method đã thêm ở model)
  const customerId = await User.generateNextCustomerId();

  // Lưu user mới vào MongoDB
  const newUser = new User({
    CustomerID: customerId,
    Phone: phone,
    Password: hashedPassword,
    FullName: fullName || 'Người dùng mới',
    Email: email || '',
    PasswordVersion: 3, // Phiên bản mật khẩu mới nhất
    tastePreferences: defaultTastePreferences()
  });

  await newUser.save();

  // Trả về thông tin user (loại bỏ Password)
  const userResponse = newUser.toObject();
  delete userResponse.Password;

  res.status(201).json(userResponse);
}));

/**
 * 2. Đăng nhập
 * POST /api/users/login
 */
router.post('/login', asyncHandler(async (req, res) => {
  const { phone, password } = req.body;

  const user = await User.findOne({ Phone: phone })
    .select('Password FullName Email CustomerID Phone CarbonPoint avatarUrl addresses Address CustomerType TotalSpent CertificateID PasswordVersion LastPasswordReset firebaseUid name email phone')
    .lean();
  if (!user || !user.Password) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  const isMatch = await bcrypt.compare(password, user.Password);
  if (!isMatch) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  delete user.Password;
  user._id = String(user._id);
  res.json(user);
}));

/**
 * 3. Quên mật khẩu
 * POST /api/users/forgot-password
 */
router.post('/forgot-password', asyncHandler(async (req, res) => {
  const { phone } = req.body;

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'Số điện thoại chưa được đăng ký' });
  }

  // Trả về OTP mock theo yêu cầu của app hiện tại
  res.json({
    message: 'Mã xác thực đã được gửi đến số điện thoại của bạn',
    otp: '123456'
  });
}));

/**
 * 4. Đặt lại mật khẩu
 * POST /api/users/reset-password
 */
router.post('/reset-password', asyncHandler(async (req, res) => {
  const { phone, otp, newPassword } = req.body;

  // Kiểm tra OTP mock
  if (otp !== '123456') {
    return res.status(400).json({ message: 'Mã xác thực không đúng' });
  }

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }

  // Hash mật khẩu mới
  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(newPassword, salt);

  // Cập nhật thông tin mật khẩu
  user.Password = hashedPassword;
  user.PasswordVersion = 3;
  user.LastPasswordReset = new Date();

  await user.save();

  res.json({ message: 'Đặt lại mật khẩu thành công' });
}));

/**
 * 5. Lấy thông tin user theo số điện thoại
 * GET /api/users/phone/:phone
 */
router.get('/phone/:phone', asyncHandler(async (req, res) => {
  const user = await User.findOne({ Phone: req.params.phone })
    .select('-Password -RegisterDate -updated_at -LastPasswordReset')
    .lean();
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }
  user._id = String(user._id);
  res.json(user);
}));

router.get('/:customerId/taste-preferences', asyncHandler(async (req, res) => {
  const user = await User.findOne({ CustomerID: req.params.customerId });
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
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
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }
  const tastePreferences = normalizeTastePreferences(req.body, user.tastePreferences);
  await User.collection.updateOne(
    { CustomerID: req.params.customerId },
    { $set: { tastePreferences } }
  );
  res.json(tastePreferences);
}));

// --- Firebase Auth (Giữ nguyên cho các chức năng khác) ---

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

/**
 * 6. Cập nhật thông tin cá nhân
 * PUT /api/users/profile
 */
router.put('/profile', upload.single('avatar'), asyncHandler(async (req, res) => {
  const currentPhone = String(req.header('X-Current-Phone') || '').trim();
  if (!currentPhone) {
    return res.status(400).json({ message: 'Thiếu thông tin phiên đăng nhập' });
  }

  const validation = validateProfileInput({
    name: req.body.name,
    phone: req.body.phone,
    email: req.body.email,
  });
  if (!validation.valid) {
    return res.status(validation.status).json({ message: validation.message });
  }

  const existingUser = await User.findOne({ Phone: currentPhone })
    .select('_id FullName Phone Email name phone email avatarUrl')
    .lean();
  if (!existingUser) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }

  const { name, phone, email } = validation.data;
  if (phone !== currentPhone) {
    const phoneTaken = await User.findOne({ Phone: phone, _id: { $ne: existingUser._id } })
      .select('_id')
      .lean();
    if (phoneTaken) {
      return res.status(400).json({ message: 'Số điện thoại này đã được sử dụng' });
    }
  }

  const updatePayload = {
    FullName: name,
    Phone: phone,
    Email: email,
    name,
    phone,
    email,
  };

  if (req.file) {
    const avatarUrl = await uploadAvatar(req, req.file, String(existingUser._id));
    if (avatarUrl) {
      updatePayload.avatarUrl = avatarUrl;
    }
  }

  await User.updateOne({ _id: existingUser._id }, { $set: updatePayload });

  res.json(formatProfileResponse({ ...existingUser, ...updatePayload }));
}));

router.use((error, req, res, next) => {
  if (error instanceof multer.MulterError && error.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({ message: 'Ảnh đại diện quá lớn (tối đa 5MB)' });
  }
  if (error && error.message === 'Chỉ chấp nhận file ảnh') {
    return res.status(400).json({ message: error.message });
  }
  return next(error);
});

function defaultTastePreferences() {
  return {
    dietMode: 'vegan',
    catalogBehavior: 'hide',
    suggestMenu: true,
    alertIngredients: [],
    replacementSuggestions: [],
    tags: [
      { label: 'ca cao', action: 'Không ăn', keywords: ['ca cao', 'cacao', 'socola', 'chocolate'], enabled: true },
      { label: 'trái cây nhiệt đới', action: 'Không ăn', keywords: ['xoài', 'dưa hấu', 'đu đủ', 'sầu riêng', 'thanh long', 'chuối', 'dứa', 'thơm'], enabled: true },
      { label: 'đào', action: 'Dị ứng', keywords: ['đào', 'peach'], enabled: true },
      { label: 'đậu phộng', action: 'Dị ứng', keywords: ['đậu phộng', 'lạc'], enabled: true },
      { label: 'cà chua', action: 'Cảnh báo', keywords: ['cà chua', 'cà chua bi'], enabled: true }
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
