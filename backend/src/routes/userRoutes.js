const express = require('express');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const mongoose = require('mongoose');
const nodemailer = require('nodemailer');
const admin = require('firebase-admin');
const User = require('../models/User');
const Otp = require('../models/Otp');
const asyncHandler = require('../middleware/asyncHandler');
const { uploadAvatar } = require('../utils/avatarStorage');
const { validateProfileInput, formatProfileResponse } = require('../utils/profileValidation');
const { signAccessToken } = require('../utils/jwt');

const router = express.Router();

const AVATAR_MAX_BYTES = 5 * 1024 * 1024;
const PHONE_REGEX = /^0\d{9}$/;
const STRONG_PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/;
const OTP_TTL_MS = 60 * 1000;
const RESET_PASSWORD_WINDOW_MS = 10 * 60 * 1000;
const MAX_OTP_ATTEMPTS = 3;
const buildOtp = () => Math.floor(100000 + Math.random() * 900000).toString();

async function validateForgotPasswordOtp(phone, otp) {
  const otpRecord = await Otp.findOne({ phone, purpose: 'forgot_password' });
  if (!otpRecord || otpRecord.expiresAt < new Date()) {
    if (otpRecord) await Otp.deleteOne({ _id: otpRecord._id });
    return { ok: false, status: 400, message: 'Mã xác thực đã hết hạn' };
  }
  if (otpRecord.attempts >= MAX_OTP_ATTEMPTS) {
    await Otp.deleteOne({ _id: otpRecord._id });
    return { ok: false, status: 400, message: 'Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã' };
  }
  if (otpRecord.otp !== String(otp || '').trim()) {
    otpRecord.attempts += 1;
    await otpRecord.save();
    return { ok: false, status: 400, message: 'Mã xác thực không đúng' };
  }
  return { ok: true, otpRecord };
}
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

  if (!PHONE_REGEX.test(String(phone || '').trim())) {
    return res.status(400).json({ message: 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số' });
  }
  if (!STRONG_PASSWORD_REGEX.test(String(password || ''))) {
    return res.status(400).json({ message: 'Mật khẩu phải có ít nhất 8 ký tự, 1 chữ in hoa và 1 ký tự đặc biệt' });
  }

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

  if (!PHONE_REGEX.test(String(phone || '').trim()) || !STRONG_PASSWORD_REGEX.test(String(password || ''))) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  const user = await User.findOne({ Phone: phone })
    .select('Password FullName Email CustomerID Phone CarbonPoint avatarUrl addresses Address CustomerType CustomerTiering TotalSpent CertificateID CertificateName CertificateStatus CertificateGrantedAt CertificateCarbonPointSnapshot CertificateCarbonEmissionSnapshot PasswordVersion LastPasswordReset firebaseUid name email phone')
    .lean();
  if (!user) {
    return res.status(401).json({ message: 'Số điện thoại chưa được đăng ký' });
  }
  if (!user.Password) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  const isMatch = await bcrypt.compare(password, user.Password);
  if (!isMatch) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  delete user.Password;
  user._id = String(user._id);
  const accessToken = signAccessToken({
    type: 'user',
    customerId: user.CustomerID,
    userId: user._id,
  });
  res.json({ ...user, accessToken });
}));

/**
 * 2.1 Đăng nhập bằng Firebase (Google/Facebook)
 * POST /api/users/firebase-login
 */
