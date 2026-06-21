const express = require('express');
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const nodemailer = require('nodemailer');
const admin = require('firebase-admin');
const User = require('../models/User');
const Otp = require('../models/Otp');
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

/**
 * 5. Lấy thông tin user theo số điện thoại
 * GET /api/users/phone/:phone
 */
router.get('/phone/:phone', asyncHandler(async (req, res) => {
  const user = await User.findOne({ Phone: req.params.phone });
  if (!user) {
    return res.status(404).json({ message: 'Không tìm thấy người dùng' });
  }
  const userResponse = user.toObject();
  delete userResponse.Password;
  res.json(userResponse);
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

// --- Admin Customer Management Endpoints ---

// Get all users (excluding deactivated ones)
router.get('/', asyncHandler(async (req, res) => {
  const users = await mongoose.connection.db
    .collection('users')
    .find({ isActive: { $ne: false } })
    .sort({ RegisterDate: -1 })
    .toArray();
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

  // So sánh mật khẩu trực tiếp (theo database hiện tại lưu plain-text)
  if (adminUser.password !== password) {
    return res.status(401).json({ error: 'Mật khẩu không đúng' });
  }

  res.json({
    success: true,
    user: {
      id: adminUser._id.toString(),
      email: adminUser.email,
      name: adminUser.name || 'Admin User',
      role: 'admin'
    }
  });
}));

const getTransporter = () => {
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587'),
    secure: process.env.SMTP_PORT === '465',
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
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

  // 3. Gửi OTP qua Email dùng nodemailer
  if (!process.env.SMTP_USER || !process.env.SMTP_PASS) {
    console.warn(`[OTP DEV MOCK] Chưa cấu hình SMTP. Mã OTP cho ${email} là: ${otp}`);
    return res.json({
      success: true,
      message: '[MOCK] OTP đã được ghi nhận trong console (chưa cấu hình SMTP)',
      devOtp: otp
    });
  }

  const transporter = getTransporter();
  const mailOptions = {
    from: `"VEGGO Admin Support" <${process.env.SMTP_USER}>`,
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
    await mongoose.connection.db.collection('admins').updateOne(
      { email },
      { $set: { password: newPassword } }
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
