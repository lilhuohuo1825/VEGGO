const mongoose = require('mongoose');

const linkedBankSchema = new mongoose.Schema(
  {
    bankCode: { type: String, required: true },     // VCB, TCB, BIDV, etc.
    accountNumber: { type: String, required: true },
    accountHolder: { type: String, required: true },
    isDefault: { type: Boolean, default: false }
  },
  { _id: false }
);

const walletSchema = new mongoose.Schema(
  {
    customerId: { type: String, required: true, unique: true, index: true }, // Links to User.CustomerID
    balance: { type: Number, default: 0 },
    status: { type: String, enum: ['inactive', 'active', 'locked'], default: 'inactive' },
    password: { type: String, default: '' },
    linkedBanks: [linkedBankSchema]
  },
  {
    timestamps: true,
    collection: 'wallets'
  }
);

module.exports = mongoose.model('Wallet', walletSchema);
