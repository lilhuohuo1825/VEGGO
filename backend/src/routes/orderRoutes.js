const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');
const { refreshCustomerOrderMetrics, isDeliveredStatus } = require('../services/certificateService');
const { validatePromotionForCustomer } = require('../utils/promotionEligibility');
const {
  processReminderForOrder,
  isRecurringShipping,
  formatDeliveryWindow,
} = require('../services/scheduledDeliveryReminderService');

const router = express.Router();
const Wallet = require('../models/Wallet');
const WalletTransaction = require('../models/WalletTransaction');

function generateWalletTxId() {
  const timestamp = Date.now().toString().slice(-6);
  const random = Math.floor(1000 + Math.random() * 9000);
  return `VP${timestamp}${random}`;
}

const ORDER_STATUSES = new Set([
  'pending',
  'confirmed',
  'shipping',
  'delivered',
  'unreview',
  'reviewed',
  'processing_return',
  'returning',
  'returned',
  'rejected',
  'cancelled',
  'completed',
]);

const PAYMENT_STATUSES = new Set(['unpaid', 'paid']);

const statusLabels = {
  pending: 'Chờ xác nhận',
  confirmed: 'Đã xác nhận',
  shipping: 'Đang giao',
  delivered: 'Đã giao',
  unreview: 'Chưa đánh giá',
  reviewed: 'Đã đánh giá',
  processing_return: 'Đang xử lý trả hàng/hoàn tiền',
  returning: 'Đang hoàn/trả',
  returned: 'Đã hoàn/trả',
  rejected: 'Từ chối hoàn/trả',
  cancelled: 'Đã huỷ',
  completed: 'Hoàn thành',
};

const paymentLabels = {
  unpaid: 'Chưa thanh toán',
  paid: 'Đã thanh toán',
};

const normalizePaymentMethod = (method) => {
  const value = String(method || '').trim().toLowerCase();
  if (['bank', 'banking', 'bank_transfer', 'transfer'].includes(value)) return 'bank';
  if (value === 'momo') return 'momo';
  if (value === 'vnpay') return 'vnpay';
  if (value === 'veggopay' || value === 'wallet') return 'veggopay';
  return 'cod';
};

const defaultPaymentStatus = (method, explicitStatus) => {
  const status = String(explicitStatus || '').trim().toLowerCase();
  if (PAYMENT_STATUSES.has(status)) return status;
  const norm = normalizePaymentMethod(method);
  if (norm === 'veggopay') return 'paid';
  return 'unpaid';
};

const toNumber = (value, fallback = 0) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

const buildOrderId = () => `ORD${Date.now()}${Math.floor(Math.random() * 900 + 100)}`;

const buildNotification = (customerId, orderId, title, body, type = 'status') => ({
  CustomerID: customerId,
  OrderID: orderId,
  category: 'orders',
  type,
  title,
  body,
  action: 'Theo dõi đơn',
  iconText: type === 'payment' ? '$' : '✓',
  targetType: 'order',
  targetId: orderId,
  isRead: false,
  createdAt: new Date(),
});

const createOrderNotification = async (customerId, orderId, title, body, type = 'status') => {
  if (!customerId || !orderId) return;
  await mongoose.connection.db
    .collection('notifications')
    .insertOne(buildNotification(customerId, orderId, title, body, type));
};

const createAdminNotification = async ({ type, customerId, orderId, orderTotal = 0, reason = '', title = '', message = '' }) => {
  if (!type || !orderId) return;
  await mongoose.connection.db.collection('admin_notifications').insertOne({
    type,
    customerId,
    orderId,
    orderTotal,
    reason,
    title,
    message,
    status: 'pending',
    read: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  });
};

const refreshCustomerMetricsAfterOrderChange = async (customerId, orderId, shouldEvaluateCertificate = false) => {
  if (!customerId) return;
  try {
    await refreshCustomerOrderMetrics(customerId, shouldEvaluateCertificate ? orderId : null);
  } catch (error) {
    console.error('[customer metrics refresh] Failed:', error);
  }
};

const recordPromotionUsage = async (promotionId, orderId, customerId) => {
  const cleanPromotionId = String(promotionId || '').trim();
  const cleanOrderId = String(orderId || '').trim();
  const cleanCustomerId = String(customerId || '').trim();
  if (!cleanPromotionId || !cleanOrderId || !cleanCustomerId) return;

  const collection = mongoose.connection.db.collection('promotion_usages');
  await collection.updateOne(
    { promotion_id: cleanPromotionId },
    {
      $addToSet: {
        order_id: cleanOrderId,
        user_id: cleanCustomerId,
      },
      $setOnInsert: {
        promotion_id: cleanPromotionId,
        created_at: new Date(),
      },
      $set: {
        updated_at: new Date(),
      },
    },
    { upsert: true }
  );

  const usage = await collection.findOne({ promotion_id: cleanPromotionId }, { projection: { order_id: 1 } });
  const usageCount = Array.isArray(usage?.order_id) ? usage.order_id.length : 0;
  await collection.updateOne(
    { promotion_id: cleanPromotionId },
    { $set: { usage_count: usageCount, updated_at: new Date() } }
  );
};

