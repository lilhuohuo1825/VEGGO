const crypto = require('crypto');
const qs = require('qs');

const VNP_TMN_CODE = '1WO0XNLP';
const VNP_HASH_SECRET = '780XQXEF82GDTU466SG68LZRA0UE9SBB';

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
    sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, "+");
  }
  return sorted;
}

function getVnpDate(extraMs = 0) {
  const d = new Date(Date.now() + 7 * 3600000 + extraMs);
  const pad = (n) => String(n).padStart(2, '0');
  return (
    String(d.getUTCFullYear()) +
    pad(d.getUTCMonth() + 1) +
    pad(d.getUTCDate()) +
    pad(d.getUTCHours()) +
    pad(d.getUTCMinutes()) +
    pad(d.getUTCSeconds())
  );
}

let vnp_Params = {};
vnp_Params['vnp_Version'] = '2.1.0';
vnp_Params['vnp_Command'] = 'pay';
vnp_Params['vnp_TmnCode'] = VNP_TMN_CODE;
vnp_Params['vnp_Locale'] = 'vn';
vnp_Params['vnp_CurrCode'] = 'VND';
vnp_Params['vnp_TxnRef'] = "123456";
vnp_Params['vnp_OrderInfo'] = "ThanhToanVEGGO";
vnp_Params['vnp_OrderType'] = 'other';
vnp_Params['vnp_Amount'] = "1000000";
vnp_Params['vnp_ReturnUrl'] = "http://localhost:5001/api/payment/vnpay-return";
vnp_Params['vnp_IpAddr'] = "127.0.0.1";
vnp_Params['vnp_CreateDate'] = getVnpDate();
vnp_Params['vnp_ExpireDate'] = getVnpDate(15 * 60 * 1000);

console.log("Before sort:", vnp_Params);
vnp_Params = sortObject(vnp_Params);
console.log("After sort:", vnp_Params);

const signData = qs.stringify(vnp_Params, { encode: false });
console.log("SignData:", signData);

const hmac = crypto.createHmac("sha512", VNP_HASH_SECRET);
const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

vnp_Params['vnp_SecureHash'] = signed;
const VNP_URL = 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
const paymentUrl = VNP_URL + '?' + qs.stringify(vnp_Params, { encode: false });
console.log("URL:", paymentUrl);
