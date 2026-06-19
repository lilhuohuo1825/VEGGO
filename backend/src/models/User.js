const mongoose = require('mongoose');

const addressSchema = new mongoose.Schema(
  {
    receiverName: String,
    phone: String,
    line1: String,
    ward: String,
    district: String,
    city: String,
    isDefault: { type: Boolean, default: false },
  },
  { _id: false }
);

const userSchema = new mongoose.Schema(
  {
    // Firebase Auth (Dùng cho đăng nhập bằng Google/Firebase)
    firebaseUid: { type: String, sparse: true, unique: true, index: true },
    name: { type: String, default: '' }, // Field cũ của Firebase sync
    email: { type: String, default: '' }, // Field cũ của Firebase sync
    phone: { type: String, default: '' }, // Field cũ của Firebase sync
    avatarUrl: { type: String, default: '' },
    addresses: [addressSchema],

    // Traditional Auth (SSOT - Theo format MongoDB hiện tại)
    CustomerID: { type: String, unique: true, sparse: true, index: true },
    Phone: { type: String, unique: true, sparse: true, index: true },
    Password: { type: String },
    FullName: { type: String, default: '' },
    Email: { type: String, default: '' },
    Address: { type: String, default: null },
    CustomerType: { type: String, default: '' },
    TotalSpent: { type: Number, default: 0 },
    CarbonPoint: { type: Number, default: 0 },
    CertificateID: { type: String, default: null },
    PasswordVersion: { type: Number, default: 1 },
    LastPasswordReset: { type: Date, default: null },
    tastePreferences: { type: mongoose.Schema.Types.Mixed, default: null }
  },
  {
    // Tự động quản lý RegisterDate (createdAt) và updated_at (updatedAt)
    timestamps: { createdAt: 'RegisterDate', updatedAt: 'updated_at' },
    collection: 'users'
  }
);

// Hàm tạo CustomerID tự động (CUS000001, ...)
userSchema.statics.generateNextCustomerId = async function() {
  const lastUser = await this.findOne({ CustomerID: /^CUS/ }).sort({ CustomerID: -1 });
  let nextNumber = 1;
  if (lastUser && lastUser.CustomerID) {
    const lastNumber = parseInt(lastUser.CustomerID.substring(3));
    if (!isNaN(lastNumber)) {
      nextNumber = lastNumber + 1;
    }
  }
  return `CUS${nextNumber.toString().padStart(6, '0')}`;
};

module.exports = mongoose.model('User', userSchema);