const statusNotificationBody = (status, orderId) => {
  const label = statusLabels[status] || status;
  switch (status) {
    case 'pending':
      return `Đơn hàng #${orderId} đã được đặt thành công và đang chờ xác nhận.`;
    case 'shipping':
      return `Đơn hàng #${orderId} đã được xác nhận và đang được giao đến bạn.`;
    case 'delivered':
      return `Đơn hàng #${orderId} đã được giao. Vui lòng xác nhận đã nhận hàng hoặc yêu cầu trả hàng nếu cần.`;
    case 'unreview':
      return `Bạn đã xác nhận nhận đơn #${orderId}. Hãy đánh giá sản phẩm để hoàn tất đơn hàng.`;
    case 'reviewed':
      return `Cảm ơn bạn đã đánh giá đơn hàng #${orderId}.`;
    case 'processing_return':
      return `Yêu cầu trả hàng/hoàn tiền cho đơn #${orderId} đang chờ admin xử lý.`;
    case 'returning':
      return `Admin đã xác nhận hoàn/trả đơn #${orderId}. Vui lòng hoàn tất trả hàng khi đã nhận được xử lý.`;
    case 'returned':
      return `Đơn hàng #${orderId} đã hoàn/trả thành công.`;
    case 'rejected':
      return `Yêu cầu hoàn/trả đơn #${orderId} đã bị từ chối.`;
    case 'cancelled':
      return `Đơn hàng #${orderId} đã bị huỷ.`;
    default:
      return `Đơn hàng #${orderId} đã cập nhật trạng thái: ${label}.`;
  }
};

const attachPaymentStatus = (order) => {
  if (!order.paymentStatus) {
    const method = normalizePaymentMethod(order.paymentMethod);
    if (['delivered', 'unreview', 'reviewed', 'completed'].includes(String(order.status || '').toLowerCase())) {
      order.paymentStatus = 'paid';
    } else {
      order.paymentStatus = method === 'cod' ? 'unpaid' : 'paid';
    }
  }
  return order;
};

const attachOrderDetails = async (orders, { lite = false } = {}) => {
  const orderIds = orders.map(o => o.OrderID).filter(Boolean);
  const projection = lite
    ? {
        OrderID: 1,
        shippingInfo: 1,
        promotion_id: 1,
        shippingPromotionId: 1,
        shipping_promotion_id: 1,
        TotalCarbonEmission: 1,
        CarbonPointEarned: 1,
        'items.sku': 1,
        'items.SKU': 1,
        'items.product_id': 1,
        'items.productId': 1,
        'items.ProductID': 1,
        'items.quantity': 1,
        'items.qty': 1,
        'items.Quantity': 1,
        'items.price': 1,
        'items.salePrice': 1,
        'items.sale_price': 1,
        'items.ProductPrice': 1,
        'items.base_price': 1,
        'items.basePrice': 1,
        'items.name': 1,
        'items.product_name': 1,
        'items.ProductName': 1,
      }
    : undefined;
  const details = orderIds.length
    ? await mongoose.connection.db
      .collection('order_details')
      .find({ OrderID: { $in: orderIds } }, projection ? { projection } : undefined)
      .toArray()
    : [];

  const detailsMap = {};
  details.forEach(d => {
    detailsMap[d.OrderID] = d;
  });

  return orders.map(o => {
    attachPaymentStatus(o);
    const d = detailsMap[o.OrderID];
    if (d) {
      o.shippingInfo = d.shippingInfo || {};
      o.items = d.items || [];
      o.promotion_id = d.promotion_id || null;
      o.shippingPromotionId = d.shippingPromotionId || d.shipping_promotion_id || null;
      o.TotalCarbonEmission = d.TotalCarbonEmission || 0;
      o.CarbonPointEarned = d.CarbonPointEarned || 0;
    }
    return o;
  });
};

let ordersListCache = null;
let ordersListCacheTime = 0;
const ORDERS_LIST_CACHE_MS = 10000;

function invalidateOrdersListCache() {
  ordersListCache = null;
  ordersListCacheTime = 0;
}

// Get all orders (active)
router.get('/', asyncHandler(async (req, res) => {
  const lite = req.query.lite === 'true';
  const cacheKey = lite ? 'lite' : 'full';
  const now = Date.now();

  if (
    ordersListCache
    && ordersListCache.key === cacheKey
    && (now - ordersListCacheTime < ORDERS_LIST_CACHE_MS)
  ) {
    return res.json(ordersListCache.data);
  }

  const orderProjection = lite
    ? {
        OrderID: 1,
        CustomerID: 1,
        status: 1,
        paymentStatus: 1,
        paymentMethod: 1,
        totalAmount: 1,
        subtotal: 1,
        shippingFee: 1,
        shippingDiscount: 1,
        discount: 1,
        cancelReason: 1,
        returnReason: 1,
        createdAt: 1,
        updatedAt: 1,
        routes: 1,
      }
    : undefined;

  const orders = await mongoose.connection.db
    .collection('orders')
    .find({ status: { $ne: 'deleted' } }, orderProjection ? { projection: orderProjection } : undefined)
    .sort({ createdAt: -1 })
    .toArray();

  const mapped = await attachOrderDetails(orders, { lite });
  ordersListCache = { key: cacheKey, data: mapped };
  ordersListCacheTime = now;

  res.json(mapped);
}));

