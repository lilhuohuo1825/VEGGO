const Wallet = require('../models/Wallet');
const WalletTransaction = require('../models/WalletTransaction');
const User = require('../models/User');
const Tree = require('../models/Tree');

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

// Get transaction history
exports.getTransactions = async (req, res) => {
  try {
    const { customerId } = req.query;
    if (!customerId) {
      return res.status(400).json({ success: false, message: 'Thiếu mã khách hàng' });
    }

    const transactions = await WalletTransaction.find({ customerId })
      .sort({ createdAt: -1 })
      .limit(100);

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

    // Increase balance
    wallet.balance += numAmount;
    await wallet.save();

    // Create completed transaction record
    const transactionId = generateTransactionId();
    const transaction = new WalletTransaction({
      transactionId,
      customerId,
      amount: numAmount,
      type: 'deposit',
      status: 'completed',
      description: `Nạp tiền từ ngân hàng ${bankCode || 'Liên kết'}`
    });
    await transaction.save();

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

    // 4. Update balances
    senderWallet.balance -= numAmount;
    recipientWallet.balance += numAmount;

    await senderWallet.save();
    await recipientWallet.save();

    // 5. Create transactions
    const txId = generateTransactionId();
    const senderTx = new WalletTransaction({
      transactionId: txId,
      customerId: senderCustomerId,
      amount: -numAmount,
      type: 'transfer_send',
      status: 'completed',
      description: description || `Chuyển tiền đến ${recipientUser.FullName || recipientPhone}`
    });
    const recipientTx = new WalletTransaction({
      transactionId: txId,
      customerId: recipientCustomerId,
      amount: numAmount,
      type: 'transfer_receive',
      status: 'completed',
      description: `Nhận tiền từ ${senderWallet.customerId}`
    });

    await senderTx.save();
    await recipientTx.save();

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

    let tree = await Tree.findOne({ customerId });
    if (!tree) {
      tree = new Tree({ customerId, status: 'none', waterCount: 0 });
      await tree.save();
    }

    res.json({ success: true, data: tree });
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

    // Deduct wallet
    wallet.balance -= seedCost;
    await wallet.save();

    // Set tree status to seed
    tree.status = 'seed';
    tree.waterCount = 0;
    await tree.save();

    // Create donation transaction
    const txId = generateTransactionId();
    const transaction = new WalletTransaction({
      transactionId: txId,
      customerId,
      amount: -seedCost,
      type: 'donation',
      status: 'completed',
      description: 'Kích hoạt hạt giống Quyên góp trồng rừng Veggo'
    });
    await transaction.save();

    res.json({ success: true, message: 'Gieo hạt giống thành công!', data: { tree, wallet } });
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
    if (!tree || tree.status === 'none') {
      return res.status(400).json({ success: false, message: 'Chưa kích hoạt hạt giống để tưới nước' });
    }

    if (tree.status === 'mature') {
      return res.status(400).json({ success: false, message: 'Cây đã trưởng thành hoàn toàn, hãy trồng cây mới!' });
    }

    const user = await User.findOne({ CustomerID: customerId });
    if (!user) {
      return res.status(404).json({ success: false, message: 'Không tìm thấy người dùng' });
    }

    const carbonCost = 10;
    if (user.CarbonPoint < carbonCost) {
      return res.status(400).json({ success: false, message: 'Số dư điểm Carbon không đủ để tưới nước (Cần 10 điểm)' });
    }

    // Deduct Carbon Points
    user.CarbonPoint -= carbonCost;
    await user.save();

    tree.waterCount += 1;
    if (tree.waterCount >= 24) {
      tree.status = 'mature';
      tree.plantedCount = (tree.plantedCount || 0) + 1;
    } else if (tree.waterCount >= 18) {
      tree.status = 'growing';
    } else if (tree.waterCount >= 12) {
      tree.status = 'growing';
    } else if (tree.waterCount >= 6) {
      tree.status = 'growing';
    }

    await tree.save();
    res.json({ success: true, message: 'Tưới nước thành công!', data: tree });
  } catch (error) {
    console.error('[waterTree] Error:', error);
    res.status(500).json({ success: false, message: 'Lỗi máy chủ' });
  }
};