router.post('/firebase-login', asyncHandler(async (req, res) => {
  const { idToken, avatarUrl } = req.body;

  if (!idToken) {
    return res.status(400).json({ message: 'Thiếu ID Token' });
  }

  try {
    // Verify Firebase ID Token
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const { uid, firebase } = decodedToken;

    // Fetch full user record from Firebase to get details reliably
    const userRecord = await admin.auth().getUser(uid);

    const email = decodedToken.email || userRecord.email;
    const name = decodedToken.name || userRecord.displayName;

    // Prioritize avatarUrl from request body (fetched via Graph API on Android)
    // then fallback to picture from token or userRecord
    const picture = avatarUrl || decodedToken.picture || userRecord.photoURL;

    // Yêu cầu bắt buộc phải có Email (theo yêu cầu SSOT)
    if (!email) {
      return res.status(400).json({
        message: 'Tài khoản này không cung cấp địa chỉ email. Vui lòng đăng nhập bằng số điện thoại hoặc sử dụng tài khoản khác có email.'
      });
    }

    const provider = firebase.sign_in_provider === 'facebook.com' ? 'facebook' : 'google';

    // Tìm user trong MongoDB theo firebaseUid
    let userDoc = await User.findOne({ firebaseUid: uid });

    if (!userDoc) {
      // Nếu chưa có tài khoản liên kết với UID này, tạo user mới hoàn toàn
      const customerId = await User.generateNextCustomerId();
      userDoc = new User({
        CustomerID: customerId,
        FullName: name || 'User',
        Email: email,
        EmailConfirmed: true,
        avatarUrl: picture || '',
        firebaseUid: uid,
        name: name,
        email: email,
        Provider: provider,
        tastePreferences: defaultTastePreferences()
      });
      await userDoc.save();
    } else {
      // Nếu đã có user, cập nhật avatar và tên nếu có sự thay đổi
      const updateData = {};
      let hasUpdate = false;

      if (picture && userDoc.avatarUrl !== picture) {
        updateData.avatarUrl = picture;
        hasUpdate = true;
      }
      if (name && (userDoc.FullName !== name || userDoc.name !== name)) {
        updateData.FullName = name;
        updateData.name = name;
        hasUpdate = true;
      }

      if (hasUpdate) {
        await User.updateOne({ _id: userDoc._id }, { $set: updateData });
        // Merge updates into the document for the response
        Object.assign(userDoc, updateData);
      }
    }

    const userResponse = userDoc.toObject();
    delete userResponse.Password;
    userResponse._id = String(userResponse._id);
    const accessToken = signAccessToken({
      type: 'user',
      customerId: userResponse.CustomerID,
      userId: userResponse._id,
    });

    res.json({ ...userResponse, accessToken });
  } catch (error) {
    console.error('Firebase Login Error:', error);
    res.status(401).json({ message: 'Xác thực tài khoản thất bại' });
  }
}));

/**
 * 2.2 Làm mới access token cho phiên đăng nhập hiện tại (không cần mật khẩu)
 * POST /api/users/refresh-access-token
 */
router.post('/refresh-access-token', asyncHandler(async (req, res) => {
  const customerId = String(req.body.customerId || '').trim();
  const phone = String(req.body.phone || '').trim();

  if (!customerId) {
    return res.status(400).json({ message: 'Thiếu customerId' });
  }

  const user = await User.findOne({ CustomerID: customerId })
    .select('CustomerID Phone')
    .lean();
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }

  if (phone && user.Phone && user.Phone !== phone) {
    return res.status(403).json({ message: 'Thông tin đăng nhập không khớp' });
  }

  const accessToken = signAccessToken({
    type: 'user',
    customerId: user.CustomerID,
    userId: String(user._id),
  });
  res.json({ accessToken });
}));

/**
 * 3. Quên mật khẩu
 * POST /api/users/forgot-password
 */
router.post('/forgot-password', asyncHandler(async (req, res) => {
  const { phone } = req.body;

  if (!PHONE_REGEX.test(String(phone || '').trim())) {
    return res.status(400).json({ message: 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số' });
  }

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'Số điện thoại chưa được đăng ký' });
  }

  const otp = buildOtp();
  await Otp.findOneAndUpdate(
    { phone, purpose: 'forgot_password' },
    {
      phone,
      purpose: 'forgot_password',
      otp,
      attempts: 0,
      createdAt: new Date(),
      expiresAt: new Date(Date.now() + OTP_TTL_MS)
    },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  res.json({
    message: 'Mã xác thực đã được gửi đến số điện thoại của bạn',
    otp
  });
}));