// Get orders by CustomerID (explicit route used by admin detail)
router.get('/customer/:customerId', asyncHandler(async (req, res) => {
  const lite = req.query.lite === 'true';
  const orders = await mongoose.connection.db
    .collection('orders')
    .find({ CustomerID: req.params.customerId, status: { $ne: 'deleted' } })
    .sort({ createdAt: -1 })
    .toArray();
  res.json({ success: true, orders: await attachOrderDetails(orders, { lite }) });
}));

// Get order details by Specific OrderID or ID
router.get('/id/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    const targetId = req.params.id;
    const strippedId = targetId.replace(/^(VG|ORD)/i, '');
    query = {
      $or: [
        { OrderID: targetId },
        { OrderID: 'VG' + targetId },
        { OrderID: 'ORD' + targetId },
        { OrderID: 'VG' + strippedId },
        { OrderID: 'ORD' + strippedId },
        { OrderID: strippedId }
      ]
    };
  }

  const order = await mongoose.connection.db.collection('orders').findOne(query);
  if (!order) {
    return res.status(404).json({ message: 'Order not found' });
  }
  attachPaymentStatus(order);

  // Fetch items and shipping info from order_details
  const orderDetail = await mongoose.connection.db.collection('order_details').findOne({ OrderID: order.OrderID });
  if (orderDetail) {
    order.items = orderDetail.items || [];
    order.shippingInfo = orderDetail.shippingInfo || {};
    order.promotion_id = orderDetail.promotion_id || null;
    order.shippingPromotionId = orderDetail.shippingPromotionId || orderDetail.shipping_promotion_id || null;
    order.TotalCarbonEmission = orderDetail.TotalCarbonEmission || 0;
    order.CarbonPointEarned = orderDetail.CarbonPointEarned || 0;
  } else {
    order.items = [];
    order.shippingInfo = {};
    order.TotalCarbonEmission = order.TotalCarbonEmission || 0;
    order.CarbonPointEarned = order.CarbonPointEarned || 0;
  }

  res.json(order);
}));

router.get('/guest/search', asyncHandler(async (req, res) => {
  const orderId = String(req.query.orderId || '').trim();
  const phone = String(req.query.phone || '').trim();
  if (!orderId || !phone) {
    return res.status(400).json({ message: 'orderId and phone are required' });
  }

  const order = await mongoose.connection.db.collection('orders').findOne({
    OrderID: orderId,
    $or: [
      { isGuestOrder: true },
      { CustomerID: /^GUEST_/ }
    ],
  });
  if (!order) {
    return res.status(404).json({ message: 'Order not found' });
  }
  const detail = await mongoose.connection.db.collection('order_details').findOne({ OrderID: orderId });
  const shippingPhone = String(detail?.shippingInfo?.phone || '').trim();
  if (shippingPhone !== phone) {
    return res.status(404).json({ message: 'Order not found' });
  }
  attachPaymentStatus(order);
  res.json({
    orderId: order.OrderID,
    status: order.status || 'pending',
    paymentMethod: order.paymentMethod || '',
    paymentStatus: order.paymentStatus || defaultPaymentStatus(order.paymentMethod),
    total: order.totalAmount || 0,
    createdAt: order.createdAt,
    shippingInfo: detail?.shippingInfo || {},
    items: detail?.items || [],
  });
}));

/**
 * GET /api/orders/:userId
 * userId here is actually the CustomerID (e.g. CUS000002)
 * Orders are stored with field "CustomerID", items in "order_details" joined by "OrderID"
 */
