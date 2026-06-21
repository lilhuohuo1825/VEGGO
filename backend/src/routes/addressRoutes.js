const express = require('express');
const SavedAddress = require('../models/SavedAddress');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const PHONE_REGEX = /^0\d{9}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateAddressPayload(body, requireUserId) {
  const { userId, name, phone, email, province, district, ward, detail } = body;

  if (requireUserId && !userId) {
    return 'Thiếu userId';
  }
  if (!name || !String(name).trim()) {
    return 'Họ và tên không được để trống';
  }
  if (!phone || !PHONE_REGEX.test(String(phone).trim())) {
    return 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số';
  }
  if (email && String(email).trim() && !EMAIL_REGEX.test(String(email).trim())) {
    return 'Email không hợp lệ';
  }
  if (!province || !String(province).trim()) {
    return 'Vui lòng chọn Tỉnh/Thành phố';
  }
  if (!district || !String(district).trim()) {
    return 'Vui lòng chọn Quận/Huyện';
  }
  if (!ward || !String(ward).trim()) {
    return 'Vui lòng chọn Phường/Xã';
  }
  if (!detail || !String(detail).trim()) {
    return 'Địa chỉ cụ thể không được để trống';
  }
  return null;
}

function formatAddress(doc) {
  return {
    id: String(doc._id),
    userId: doc.userId,
    name: doc.name,
    phone: doc.phone,
    email: doc.email || '',
    province: doc.province,
    district: doc.district,
    ward: doc.ward,
    detail: doc.detail,
    isDefault: Boolean(doc.isDefault),
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
  };
}

async function clearOtherDefaults(userId, exceptId) {
  const filter = { userId, isDefault: true };
  if (exceptId) {
    filter._id = { $ne: exceptId };
  }
  await SavedAddress.updateMany(filter, { $set: { isDefault: false } });
}

router.get('/user/:userId', asyncHandler(async (req, res) => {
  const addresses = await SavedAddress.find({ userId: req.params.userId })
    .sort({ isDefault: -1, updatedAt: -1 })
    .lean();

  res.json(addresses.map(formatAddress));
}));

router.post('/', asyncHandler(async (req, res) => {
  const validationError = validateAddressPayload(req.body, true);
  if (validationError) {
    return res.status(400).json({ message: validationError });
  }

  const { userId, name, phone, email, province, district, ward, detail, isDefault } = req.body;
  const existingCount = await SavedAddress.countDocuments({ userId });
  const shouldBeDefault = Boolean(isDefault) || existingCount === 0;

  if (shouldBeDefault) {
    await clearOtherDefaults(userId);
  }

  const address = await SavedAddress.create({
    userId,
    name: String(name).trim(),
    phone: String(phone).trim(),
    email: email ? String(email).trim() : '',
    province: String(province).trim(),
    district: String(district).trim(),
    ward: String(ward).trim(),
    detail: String(detail).trim(),
    isDefault: shouldBeDefault,
  });

  res.status(201).json(formatAddress(address));
}));

router.put('/:id', asyncHandler(async (req, res) => {
  const validationError = validateAddressPayload(req.body, false);
  if (validationError) {
    return res.status(400).json({ message: validationError });
  }

  const address = await SavedAddress.findById(req.params.id);
  if (!address) {
    return res.status(404).json({ message: 'Không tìm thấy địa chỉ' });
  }

  const { name, phone, email, province, district, ward, detail, isDefault } = req.body;

  address.name = String(name).trim();
  address.phone = String(phone).trim();
  address.email = email ? String(email).trim() : '';
  address.province = String(province).trim();
  address.district = String(district).trim();
  address.ward = String(ward).trim();
  address.detail = String(detail).trim();

  if (Boolean(isDefault)) {
    await clearOtherDefaults(address.userId, address._id);
    address.isDefault = true;
  } else if (isDefault === false) {
    address.isDefault = false;
  }

  await address.save();
  res.json(formatAddress(address));
}));

router.delete('/:id', asyncHandler(async (req, res) => {
  const address = await SavedAddress.findById(req.params.id);
  if (!address) {
    return res.status(404).json({ message: 'Không tìm thấy địa chỉ' });
  }

  const wasDefault = address.isDefault;
  const userId = address.userId;
  await address.deleteOne();

  if (wasDefault) {
    const nextDefault = await SavedAddress.findOne({ userId }).sort({ updatedAt: -1 });
    if (nextDefault) {
      nextDefault.isDefault = true;
      await nextDefault.save();
    }
  }

  res.status(204).send();
}));

router.patch('/:id/default', asyncHandler(async (req, res) => {
  const address = await SavedAddress.findById(req.params.id);
  if (!address) {
    return res.status(404).json({ message: 'Không tìm thấy địa chỉ' });
  }

  await clearOtherDefaults(address.userId, address._id);
  address.isDefault = true;
  await address.save();
  res.json(formatAddress(address));
}));

module.exports = router;