/**
 * 3b. Xác thực OTP quên mật khẩu (gia hạn thời gian để đặt lại mật khẩu)
 * POST /api/users/verify-forgot-password-otp
 */
router.post('/verify-forgot-password-otp', asyncHandler(async (req, res) => {
  const { phone, otp } = req.body;

  if (!PHONE_REGEX.test(String(phone || '').trim())) {
    return res.status(400).json({ message: 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số' });
  }

  const result = await validateForgotPasswordOtp(phone, otp);
  if (!result.ok) {
    return res.status(result.status).json({ message: result.message });
  }

  result.otpRecord.expiresAt = new Date(Date.now() + RESET_PASSWORD_WINDOW_MS);
  result.otpRecord.attempts = 0;
  await result.otpRecord.save();

  res.json({ message: 'Xác thực thành công' });
}));

/**
 * 4. Đặt lại mật khẩu
 * POST /api/users/reset-password
 */
router.post('/reset-password', asyncHandler(async (req, res) => {
  const { phone, otp, newPassword } = req.body;

  if (!STRONG_PASSWORD_REGEX.test(String(newPassword || ''))) {
    return res.status(400).json({ message: 'Mật khẩu phải có ít nhất 8 ký tự, 1 chữ in hoa và 1 ký tự đặc biệt' });
  }

  const result = await validateForgotPasswordOtp(phone, otp);
  if (!result.ok) {
    return res.status(result.status).json({ message: result.message });
  }

  const user = await User.findOne({ Phone: phone });
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }

  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(newPassword, salt);

  await User.updateOne(
    { _id: user._id },
    {
      $set: {
        Password: hashedPassword,
        PasswordVersion: 3,
        LastPasswordReset: new Date(),
      },
    }
  );
  await Otp.deleteOne({ _id: result.otpRecord._id });

  res.json({ message: 'Đặt lại mật khẩu thành công' });
}));

/**
 * Guest checkout OTP - send
 * POST /api/users/guest-order-otp
 */
router.post('/guest-order-otp', asyncHandler(async (req, res) => {
  const phone = String(req.body.phone || '').trim();

  if (!PHONE_REGEX.test(phone)) {
    return res.status(400).json({ message: 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số' });
  }

  const otp = buildOtp();
  await Otp.findOneAndUpdate(
    { phone, purpose: 'guest_order' },
    {
      phone,
      purpose: 'guest_order',
      otp,
      attempts: 0,
      createdAt: new Date(),
      expiresAt: new Date(Date.now() + OTP_TTL_MS),
    },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  res.json({
    message: 'Mã OTP đã được gửi đến số điện thoại của bạn',
    otp,
  });
}));

/**
 * Guest checkout OTP - verify
 * POST /api/users/guest-order-otp/verify
 */
router.post('/guest-order-otp/verify', asyncHandler(async (req, res) => {
  const phone = String(req.body.phone || '').trim();
  const otp = String(req.body.otp || '').trim();

  if (!PHONE_REGEX.test(phone)) {
    return res.status(400).json({ message: 'Số điện thoại không hợp lệ' });
  }
  if (!/^\d{6}$/.test(otp)) {
    return res.status(400).json({ message: 'Mã OTP không chính xác' });
  }

  const otpRecord = await Otp.findOne({ phone, purpose: 'guest_order' });
  if (!otpRecord || otpRecord.expiresAt < new Date()) {
    if (otpRecord) await Otp.deleteOne({ _id: otpRecord._id });
    return res.status(400).json({ message: 'Mã xác thực đã hết hạn' });
  }
  if (otpRecord.attempts >= MAX_OTP_ATTEMPTS) {
    await Otp.deleteOne({ _id: otpRecord._id });
    return res.status(400).json({ message: 'Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã' });
  }
  if (otpRecord.otp !== otp) {
    otpRecord.attempts += 1;
    await otpRecord.save();
    return res.status(400).json({ message: 'Mã OTP không chính xác' });
  }

  await Otp.deleteOne({ _id: otpRecord._id });
  res.json({ success: true, message: 'Xác thực OTP thành công' });
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
    birthday: req.body.birthday,
    gender: req.body.gender,
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

  const { name, phone, email, birthday, gender } = validation.data;
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
    BirthDay: birthday,
    Gender: gender,
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

// --- Admin Customer Management Endpoints ---

// Get all users (excluding deactivated ones)
let usersListCache = null;
let usersListCacheTime = 0;
const USERS_LIST_CACHE_MS = 15000;

router.get('/', asyncHandler(async (req, res) => {
  const now = Date.now();
  if (usersListCache && (now - usersListCacheTime < USERS_LIST_CACHE_MS)) {
    return res.json(usersListCache);
  }

  const users = await mongoose.connection.db
    .collection('users')
    .find(
      { isActive: { $ne: false } },
      {
        projection: {
          Password: 0,
          password: 0,
          tastePreferences: 0,
          TastePreferences: 0,
          FcmTokens: 0,
          fcmTokens: 0,
          deviceTokens: 0,
        },
      }
    )
    .sort({ RegisterDate: -1 })
    .toArray();

  usersListCache = users;
  usersListCacheTime = now;
  res.json(users);
}));

// Get customer detail by CustomerID (used by admin customer detail page)
router.get('/customer/:customerId', asyncHandler(async (req, res) => {
  const user = await mongoose.connection.db
    .collection('users')
    .findOne({ CustomerID: req.params.customerId, isActive: { $ne: false } });
  if (!user) {
    return res.status(404).json({ success: false, message: 'Customer not found' });
  }
  res.json({ success: true, customer: user });
}));

// Get user by specific ID (supports ObjectId or CustomerID fallback)
router.get('/id/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { CustomerID: req.params.id };
  }

  const user = await mongoose.connection.db.collection('users').findOne(query);
  if (!user) {
    return res.status(404).json({ message: 'User not found' });
  }
  res.json(user);
}));

// Update user info
router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { CustomerID: req.params.id };
  }

  const updateData = { ...req.body };
  delete updateData._id;

  await mongoose.connection.db.collection('users').updateOne(query, { $set: updateData });
  const updatedUser = await mongoose.connection.db.collection('users').findOne(query);
  res.json(updatedUser);
}));