router.get('/:userId', asyncHandler(async (req, res) => {
  const customerId = req.params.userId;
  console.log(`[GET /api/orders] Requested for CustomerID: ${customerId}`);

  const db = mongoose.connection.db;

  // 1. Fetch orders by CustomerID
  const orders = await db.collection('orders')
    .find({ CustomerID: customerId })
    .sort({ createdAt: -1 })
    .toArray();

  console.log(`[GET /api/orders] Found ${orders.length} orders`);

  if (orders.length === 0) {
    return res.json([]);
  }

  // 2. Fetch matching order_details
  const orderIds = orders.map(o => o.OrderID).filter(Boolean);
  const detailsList = await db.collection('order_details')
    .find({ OrderID: { $in: orderIds } })
    .toArray();

  // Build map OrderID -> detail
  const detailMap = {};
  for (const d of detailsList) {
    detailMap[d.OrderID] = d;
  }

  // 3. Merge and respond with a unified shape the Android app expects
  const result = orders.map(order => {
    attachPaymentStatus(order);
    const detail = detailMap[order.OrderID] || {};
    const shippingInfo = detail.shippingInfo || {};
    const shippingAddress = shippingInfo.address
      ? {
          receiverName: shippingInfo.fullName || '',
          phone: shippingInfo.phone || '',
          email: shippingInfo.email || '',
          line1: shippingInfo.address.detail || '',
          ward: shippingInfo.address.ward || '',
          district: shippingInfo.address.district || '',
          city: shippingInfo.address.city || '',
        }
      : {};

    const items = (detail.items || []).map(item => ({
      name: item.productName || '',
      price: item.price || 0,
      quantity: item.quantity || 0,
      imageUrl: item.image || '',
      sku: item.sku || '',
      unit: item.unit || '',
      originalPrice: item.originalPrice || item.price || 0,
      CarbonPointEarned: item.CarbonPointEarned || 0,
      TotalCarbonEmission: item.TotalCarbonEmission || 0,
    }));

    // Normalise createdAt: handle MongoDB extended JSON { $date: ... }
    let createdAt = order.createdAt;
    if (createdAt && typeof createdAt === 'object' && createdAt.$date) {
      createdAt = new Date(createdAt.$date).toISOString();
    } else if (createdAt instanceof Date) {
      createdAt = createdAt.toISOString();
    }

    return {
      _id: order._id ? order._id.toString() : order.OrderID,
      orderId: order.OrderID || '',
      userId: order.CustomerID || '',
      paymentMethod: order.paymentMethod || '',
      paymentStatus: order.paymentStatus || defaultPaymentStatus(order.paymentMethod),
      subtotal: order.subtotal || 0,
      shippingFee: order.shippingFee || 0,
      shippingDiscount: order.shippingDiscount || 0,
      discount: order.discount || 0,
      total: order.totalAmount || 0,
      status: order.status || 'pending',
      shippingAddress,
      warehouseId: shippingInfo.warehouse_id || '',
      items,
      CarbonPointEarned: detail.CarbonPointEarned || order.CarbonPointEarned || 0,
      TotalCarbonEmission: detail.TotalCarbonEmission || order.TotalCarbonEmission || 0,
      createdAt,
    };
  });

  res.json(result);
}));

// Get order notifications for the Android notification screen.
router.get('/notifications/:customerId', asyncHandler(async (req, res) => {
  const notifications = await mongoose.connection.db
    .collection('notifications')
    .find({
      CustomerID: req.params.customerId,
    })
    .sort({ createdAt: -1 })
    .limit(50)
    .toArray();

  res.json(notifications);
}));

router.patch('/notifications/:customerId/read-all', asyncHandler(async (req, res) => {
  const result = await mongoose.connection.db
    .collection('notifications')
    .updateMany(
      {
        CustomerID: req.params.customerId,
        isRead: { $ne: true },
      },
      {
        $set: {
          isRead: true,
          readAt: new Date(),
        },
      }
    );

  res.json({ success: true, modifiedCount: result.modifiedCount || 0 });
}));

router.patch('/notifications/item/:notificationId/read', asyncHandler(async (req, res) => {
  const { notificationId } = req.params;
  if (!mongoose.Types.ObjectId.isValid(notificationId)) {
    return res.status(400).json({ message: 'Valid notificationId is required' });
  }

  const result = await mongoose.connection.db
    .collection('notifications')
    .findOneAndUpdate(
      { _id: new mongoose.Types.ObjectId(notificationId) },
      {
        $set: {
          isRead: true,
          readAt: new Date(),
        },
      },
      { returnDocument: 'after' }
    );

  if (!result) {
    return res.status(404).json({ message: 'Notification not found' });
  }

  res.json({ success: true, notification: result });
}));

