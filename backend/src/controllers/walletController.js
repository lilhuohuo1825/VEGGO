const Wallet = require('../models/Wallet');
const WalletTransaction = require('../models/WalletTransaction');
const User = require('../models/User');
const Tree = require('../models/Tree');
const { recalculateCustomerCarbon } = require('../services/certificateService');

const CARBON_SYNC_VERSION = 2;
const WATER_CARBON_COST = 5;

// Find VeggoPay recipient by phone number
exports.findRecipient = async (req, res) => {
  try {
    const { phone } = req.query;
    if (!phone) {
      return res.status(400).json({ success: false, message: 'Thiếu số điện thoại' });
    }

    // Clean up input phone number (remove spaces, etc.)
    const cleanPhone = phone.trim();
    const recipientUser = await User.findOne({ Phone: cleanPhone });
    if (!recipientUser) {
      return res.status(404).json({ success: false, message: 'Số điện thoại chưa đăng ký tài khoản Veggo' });
    }

    const recipientWallet = await Wallet.findOne({ customerId: recipientUser.CustomerID });
    if (!recipientWallet || recipientWallet.status !== 'active') {
      return res.status(400).json({ success: false, message: 'Người dùng này chưa kích hoạt ví VeggoPay' });
    }

    res.json({
      success: true,
      name: recipientUser.FullName,
      customerId: recipientUser.CustomerID
    });
  } catch (error) {
    console.error('[findRecipient] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Helper to generate transaction ID: VP + timestamp + random digits
function generateTransactionId() {
  const timestamp = Date.now().toString().slice(-6);
  const random = Math.floor(1000 + Math.random() * 9000);
  return `VP${timestamp}${random}`;
}

/** Record a wallet balance movement for "Biến động số dư" history. */
async function recordWalletTransaction({
  customerId,
  amount,
  type,
  description,
  referenceId = '',
  carbonPoints = 0,
  status = 'completed',
}) {
  const transaction = new WalletTransaction({
    transactionId: generateTransactionId(),
    customerId,
    amount,
    type,
    status,
    referenceId,
    description,
    carbonPoints,
  });
  await transaction.save();
  return transaction;
}

/**
 * One-time reset so tree progress and carbon ledger stay in sync.
 * Clears legacy watering counts/transactions and recalculates carbon from orders.
 */
async function ensureTreeCarbonSync(customerId, tree) {
  if (!tree || toTreeNumber(tree.carbonSyncVersion) >= CARBON_SYNC_VERSION) {
    return tree;
  }

  tree.waterCount = 0;
  tree.totalWaterCount = 0;
  tree.plantedCount = 0;
  tree.status = 'none';
  tree.carbonSyncVersion = CARBON_SYNC_VERSION;
  await tree.save();

  await WalletTransaction.deleteMany({ customerId, type: 'carbon_watering' });
  await recalculateCustomerCarbon(customerId);

  return tree;
}

function toTreeNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

// Get wallet balance and linked banks (lazily creates wallet if not exists)
exports.getWalletInfo = async (req, res) => {
  try {
    const { customerId } = req.query;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    // Verify user exists
    const user = await User.findOne({ CustomerID: customerId });
    if (!user) {
      return res.status(404).json({ success: false, message: 'Không tìm thấy người dùng' });
    }

    let wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      wallet = new Wallet({
        customerId,
        balance: 0,
        status: 'inactive',
        linkedBanks: []
      });
      await wallet.save();
    }

    res.json({ success: true, data: wallet });
  } catch (error) {
    console.error('[getWalletInfo] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Activate wallet
exports.activateWallet = async (req, res) => {
  try {
    const { customerId, password } = req.body;
    if (!customerId || !password) {
      return res.status(400).json({ success: false, message: 'Thiếu thông tin kích hoạt' });
    }

    let wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      wallet = new Wallet({ customerId, balance: 0, status: 'inactive', linkedBanks: [] });
    }

    wallet.password = password;
    wallet.status = 'active';
    await wallet.save();

    res.json({ success: true, message: 'Kích hoạt ví VeggoPay thành công', data: wallet });
  } catch (error) {
    console.error('[activateWallet] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Verify wallet password
exports.verifyWalletPassword = async (req, res) => {
  try {
    const { customerId, password } = req.body;
    if (!customerId || !password) {
      return res.status(400).json({ success: false, message: 'Thiếu thông tin xác thực' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      return res.status(404).json({ success: false, message: 'Ví chưa được đăng ký' });
    }

    if (wallet.status !== 'active') {
      return res.status(400).json({ success: false, message: 'Ví chưa được kích hoạt' });
    }

    if (wallet.password !== password) {
      return res.status(400).json({ success: false, message: 'Mật khẩu ví không đúng' });
    }

    res.json({ success: true, message: 'Xác thực mật khẩu thành công' });
  } catch (error) {
    console.error('[verifyWalletPassword] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Get transaction history (wallet balance movements only; carbon watering excluded)
exports.getTransactions = async (req, res) => {
  try {
    const { customerId, type } = req.query;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    const filter = { customerId };
    if (type) {
      filter.type = type;
    } else {
      // Carbon watering is tracked separately in carbon history, not wallet balance.
      filter.type = { $ne: 'carbon_watering' };
    }

    const transactions = await WalletTransaction.find(filter)
      .sort({ createdAt: -1 })
      .limit(500)
      .lean();

    res.json({ success: true, data: transactions });
  } catch (error) {
    console.error('[getTransactions] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Link bank account
exports.linkBank = async (req, res) => {
  try {
    const { customerId, bankCode, accountNumber, accountHolder } = req.body;
    if (!customerId || !bankCode || !accountNumber || !accountHolder) {
      return res.status(400).json({ success: false, message: 'Thông tin liên kết không đầy đủ' });
    }

    let wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      wallet = new Wallet({ customerId, balance: 0, status: 'inactive', linkedBanks: [] });
    }

    // Check if bank account already linked
    const exists = wallet.linkedBanks.some(b => b.bankCode === bankCode && b.accountNumber === accountNumber);
    if (exists) {
      return res.status(400).json({ success: false, message: 'Tài khoản ngân hàng này đã được liên kết' });
    }

    // Add new linked bank
    const isDefault = wallet.linkedBanks.length === 0;
    wallet.linkedBanks.push({ bankCode, accountNumber, accountHolder, isDefault });
    await wallet.save();

    res.json({ success: true, message: 'Liên kết ngân hàng thành công', data: wallet });
  } catch (error) {
    console.error('[linkBank] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Set default bank account
exports.setDefaultBank = async (req, res) => {
  try {
    const { customerId, bankCode, accountNumber } = req.body;
    if (!customerId || !bankCode || !accountNumber) {
      return res.status(400).json({ success: false, message: 'Thiếu thông tin yêu cầu' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      return res.status(404).json({ success: false, message: 'Ví không tồn tại' });
    }

    wallet.linkedBanks = wallet.linkedBanks.map(b => {
      b.isDefault = (b.bankCode === bankCode && b.accountNumber === accountNumber);
      return b;
    });

    await wallet.save();
    res.json({ success: true, message: 'Đổi tài khoản mặc định thành công', data: wallet });
  } catch (error) {
    console.error('[setDefaultBank] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Update linked bank account details
exports.updateLinkedBank = async (req, res) => {
  try {
    const { customerId, oldBankCode, oldAccountNumber, bankCode, accountNumber, accountHolder } = req.body;
    if (!customerId || !oldBankCode || !oldAccountNumber || !bankCode || !accountNumber || !accountHolder) {
      return res.status(400).json({ success: false, message: 'Thông tin cập nhật không đầy đủ' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      return res.status(404).json({ success: false, message: 'Ví không tồn tại' });
    }

    const targetIndex = wallet.linkedBanks.findIndex(
      b => b.bankCode === oldBankCode && b.accountNumber === oldAccountNumber
    );
    if (targetIndex === -1) {
      return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản liên kết' });
    }

    const duplicate = wallet.linkedBanks.some(
      (b, index) => index !== targetIndex && b.bankCode === bankCode && b.accountNumber === accountNumber
    );
    if (duplicate) {
      return res.status(400).json({ success: false, message: 'Tài khoản ngân hàng này đã được liên kết' });
    }

    const wasDefault = wallet.linkedBanks[targetIndex].isDefault;
    wallet.linkedBanks[targetIndex] = {
      bankCode,
      accountNumber,
      accountHolder,
      isDefault: wasDefault
    };
    await wallet.save();

    res.json({ success: true, message: 'Cập nhật tài khoản liên kết thành công', data: wallet });
  } catch (error) {
    console.error('[updateLinkedBank] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Remove linked bank account
exports.unlinkBank = async (req, res) => {
  try {
    const { customerId, bankCode, accountNumber } = req.body;
    if (!customerId || !bankCode || !accountNumber) {
      return res.status(400).json({ success: false, message: 'Thiếu thông tin yêu cầu' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      return res.status(404).json({ success: false, message: 'Ví không tồn tại' });
    }

    const targetIndex = wallet.linkedBanks.findIndex(
      b => b.bankCode === bankCode && b.accountNumber === accountNumber
    );
    if (targetIndex === -1) {
      return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản liên kết' });
    }

    const wasDefault = wallet.linkedBanks[targetIndex].isDefault;
    wallet.linkedBanks.splice(targetIndex, 1);

    if (wasDefault && wallet.linkedBanks.length > 0) {
      wallet.linkedBanks[0].isDefault = true;
    }

    await wallet.save();
    res.json({ success: true, message: 'Xóa liên kết ngân hàng thành công', data: wallet });
  } catch (error) {
    console.error('[unlinkBank] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

exports.deposit = async (req, res) => {
  try {
    const { customerId, amount, bankCode, password } = req.body;
    const numAmount = Number(amount);
    if (!customerId || isNaN(numAmount) || numAmount <= 0) {
      return res.status(400).json({ success: false, message: 'Thông tin nạp tiền không hợp lệ' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      return res.status(404).json({ success: false, message: 'Ví chưa được đăng ký' });
    }

    if (wallet.status !== 'active') {
      return res.status(400).json({ success: false, message: 'Ví chưa được kích hoạt' });
    }

    if (wallet.password !== password) {
      return res.status(400).json({ success: false, message: 'Mật khẩu ví không đúng' });
    }

    wallet.balance += numAmount;
    await wallet.save();

    const transaction = await recordWalletTransaction({
      customerId,
      amount: numAmount,
      type: 'deposit',
      description: `Nạp tiền từ ngân hàng ${bankCode || 'Liên kết'}`,
    });

    res.json({
      success: true,
      message: 'Nạp tiền vào ví VeggoPay thành công',
      data: {
        balance: wallet.balance,
        transaction
      }
    });
  } catch (error) {
    console.error('[deposit] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Transfer money nội bộ giữa 2 tài khoản dùng VeggoPay
exports.transferMoney = async (req, res) => {
  try {
    const { senderCustomerId, recipientPhone, amount, description, password } = req.body;
    const numAmount = Number(amount);

    if (!senderCustomerId || !recipientPhone || isNaN(numAmount) || numAmount <= 0 || !password) {
      return res.status(400).json({ success: false, message: 'Thông tin chuyển khoản không hợp lệ' });
    }

    // 1. Verify sender wallet & password
    const senderWallet = await Wallet.findOne({ customerId: senderCustomerId });
    if (!senderWallet) {
      return res.status(404).json({ success: false, message: 'Ví của người gửi chưa được đăng ký' });
    }
    if (senderWallet.status !== 'active') {
      return res.status(400).json({ success: false, message: 'Ví của người gửi chưa được kích hoạt' });
    }
    if (password !== "OTP_VERIFIED" && senderWallet.password !== password) {
      return res.status(400).json({ success: false, message: 'Mật khẩu ví không đúng' });
    }
    if (senderWallet.balance < numAmount) {
      return res.status(400).json({ success: false, message: 'Số dư ví VeggoPay không đủ để thực hiện giao dịch' });
    }

    // 2. Find recipient user by Phone
    const recipientUser = await User.findOne({ Phone: recipientPhone });
    if (!recipientUser) {
      return res.status(404).json({ success: false, message: 'Người nhận chưa đăng ký tài khoản Veggo' });
    }
    const recipientCustomerId = recipientUser.CustomerID;
    if (senderCustomerId === recipientCustomerId) {
      return res.status(400).json({ success: false, message: 'Không thể tự chuyển khoản cho chính mình' });
    }

    // 3. Find or lazily create recipient wallet
    let recipientWallet = await Wallet.findOne({ customerId: recipientCustomerId });
    if (!recipientWallet) {
      recipientWallet = new Wallet({
        customerId: recipientCustomerId,
        balance: 0,
        status: 'inactive',
        linkedBanks: []
      });
    }

    senderWallet.balance -= numAmount;
    recipientWallet.balance += numAmount;

    await senderWallet.save();
    await recipientWallet.save();

    const txId = generateTransactionId();
    await recordWalletTransaction({
      customerId: senderCustomerId,
      amount: -numAmount,
      type: 'transfer_send',
      description: description || `Chuyển tiền đến ${recipientUser.FullName || recipientPhone}`,
      referenceId: txId,
    });
    await recordWalletTransaction({
      customerId: recipientCustomerId,
      amount: numAmount,
      type: 'transfer_receive',
      description: `Nhận tiền từ ${senderWallet.customerId}`,
      referenceId: txId,
    });

    res.json({
      success: true,
      message: 'Chuyển khoản thành công!',
      data: senderWallet
    });
  } catch (error) {
    console.error('[transferMoney] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Get tree status
exports.getTreeStatus = async (req, res) => {
  try {
    const { customerId } = req.query;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    const user = await User.findOne({ CustomerID: customerId });
    if (!user) {
      return res.status(404).json({ success: false, message: 'Không tìm thấy người dùng' });
    }

    let tree = await Tree.findOne({ customerId });
    if (!tree) {
      tree = new Tree({ customerId, status: 'none', waterCount: 0, totalWaterCount: 0 });
      await tree.save();
    }

    tree = await ensureTreeCarbonSync(customerId, tree);

    const syncedUser = await User.findOne({ CustomerID: customerId });
    const carbonPoint = syncedUser ? toTreeNumber(syncedUser.CarbonPoint, 0) : 0;
    const plantedCount = toTreeNumber(tree.plantedCount, 0);

    res.json({
      success: true,
      data: {
        ...tree.toObject(),
        plantedCount,
        carbonPoint,
      }
    });
  } catch (error) {
    console.error('[getTreeStatus] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Activate seed (costs 10,000 VNĐ)
exports.activateSeed = async (req, res) => {
  try {
    const { customerId } = req.body;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    const wallet = await Wallet.findOne({ customerId });
    if (!wallet || wallet.status !== 'active') {
      return res.status(400).json({ success: false, message: 'Ví chưa được kích hoạt' });
    }

    const seedCost = 10000;
    if (wallet.balance < seedCost) {
      return res.status(400).json({ success: false, message: 'Số dư ví VeggoPay không đủ (Cần 10.000 ₫ để kích hoạt hạt giống)' });
    }

    let tree = await Tree.findOne({ customerId });
    if (!tree) {
      tree = new Tree({ customerId });
    }

    if (tree.status !== 'none' && tree.status !== 'mature') {
      return res.status(400).json({ success: false, message: 'Hạt giống đã được kích hoạt trước đó và đang phát triển' });
    }

    // Record wallet movement first so "Biến động số dư" never misses a deduction.
    const transaction = await recordWalletTransaction({
      customerId,
      amount: -seedCost,
      type: 'donation',
      description: 'Kích hoạt hạt giống trồng cây VeggoPay (-10.000₫)',
      referenceId: 'tree_seed',
    });

    wallet.balance -= seedCost;
    await wallet.save();

    tree.status = 'seed';
    tree.waterCount = 0;
    await tree.save();

    res.json({
      success: true,
      message: 'Gieo hạt giống thành công!',
      data: { tree, wallet, transaction },
    });
  } catch (error) {
    console.error('[activateSeed] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};

// Water tree
exports.waterTree = async (req, res) => {
  try {
    const { customerId } = req.body;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    let tree = await Tree.findOne({ customerId });
    if (!tree) {
      return res.status(400).json({ success: false, message: 'Chưa kích hoạt hạt giống để tưới nước' });
    }

    tree = await ensureTreeCarbonSync(customerId, tree);

    if (tree.status === 'none') {
      return res.status(400).json({ success: false, message: 'Chưa kích hoạt hạt giống để tưới nước' });
    }

    if (tree.status === 'mature') {
      return res.status(400).json({ success: false, message: 'Cây đã trưởng thành hoàn toàn, hãy trồng cây mới!' });
    }

    const carbonSummary = await recalculateCustomerCarbon(customerId);
    if (carbonSummary.totalCarbonPoint < WATER_CARBON_COST) {
      return res.status(400).json({
        success: false,
        message: `Số dư điểm Carbon không đủ để tưới nước (Cần ${WATER_CARBON_COST} điểm)`
      });
    }

    // Record carbon history first so balance + history always stay aligned.
    await recordWalletTransaction({
      customerId,
      amount: 0,
      type: 'carbon_watering',
      referenceId: String(tree._id || ''),
      carbonPoints: -WATER_CARBON_COST,
      description: `Tưới nước cho cây VeggoPay (-${WATER_CARBON_COST} điểm Carbon)`,
    });

    tree.waterCount += 1;
    tree.totalWaterCount = (tree.totalWaterCount || 0) + 1;
    if (tree.waterCount >= 24) {
      tree.status = 'mature';
      tree.plantedCount = (tree.plantedCount || 0) + 1;
      tree.waterCount = 0;
    } else if (tree.waterCount >= 6) {
      tree.status = 'growing';
    } else {
      tree.status = 'seed';
    }

    await tree.save();

    const updatedCarbon = await recalculateCustomerCarbon(customerId);

    res.json({
      success: true,
      message: 'Tưới nước thành công!',
      data: {
        ...tree.toObject(),
        carbonPoint: updatedCarbon.totalCarbonPoint
      }
    });
  } catch (error) {
    console.error('[waterTree] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};