// Soft delete user
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { CustomerID: req.params.id };
  }

  await mongoose.connection.db.collection('users').updateOne(query, { $set: { isActive: false } });
  res.json({ success: true });
}));

// --- Admin Auth OTP Password Reset ---

const BCRYPT_HASH_PATTERN = /^\$2[aby]\$\d{2}\$/;
const isBcryptHash = (value) => typeof value === 'string' && BCRYPT_HASH_PATTERN.test(value);

/**
 * Admin Đăng nhập qua MongoDB
 * POST /api/users/admin/login
 */
router.post('/admin/login', asyncHandler(async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email và mật khẩu là bắt buộc' });
  }

  const adminUser = await mongoose.connection.db.collection('admins').findOne({ email });
  if (!adminUser) {
    return res.status(401).json({ error: 'Tài khoản không tồn tại' });
  }

  if (!adminUser.password) {
    return res.status(401).json({ error: 'Mật khẩu không đúng' });
  }

  let isPasswordValid = false;
  if (isBcryptHash(adminUser.password)) {
    isPasswordValid = await bcrypt.compare(password, adminUser.password);
  } else {
    // Migrate legacy plaintext admin passwords on successful login.
    isPasswordValid = adminUser.password === password;
    if (isPasswordValid) {
      const hashedPassword = await bcrypt.hash(password, 10);
      await mongoose.connection.db.collection('admins').updateOne(
        { _id: adminUser._id },
        { $set: { password: hashedPassword, passwordUpdatedAt: new Date() } }
      );
    }
  }

  if (!isPasswordValid) {
    return res.status(401).json({ error: 'Mật khẩu không đúng' });
  }

  const accessToken = signAccessToken({
    type: 'admin',
    adminId: adminUser._id.toString(),
    email: adminUser.email,
  });

  res.json({
    success: true,
    user: {
      id: adminUser._id.toString(),
      email: adminUser.email,
      name: adminUser.name || 'Admin User',
      role: 'admin'
    },
    accessToken,
  });
}));