// Create order into both orders and order_details collections.
router.post('/', asyncHandler(async (req, res) => {
  const db = mongoose.connection.db;
  const now = new Date();
  const orderId = req.body.OrderID || req.body.orderId || buildOrderId();
  const customerId = req.body.CustomerID || req.body.customerId || req.body.userId;

  if (!customerId) {
    return res.status(400).json({ message: 'CustomerID is required' });
  }

  const items = Array.isArray(req.body.items) ? req.body.items : [];
  if (items.length === 0) {
    return res.status(400).json({ message: 'Order items are required' });
  }

  const paymentMethod = normalizePaymentMethod(req.body.paymentMethod);
  const status = ORDER_STATUSES.has(String(req.body.status || '').toLowerCase())
    ? String(req.body.status).toLowerCase()
    : 'pending';
  const paymentStatus = defaultPaymentStatus(paymentMethod, req.body.paymentStatus);
  const subtotal = toNumber(req.body.subtotal);
  const shippingFee = toNumber(req.body.shippingFee, 30000);
  const shippingDiscount = toNumber(req.body.shippingDiscount);
  const discount = toNumber(req.body.discount);
  const totalAmount = toNumber(req.body.totalAmount ?? req.body.total, subtotal + shippingFee - shippingDiscount - discount);
  const requestedTotalCarbonEmission = toNumber(req.body.TotalCarbonEmission ?? req.body.totalCarbonEmission);
  const requestedCarbonPointEarned = toNumber(req.body.CarbonPointEarned ?? req.body.carbonPointEarned);

  const promotionId = req.body.promotion_id || req.body.promotionId || null;
  const shippingPromotionId = req.body.shippingPromotionId || req.body.shipping_promotion_id || null;

  const productPromotionCheck = await validatePromotionForCustomer(promotionId, customerId);
  if (!productPromotionCheck.ok) {
    return res.status(400).json({ message: productPromotionCheck.message });
  }
  const shippingPromotionCheck = await validatePromotionForCustomer(shippingPromotionId, customerId);
  if (!shippingPromotionCheck.ok) {
    return res.status(400).json({ message: shippingPromotionCheck.message });
  }

  if (paymentMethod === 'veggopay') {
    const { walletPassword } = req.body;
    let wallet = await Wallet.findOne({ customerId });
    if (!wallet) {
      wallet = new Wallet({ customerId, balance: 0, status: 'inactive', linkedBanks: [] });
      await wallet.save();
    }
    if (wallet.status !== 'active') {
      return res.status(400).json({ message: 'Ví VeggoPay chưa được kích hoạt' });
    }
    if (!walletPassword || wallet.password !== walletPassword) {
      return res.status(400).json({ message: 'Mật khẩu ví VeggoPay không chính xác' });
    }
    if (wallet.balance < totalAmount) {
      return res.status(400).json({ message: 'Số dư ví VeggoPay không đủ để thanh toán đơn hàng này' });
    }

    wallet.balance -= totalAmount;
    await wallet.save();

    const transaction = new WalletTransaction({
      transactionId: generateWalletTxId(),
      customerId,
      amount: -totalAmount,
      type: 'payment',
      status: 'completed',
      referenceId: orderId,
      description: `Thanh toán đơn hàng #${orderId}`
    });
    await transaction.save();
  }

  const orderDoc = {
    OrderID: orderId,
    CustomerID: customerId,
    paymentMethod,
    paymentStatus,
    subtotal,
    shippingFee,
    shippingDiscount,
    discount,
    vatRate: toNumber(req.body.vatRate),
    vatAmount: toNumber(req.body.vatAmount),
    totalAmount,
    code: req.body.code || promotionId || '',
    promotionName: req.body.promotionName || '',
    shippingPromotionId: shippingPromotionId || null,
    shippingPromotionName: req.body.shippingPromotionName || '',
    wantInvoice: Boolean(req.body.wantInvoice),
    invoiceInfo: req.body.invoiceInfo || {},
    consultantCode: req.body.consultantCode || '',
    cancelReason: req.body.cancelReason || '',
    returnReason: req.body.returnReason || '',
    status,
    isGuestOrder: Boolean(req.body.isGuestOrder) || String(customerId).startsWith('GUEST_'),
    TotalCarbonEmission: requestedTotalCarbonEmission,
    CarbonPointEarned: requestedCarbonPointEarned,
    routes: { [status]: now },
    createdAt: now,
    updatedAt: now,
    __v: 0,
  };

  const detailItems = items.map(item => ({
    sku: item.sku || '',
    productName: item.productName || item.name || '',
    quantity: toNumber(item.quantity, 1),
    price: toNumber(item.price),
    image: item.image || item.imageUrl || '',
    unit: item.unit || '',
    weight: item.weight || item.Weight || item.unit || '',
    selectedWeight: item.selectedWeight !== undefined ? toNumber(item.selectedWeight) : undefined,
    itemType: item.itemType || 'purchased',
    originalPrice: toNumber(item.originalPrice, toNumber(item.price)),
    _id: item._id || item.productId || undefined,
    CategoryID: item.CategoryID || item.categoryId || item.category || '',
    SubcategoryID: item.SubcategoryID || item.subcategoryId || item.subcategory || null,
    TotalCarbonEmission: toNumber(item.TotalCarbonEmission ?? item.totalCarbonEmission),
    CarbonPointEarned: toNumber(item.CarbonPointEarned ?? item.carbonPointEarned),
  }));

  const totalCarbonEmission = toNumber(
    req.body.TotalCarbonEmission ?? req.body.totalCarbonEmission,
    detailItems.reduce((sum, item) => sum + toNumber(item.TotalCarbonEmission), 0)
  );
  const carbonPointEarned = toNumber(
    req.body.CarbonPointEarned ?? req.body.carbonPointEarned,
    detailItems.reduce((sum, item) => sum + toNumber(item.CarbonPointEarned), 0)
  );
  orderDoc.TotalCarbonEmission = totalCarbonEmission;
  orderDoc.CarbonPointEarned = carbonPointEarned;

  const orderDetailDoc = {
    OrderID: orderId,
    shippingInfo: req.body.shippingInfo || {},
    items: detailItems,
    promotion_id: promotionId,
    shippingPromotionId: shippingPromotionId || null,
    shippingPromotionName: req.body.shippingPromotionName || '',
    TotalCarbonEmission: totalCarbonEmission,
    CarbonPointEarned: carbonPointEarned,
  };

  const existing = await db.collection('orders').findOne({ OrderID: orderId });
  if (existing) {
    return res.status(409).json({ message: 'OrderID already exists' });
  }

  const result = await db.collection('orders').insertOne(orderDoc);
  await db.collection('order_details').insertOne(orderDetailDoc);
  await createOrderNotification(
    customerId,
    orderId,
    'Đặt hàng thành công',
    statusNotificationBody(status, orderId),
    'created'
  );

  const shippingInfo = orderDetailDoc.shippingInfo || {};
  const recurringOrder = isRecurringShipping(shippingInfo);
  const deliveryText = formatDeliveryWindow(shippingInfo);
  const recurringLabel = shippingInfo.recurringOrderName
    ? ` "${shippingInfo.recurringOrderName}"`
    : '';

  await createAdminNotification({
    type: 'new_order',
    customerId,
    orderId,
    orderTotal: totalAmount,
    isRecurring: recurringOrder,
    title: recurringOrder
      ? `Đơn định kỳ mới #${orderId}`
      : `Đơn hàng mới #${orderId}`,
    message: recurringOrder
      ? `Đơn định kỳ${recurringLabel} #${orderId}${deliveryText ? ` - giao ${deliveryText}` : ''}. Tổng ${totalAmount.toLocaleString('vi-VN')}₫.`
      : `Có đơn hàng mới từ khách hàng ${customerId} với tổng giá trị ${totalAmount.toLocaleString('vi-VN')}₫${deliveryText ? ` - giao ${deliveryText}` : ''}.`,
  });
  await processReminderForOrder(db, orderId);
  await recordPromotionUsage(orderDetailDoc.promotion_id, orderId, customerId);
  await recordPromotionUsage(orderDetailDoc.shippingPromotionId, orderId, customerId);
  await refreshCustomerMetricsAfterOrderChange(customerId, orderId, isDeliveredStatus(status));
  invalidateOrdersListCache();

  res.status(201).json({
    _id: result.insertedId,
    orderId,
    OrderID: orderId,
    userId: customerId,
    CustomerID: customerId,
    paymentMethod,
    paymentStatus,
    subtotal,
    shippingFee,
    shippingDiscount,
    discount,
    total: totalAmount,
    totalAmount,
    status,
    items: detailItems.map(item => ({
      name: item.productName,
      price: item.price,
      quantity: item.quantity,
      imageUrl: item.image,
      sku: item.sku,
      unit: item.unit,
      originalPrice: item.originalPrice,
      CarbonPointEarned: item.CarbonPointEarned,
      TotalCarbonEmission: item.TotalCarbonEmission,
    })),
    CarbonPointEarned: carbonPointEarned,
    TotalCarbonEmission: totalCarbonEmission,
    createdAt: now.toISOString(),
  });
}));

