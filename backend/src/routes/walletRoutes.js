const express = require('express');
const router = express.Router();
const walletController = require('../controllers/walletController');
const asyncHandler = require('../middleware/asyncHandler');

router.get('/info', asyncHandler(walletController.getWalletInfo));
router.get('/find-recipient', asyncHandler(walletController.findRecipient));
router.get('/transactions', asyncHandler(walletController.getTransactions));
router.post('/link-bank', asyncHandler(walletController.linkBank));
router.post('/set-default-bank', asyncHandler(walletController.setDefaultBank));
router.post('/deposit', asyncHandler(walletController.deposit));
router.post('/activate', asyncHandler(walletController.activateWallet));
router.post('/verify-password', asyncHandler(walletController.verifyWalletPassword));
router.post('/transfer', asyncHandler(walletController.transferMoney));
router.get('/tree', asyncHandler(walletController.getTreeStatus));
router.post('/tree/activate', asyncHandler(walletController.activateSeed));
router.post('/tree/water', asyncHandler(walletController.waterTree));

module.exports = router;