const getEmailConfig = () => {
  const user = process.env.SMTP_USER || process.env.EMAIL_USER;
  const pass = process.env.SMTP_PASS || process.env.EMAIL_PASS || process.env.EMAIL_PASSWORD;
  const host = process.env.SMTP_HOST || process.env.EMAIL_HOST || 'smtp.gmail.com';
  const port = parseInt(process.env.SMTP_PORT || process.env.EMAIL_PORT || '587', 10);
  const secure = String(process.env.SMTP_SECURE || process.env.EMAIL_SECURE || '').toLowerCase() === 'true' || port === 465;

  return {
    host,
    port,
    secure,
    user,
    pass,
    from: process.env.SMTP_FROM || process.env.EMAIL_FROM || (user ? `"VEGGO Admin Support" <${user}>` : '')
  };
};

const getTransporter = () => {
  const emailConfig = getEmailConfig();
  return nodemailer.createTransport({
    host: emailConfig.host,
    port: emailConfig.port,
    secure: emailConfig.secure,
    auth: {
      user: emailConfig.user,
      pass: emailConfig.pass,
    },
  });
};

/**
 * 6. Admin Quên mật khẩu - Phát sinh và gửi OTP
 * POST /api/users/admin/forgot-password
 */
router.post('/admin/forgot-password', asyncHandler(async (req, res) => {
  const { email } = req.body;
  if (!email) {
    return res.status(400).json({ error: 'Email là bắt buộc' });
  }

  // 1. Kiểm tra tài khoản có tồn tại trong MongoDB collection 'admins'
  const adminDoc = await mongoose.connection.db.collection('admins').findOne({ email });
  if (!adminDoc) {
    return res.status(404).json({ error: 'Email không tồn tại trong hệ thống Admin' });
  }

  // 2. Tạo mã OTP ngẫu nhiên 6 chữ số
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 phút hiệu lực

  // Lưu hoặc cập nhật OTP trong MongoDB
  await Otp.findOneAndUpdate(
    { email },
    { email, otp, expiresAt },
    { upsert: true, new: true }
  );

  const emailConfig = getEmailConfig();
  if (!emailConfig.user || !emailConfig.pass) {
    console.error('Thiếu cấu hình email OTP. Vui lòng cấu hình SMTP_USER/SMTP_PASS hoặc EMAIL_USER/EMAIL_PASS trong .env');
    return res.status(500).json({
      error: 'Chưa cấu hình email OTP. Vui lòng kiểm tra cấu hình máy chủ.'
    });
  }

  const transporter = getTransporter();
  const mailOptions = {
    from: emailConfig.from,
    to: email,
    subject: '[VEGGO] Mã xác thực OTP khôi phục mật khẩu Admin',
    html: `
      <div style="font-family: 'Lexend', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
        <div style="text-align: center; margin-bottom: 20px;">
          <h2 style="color: #57AF37; margin: 0;">VEGGO</h2>
          <p style="color: #666; font-size: 14px; margin: 5px 0 0 0;">Fresh groceries, on the go</p>
        </div>
        <hr style="border: 0; border-top: 1px solid #eee; margin-bottom: 20px;" />
        <h3 style="color: #333333; margin-top: 0;">Yêu cầu khôi phục mật khẩu Admin</h3>
        <p style="color: #555555; line-height: 1.6;">Chào bạn,</p>
        <p style="color: #555555; line-height: 1.6;">Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản quản trị của bạn. Vui lòng sử dụng mã OTP dưới đây để hoàn tất quá trình:</p>
        <div style="text-align: center; margin: 30px 0;">
          <span style="font-size: 32px; font-weight: 700; color: #57AF37; letter-spacing: 5px; padding: 10px 20px; background-color: #EAF7E8; border-radius: 8px; border: 1px dashed #7CCF5B;">${otp}</span>
        </div>
        <p style="color: #ff0000; font-size: 13px; font-weight: 500;">Lưu ý: Mã OTP này có hiệu lực trong vòng 10 phút. Không chia sẻ mã này cho bất kỳ ai.</p>
        <hr style="border: 0; border-top: 1px solid #eee; margin-top: 30px; margin-bottom: 20px;" />
        <p style="color: #999999; font-size: 12px; text-align: center; margin: 0;">Đây là email tự động từ hệ thống VEGGO. Vui lòng không phản hồi email này.</p>
      </div>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    res.json({ success: true, message: 'Mã OTP đã được gửi đến email của bạn.' });
  } catch (mailError) {
    console.error('Lỗi gửi email:', mailError);
    return res.status(500).json({ error: 'Không thể gửi email OTP. Vui lòng thử lại sau.' });
  }
}));

/**
 * 7. Admin Xác thực OTP
 * POST /api/users/admin/verify-otp
 */
router.post('/admin/verify-otp', asyncHandler(async (req, res) => {
  const { email, otp } = req.body;
  if (!email || !otp) {
    return res.status(400).json({ error: 'Email và mã OTP là bắt buộc' });
  }

  const otpRecord = await Otp.findOne({ email, otp });
  if (!otpRecord) {
    return res.status(400).json({ error: 'Mã OTP không đúng' });
  }

  if (otpRecord.expiresAt < new Date()) {
    await Otp.deleteOne({ _id: otpRecord._id });
    return res.status(400).json({ error: 'Mã OTP đã hết hạn' });
  }

  res.json({ success: true, message: 'Xác thực OTP thành công.' });
}));

/**
 * 8. Admin Đặt lại mật khẩu
 * POST /api/users/admin/reset-password
 */
router.post('/admin/reset-password', asyncHandler(async (req, res) => {
  const { email, otp, newPassword } = req.body;
  if (!email || !otp || !newPassword) {
    return res.status(400).json({ error: 'Thiếu thông tin yêu cầu' });
  }

  if (String(newPassword).length < 6) {
    return res.status(400).json({ error: 'Mật khẩu mới phải có ít nhất 6 ký tự' });
  }

  // 1. Xác thực OTP lần nữa để bảo mật
  const otpRecord = await Otp.findOne({ email, otp });
  if (!otpRecord) {
    return res.status(400).json({ error: 'Xác thực không hợp lệ hoặc mã OTP sai' });
  }

  if (otpRecord.expiresAt < new Date()) {
    await Otp.deleteOne({ _id: otpRecord._id });
    return res.status(400).json({ error: 'Mã OTP đã hết hạn' });
  }

  // 2. Tiến hành cập nhật mật khẩu trên MongoDB admins collection
  try {
    const hashedPassword = await bcrypt.hash(newPassword, 10);
    await mongoose.connection.db.collection('admins').updateOne(
      { email },
      { $set: { password: hashedPassword, passwordUpdatedAt: new Date() } }
    );

    // Đồng bộ cập nhật mật khẩu trên Firebase Auth (nếu có)
    try {
      const userRecord = await admin.auth().getUserByEmail(email);
      await admin.auth().updateUser(userRecord.uid, {
        password: newPassword
      });
    } catch (fbErr) {
      console.log('Không tìm thấy tài khoản tương ứng trên Firebase Auth, bỏ qua đồng bộ.');
    }

    // 3. Xoá OTP đã dùng
    await Otp.deleteOne({ _id: otpRecord._id });

    res.json({ success: true, message: 'Đặt lại mật khẩu thành công.' });
  } catch (error) {
    console.error('Lỗi khi cập nhật mật khẩu:', error);
    res.status(500).json({ error: 'Không thể đặt lại mật khẩu. Vui lòng thử lại sau.' });
  }
}));

module.exports = router;