/**
 * PATCH /api/orders/:orderId/status
 * Cập nhật trạng thái đơn hàng (by OrderID field, e.g. ORD...)
 */
router.patch('/:orderId/status', asyncHandler(async (req, res) => {
  const { orderId } = req.params;
  const { status, returnReason, cancelReason, rejectReason, returnEvidenceUrls } = req.body;

  const nextStatus = String(status || '').trim().toLowerCase();
  if (!ORDER_STATUSES.has(nextStatus)) {
    return res.status(400).json({ message: 'Valid status is required' });
  }

  const db = mongoose.connection.db;
  const setData = {
    status: nextStatus,
    updatedAt: new Date(),
    [`routes.${nextStatus}`]: new Date(),
  };
  if (['unreview', 'reviewed', 'completed'].includes(nextStatus)) {
    setData.paymentStatus = 'paid';
  }
  if (nextStatus === 'cancelled') {
    setData.paymentStatus = 'unpaid';
  }
  if (returnReason) setData.returnReason = returnReason;
  if (cancelReason) setData.cancelReason = cancelReason;
  if (rejectReason) setData.rejectReason = rejectReason;
  if (returnEvidenceUrls) {
    setData.returnEvidenceUrls = Array.isArray(returnEvidenceUrls)
      ? returnEvidenceUrls
      : String(returnEvidenceUrls).split(',').map(value => value.trim()).filter(Boolean);
  }

  const targetId = orderId;
  const strippedId = targetId.replace(/^(VG|ORD)/i, '');
  const query = {
    $or: [
      { OrderID: targetId },
      { OrderID: 'VG' + targetId },
      { OrderID: 'ORD' + targetId },
      { OrderID: 'VG' + strippedId },
      { OrderID: 'ORD' + strippedId },
      { OrderID: strippedId }
    ]
  };

  const result = await db.collection('orders').findOneAndUpdate(
    query,
    { $set: setData },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ message: 'Order not found' });
  }

  // VeggoPay Refund on Cancellation
  if (nextStatus === 'cancelled' && result.paymentMethod === 'veggopay') {
    const existingRefund = await WalletTransaction.findOne({ referenceId: result.OrderID, type: 'refund' });
    if (!existingRefund) {
      let wallet = await Wallet.findOne({ customerId: result.CustomerID });
      if (!wallet) {
        wallet = new Wallet({ customerId: result.CustomerID, balance: 0, status: 'active', linkedBanks: [] });
      }
      const refundAmount = result.totalAmount || 0;
      wallet.balance += refundAmount;
      await wallet.save();

      const refundTx = new WalletTransaction({
        transactionId: generateWalletTxId(),
        customerId: result.CustomerID,
        amount: refundAmount,
        type: 'refund',
        status: 'completed',
        referenceId: result.OrderID,
        description: `Hoàn tiền đơn hàng hủy #${result.OrderID}`
      });
      await refundTx.save();

      await createOrderNotification(
        result.CustomerID,
        result.OrderID,
        'Biến động số dư',
        `Ví VeggoPay đã hoàn +${refundAmount.toLocaleString('vi-VN')}đ do đơn hàng #${result.OrderID} bị hủy.`,
        'payment'
      );
    }
  }

  // VeggoPay Cashback on Completed Order (2% Cashback)
  if (['completed', 'unreview', 'reviewed'].includes(nextStatus) && result.paymentMethod === 'veggopay') {
    const existingCashback = await WalletTransaction.findOne({ referenceId: result.OrderID, type: 'cashback' });
    if (!existingCashback) {
      let wallet = await Wallet.findOne({ customerId: result.CustomerID });
      if (!wallet) {
        wallet = new Wallet({ customerId: result.CustomerID, balance: 0, status: 'active', linkedBanks: [] });
      }
      const cashbackAmount = Math.round((result.totalAmount || 0) * 0.02);
      if (cashbackAmount > 0) {
        wallet.balance += cashbackAmount;
        await wallet.save();

        const cashbackTx = new WalletTransaction({
          transactionId: generateWalletTxId(),
          customerId: result.CustomerID,
          amount: cashbackAmount,
          type: 'cashback',
          status: 'completed',
          referenceId: result.OrderID,
          description: `Hoàn tiền đặc quyền VeggoPay 2% đơn #${result.OrderID}`
        });
        await cashbackTx.save();

        await createOrderNotification(
          result.CustomerID,
          result.OrderID,
          'Biến động số dư',
          `Ví VeggoPay nhận +${cashbackAmount.toLocaleString('vi-VN')}đ (hoàn 2%) cho đơn hàng #${result.OrderID}.`,
          'payment'
        );
      }
    }
  }

  // VeggoPay Refund on Return/Refund Request Approved
  if (nextStatus === 'returned') {
    const existingRefund = await WalletTransaction.findOne({ referenceId: result.OrderID, type: 'refund' });
    if (!existingRefund) {
      let wallet = await Wallet.findOne({ customerId: result.CustomerID });
      if (!wallet) {
        wallet = new Wallet({ customerId: result.CustomerID, balance: 0, status: 'active', linkedBanks: [] });
      }
      const refundAmount = result.totalAmount || 0;
      wallet.balance += refundAmount;
      await wallet.save();

      const refundTx = new WalletTransaction({
        transactionId: generateWalletTxId(),
        customerId: result.CustomerID,
        amount: refundAmount,
        type: 'refund',
        status: 'completed',
        referenceId: result.OrderID,
        description: `Hoàn tiền trả hàng đơn hàng #${result.OrderID}`
      });
      await refundTx.save();

      await createOrderNotification(
        result.CustomerID,
        result.OrderID,
        'Biến động số dư',
        `Ví VeggoPay đã hoàn +${refundAmount.toLocaleString('vi-VN')}đ do trả hàng đơn #${result.OrderID}.`,
        'payment'
      );
    }
  }

  await createOrderNotification(
    result.CustomerID,
    result.OrderID,
    'Cập nhật đơn hàng',
    statusNotificationBody(nextStatus, result.OrderID)
  );

  if (nextStatus === 'processing_return') {
    await createAdminNotification({
      type: 'return_request',
      customerId: result.CustomerID,
      orderId: result.OrderID,
      orderTotal: result.totalAmount || 0,
      reason: returnReason || result.returnReason || '',
      title: `Yêu cầu trả hàng/hoàn tiền #${result.OrderID}`,
      message: `Khách hàng ${result.CustomerID} vừa gửi yêu cầu trả hàng/hoàn tiền cho đơn ${result.OrderID}.`,
    });
  }

  // Chỉ recalc spending/carbon khi trạng thái thực sự ảnh hưởng metrics (completed/unreview/reviewed/returned).
  // Admin workflow (pending→shipping→delivered, returning, rejected…) không cần quét toàn bộ products/orders.
  const METRIC_REFRESH_STATUSES = new Set(['completed', 'unreview', 'reviewed', 'returned']);
  if (METRIC_REFRESH_STATUSES.has(nextStatus)) {
    await refreshCustomerMetricsAfterOrderChange(
      result.CustomerID,
      result.OrderID,
      isDeliveredStatus(nextStatus)
    );
  }

  invalidateOrdersListCache();
  res.json({ orderId, status: nextStatus, message: 'Order status updated' });
}));

