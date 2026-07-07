const mongoose = require('mongoose');

const walletTransactionSchema = new mongoose.Schema(
  {
    transactionId: { type: String, required: true, unique: true, index: true },
    customerId: { type: String, required: true, index: true }, // Links to User.CustomerID
    amount: { type: Number, required: true }, // Positive for deposit/refund/cashback, negative for payment
    type: { type: String, enum: ['deposit', 'refund', 'payment', 'cashback'], required: true },
    status: { type: String, enum: ['pending', 'completed', 'failed'], default: 'completed' },
    referenceId: { type: String, default: '' }, // e.g. Order ID, Bank Transaction ID
    description: { type: String, default: '' }
  },
  {
    timestamps: true,
    collection: 'wallet_transactions'
  }
);

module.exports = mongoose.model('WalletTransaction', walletTransactionSchema);
