const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

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
    PasswordVersion: 3 // Phiên bản mật khẩu mới nhất
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

  // Tìm user theo Phone (Field PascalCase trong MongoDB)
  const user = await User.findOne({ Phone: phone });
  if (!user || !user.Password) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  // So sánh mật khẩu bằng bcrypt
  const isMatch = await bcrypt.compare(password, user.Password);
  if (!isMatch) {
    return res.status(401).json({ message: 'Số điện thoại hoặc mật khẩu không đúng' });
  }

  // Thành công: Trả về user không có Password
  const userResponse = user.toObject();
  delete userResponse.Password;

  res.json(userResponse);
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
    { firebaseUid, name, email, phone, avatarUrl },
    { new: true, upsert: true }
  );
  res.json(user);
}));

module.exports = router;