router.patch('/:orderId/payment-status', asyncHandler(async (req, res) => {
  const { orderId } = req.params;
  const paymentStatus = String(req.body.paymentStatus || '').trim().toLowerCase();
  if (!PAYMENT_STATUSES.has(paymentStatus)) {
    return res.status(400).json({ message: 'Valid paymentStatus is required' });
  }

  const result = await mongoose.connection.db.collection('orders').findOneAndUpdate(
    { OrderID: orderId },
    {
      $set: {
        paymentStatus,
        updatedAt: new Date(),
        [`routes.payment_${paymentStatus}`]: new Date(),
      }
    },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ message: 'Order not found' });
  }

  await createOrderNotification(
    result.CustomerID,
    result.OrderID,
    'Cập nhật thanh toán',
    `Đơn hàng #${result.OrderID} đã cập nhật trạng thái thanh toán: ${paymentLabels[paymentStatus]}.`,
    'payment'
  );

  invalidateOrdersListCache();
  res.json({ orderId, paymentStatus, message: 'Payment status updated' });
}));

// Update order status/details
router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    const targetId = req.params.id;
    const strippedId = targetId.replace(/^(VG|ORD)/i, '');
    query = {
      $or: [
        { OrderID: targetId },
        { OrderID: 'VG' + targetId },
        { OrderID: 'ORD' + targetId },
        { OrderID: 'VG' + strippedId },
        { OrderID: 'ORD' + strippedId },
        { OrderID: strippedId }
      ]
    };
  }

  const updateData = { ...req.body };
  delete updateData._id;
  if (updateData.paymentMethod) {
    updateData.paymentMethod = normalizePaymentMethod(updateData.paymentMethod);
  }
  if (updateData.paymentStatus) {
    updateData.paymentStatus = defaultPaymentStatus(updateData.paymentMethod, updateData.paymentStatus);
  }
  if (updateData.shippingInfo || updateData.items || updateData.promotion_id || updateData.promotionId
      || updateData.TotalCarbonEmission || updateData.CarbonPointEarned) {
    const detailUpdate = {};
    if (updateData.shippingInfo) {
      detailUpdate.shippingInfo = updateData.shippingInfo;
      delete updateData.shippingInfo;
    }
    if (updateData.items) {
      detailUpdate.items = updateData.items.map(item => ({
        sku: item.sku || '',
        productName: item.productName || item.name || '',
        quantity: toNumber(item.quantity, 1),
        price: toNumber(item.price),
        image: item.image || item.imageUrl || '',
        unit: item.unit || '',
        weight: item.weight || item.Weight || item.unit || '',
        selectedWeight: item.selectedWeight !== undefined ? toNumber(item.selectedWeight) : undefined,
        itemType: item.itemType || 'purchased',
        originalPrice: toNumber(item.originalPrice, toNumber(item.price)),
        _id: item._id || item.productId || undefined,
        CategoryID: item.CategoryID || item.categoryId || item.category || '',
        SubcategoryID: item.SubcategoryID || item.subcategoryId || item.subcategory || null,
        TotalCarbonEmission: toNumber(item.TotalCarbonEmission ?? item.totalCarbonEmission),
        CarbonPointEarned: toNumber(item.CarbonPointEarned ?? item.carbonPointEarned),
      }));
      delete updateData.items;
    }
    if (updateData.promotion_id || updateData.promotionId) {
      detailUpdate.promotion_id = updateData.promotion_id || updateData.promotionId;
      delete updateData.promotion_id;
      delete updateData.promotionId;
    }
    if (updateData.TotalCarbonEmission !== undefined) {
      detailUpdate.TotalCarbonEmission = toNumber(updateData.TotalCarbonEmission);
      delete updateData.TotalCarbonEmission;
    }
    if (updateData.CarbonPointEarned !== undefined) {
      detailUpdate.CarbonPointEarned = toNumber(updateData.CarbonPointEarned);
      delete updateData.CarbonPointEarned;
    }
    if (Object.keys(detailUpdate).length > 0) {
      await mongoose.connection.db.collection('order_details').updateOne(
        { OrderID: order.OrderID },
        { $set: detailUpdate },
        { upsert: true }
      );
    }
  }

  // If status changed, update the dates under the routes field
  if (updateData.status) {
    updateData.status = String(updateData.status).toLowerCase();
    const statusField = `routes.${updateData.status}`;
    await mongoose.connection.db.collection('orders').updateOne(query, {
      $set: {
        ...updateData,
        [statusField]: new Date(),
        updatedAt: new Date()
      }
    });
  } else {
    await mongoose.connection.db.collection('orders').updateOne(query, {
      $set: {
        ...updateData,
        updatedAt: new Date()
      }
    });
  }

  const updatedOrder = await mongoose.connection.db.collection('orders').findOne(query);
  if (updatedOrder?.CustomerID) {
    await refreshCustomerMetricsAfterOrderChange(
      updatedOrder.CustomerID,
      updatedOrder.OrderID,
      Boolean(updateData.status && isDeliveredStatus(updateData.status))
    );
  }
  if (updatedOrder?.CustomerID && updateData.status) {
    await createOrderNotification(
      updatedOrder.CustomerID,
      updatedOrder.OrderID,
      'Cập nhật đơn hàng',
      statusNotificationBody(updateData.status, updatedOrder.OrderID)
    );
  }
  invalidateOrdersListCache();
  res.json(updatedOrder);
}));

// Soft delete order
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    const targetId = req.params.id;
    const strippedId = targetId.replace(/^(VG|ORD)/i, '');
    query = {
      $or: [
        { OrderID: targetId },
        { OrderID: 'VG' + targetId },
        { OrderID: 'ORD' + targetId },
        { OrderID: 'VG' + strippedId },
        { OrderID: 'ORD' + strippedId },
        { OrderID: strippedId }
      ]
    };
  }

  await mongoose.connection.db.collection('orders').updateOne(query, {
    $set: { status: 'deleted', updatedAt: new Date() }
  });
  invalidateOrdersListCache();
  res.json({ success: true });
}));

module.exports = router;
