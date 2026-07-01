const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const qs = require('qs');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const VNP_TMN_CODE = (process.env.VNP_TMN_CODE || '29ZPJ1EU').trim();
const VNP_HASH_SECRET = (process.env.VNP_HASH_SECRET || 'IM078XAFK0OJHODUC6BTY0SHZU1VRTRZ').trim();
const VNP_URL = (process.env.VNP_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html').trim();

// Sort object by keys (VNPay requires alphabetical key order)
// Values are NOT encoded here - encoding happens only at URL building step
function sortObject(obj) {
  let sorted = {};
  let str = [];
  let key;
  for (key in obj) {
    if (obj.hasOwnProperty(key)) {
      str.push(encodeURIComponent(key));
    }
  }
  str.sort();
  for (key = 0; key < str.length; key++) {
    const val = obj[str[key]];
    if (val === undefined || val === null) {
      sorted[str[key]] = '';
    } else {
      sorted[str[key]] = encodeURIComponent(val).replace(/%20/g, "+");
    }
  }
  return sorted;
}

// Format date yyyyMMddHHmmss at GMT+7
function getVnpDate(extraMs = 0) {
  const d = new Date(Date.now() + 7 * 3600000 + extraMs);
  const pad = (n) => String(n).padStart(2, '0');
  return (
    d.getUTCFullYear() +
    pad(d.getUTCMonth() + 1) +
    pad(d.getUTCDate()) +
    pad(d.getUTCHours()) +
    pad(d.getUTCMinutes()) +
    pad(d.getUTCSeconds())
  );
}

// POST /api/payment/create-payment-url
router.post('/create-payment-url', asyncHandler(async (req, res) => {
  const { orderId, amount, orderInfo } = req.body;
  if (!orderId || !amount) {
    return res.status(400).json({ message: 'orderId va amount la bat buoc' });
  }

  let clientIp = req.headers['x-forwarded-for'] ||
    req.connection?.remoteAddress ||
    req.socket?.remoteAddress ||
    '127.0.0.1';
  // VNPay strictly requires IPv4 format, max 15 chars.
  if (clientIp.includes(',')) {
    clientIp = clientIp.split(',')[0].trim();
  }
  if (clientIp.includes('::ffff:')) {
    clientIp = clientIp.split('::ffff:')[1];
  }
  if (clientIp === '::1' || clientIp.length > 15) {
    clientIp = '12.34.56.78';
  }

  // Use configured return host or derive from request (ensure phone can reach this)
  const returnHost = process.env.VNP_RETURN_HOST || `${req.protocol}://${req.get('host')}`;
  const returnUrl = `${returnHost}/api/payment/vnpay-return`;


  const safeInfo = (orderInfo || 'ThanhToanDonHang' + orderId)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .trim()
    .slice(0, 255) || 'ThanhToanVEGGO';

  const parsedAmount = Number(amount);
  if (isNaN(parsedAmount) || parsedAmount <= 0) {
    return res.status(400).json({ message: 'Số tiền (amount) không hợp lệ' });
  }
  const vnpAmount = Math.round(parsedAmount) * 100;

  let vnp_Params = {};
  vnp_Params['vnp_Version'] = '2.1.0';
  vnp_Params['vnp_Command'] = 'pay';
  vnp_Params['vnp_TmnCode'] = VNP_TMN_CODE;
  vnp_Params['vnp_Locale'] = 'vn';
  vnp_Params['vnp_CurrCode'] = 'VND';
  vnp_Params['vnp_TxnRef'] = String(orderId);
  vnp_Params['vnp_OrderInfo'] = safeInfo;
  vnp_Params['vnp_OrderType'] = 'other';
  vnp_Params['vnp_Amount'] = String(vnpAmount);
  vnp_Params['vnp_ReturnUrl'] = returnUrl;
  vnp_Params['vnp_IpAddr'] = clientIp;
  vnp_Params['vnp_CreateDate'] = getVnpDate();
  vnp_Params['vnp_ExpireDate'] = getVnpDate(15 * 60 * 1000);

  // 1. Sort keys alphabetically and encode values
  const sortedParams = sortObject(vnp_Params);

  // 2. Build signData using qs.stringify with encode: false (values are already encoded by sortObject)
  const signData = qs.stringify(sortedParams, { encode: false });

  // 3. Create HMAC-SHA512 signature
  const hmac = crypto.createHmac('sha512', VNP_HASH_SECRET);
  const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest('hex');

  // 4. Build final URL (using encode: false since values are already encoded)
  sortedParams['vnp_SecureHash'] = signed;
  const paymentUrl = VNP_URL + '?' + qs.stringify(sortedParams, { encode: false });

  console.log('[VNPAY] orderId=%s amount=%d TmnCode=%s signData=%s', orderId, vnpAmount, VNP_TMN_CODE, signData.slice(0, 120));
  res.json({ paymentUrl });
}));

// GET /api/payment/vnpay-return  (VNPAY redirects browser here after payment)
router.get('/vnpay-return', asyncHandler(async (req, res) => {
  let vnp_Params = req.query;
  const secureHash = vnp_Params['vnp_SecureHash'];

  delete vnp_Params['vnp_SecureHash'];
  delete vnp_Params['vnp_SecureHashType'];

  vnp_Params = sortObject(vnp_Params);
  const signData = qs.stringify(vnp_Params, { encode: false });
  const hmac = crypto.createHmac("sha512", VNP_HASH_SECRET);
  const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

  const orderId = vnp_Params['vnp_TxnRef'] || req.query['vnp_TxnRef'] || '';
  const responseCode = vnp_Params['vnp_ResponseCode'] || req.query['vnp_ResponseCode'] || '99';
  const success = secureHash === signed && responseCode === '00';

  console.log('[VNPAY Return] orderId=%s code=%s hashMatch=%s', orderId, responseCode, secureHash === signed);

  // Update order status in DB
  if (orderId) {
    try {
      const db = mongoose.connection.db;
      await db.collection('orders').updateOne(
        { OrderID: orderId },
        {
          $set: {
            paymentStatus: success ? 'paid' : 'failed',
            vnpayResponseCode: responseCode,
            vnpayTransactionNo: req.query['vnp_TransactionNo'] || '',
          },
        }
      );
    } catch (e) {
      console.error('[VNPAY Return] DB update error:', e.message);
    }
  }

  // Redirect deep link ve app
  const deepLink = `veggo://payment-result?success=${success}&orderId=${encodeURIComponent(orderId)}&code=${responseCode}`;
  return res.redirect(deepLink);
}));

// GET /api/payment/vnpay-ipn  (server-to-server from VNPAY)
router.get('/vnpay-ipn', asyncHandler(async (req, res) => {
  let vnp_Params = req.query;
  const secureHash = vnp_Params['vnp_SecureHash'];

  delete vnp_Params['vnp_SecureHash'];
  delete vnp_Params['vnp_SecureHashType'];

  vnp_Params = sortObject(vnp_Params);
  const signData = qs.stringify(vnp_Params, { encode: false });
  const hmac = crypto.createHmac("sha512", VNP_HASH_SECRET);
  const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

  if (secureHash !== signed) {
    return res.status(200).json({ RspCode: '97', Message: 'Fail checksum' });
  }

  const orderId = vnp_Params['vnp_TxnRef'] || req.query['vnp_TxnRef'];
  const responseCode = vnp_Params['vnp_ResponseCode'] || req.query['vnp_ResponseCode'];
  const transactionStatus = req.query['vnp_TransactionStatus'];

  if (!orderId) return res.status(200).json({ RspCode: '01', Message: 'Order not found' });

  try {
    const db = mongoose.connection.db;
    const order = await db.collection('orders').findOne({ OrderID: orderId });
    if (!order) return res.status(200).json({ RspCode: '01', Message: 'Order not found' });
    if (order.paymentStatus === 'paid') return res.status(200).json({ RspCode: '02', Message: 'Order already confirmed' });

    const paid = responseCode === '00' && transactionStatus === '00';
    await db.collection('orders').updateOne(
      { OrderID: orderId },
      { $set: { paymentStatus: paid ? 'paid' : 'failed', vnpayResponseCode: responseCode } }
    );
    return res.status(200).json({ RspCode: '00', Message: 'Confirm Success' });
  } catch (e) {
    console.error('[VNPAY IPN] Error:', e.message);
    return res.status(200).json({ RspCode: '99', Message: 'Unknown error' });
  }
}));

module.exports = router;
