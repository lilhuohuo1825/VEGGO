const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');
const { refreshCustomerOrderMetrics, isDeliveredStatus } = require('../services/certificateService');

const router = express.Router();

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
  return 'cod';
};

const defaultPaymentStatus = (method, explicitStatus) => {
  const status = String(explicitStatus || '').trim().toLowerCase();
  if (PAYMENT_STATUSES.has(status)) return status;
  return normalizePaymentMethod(method) === 'cod' ? 'unpaid' : 'unpaid';
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

const attachOrderDetails = async (orders) => {
  const orderIds = orders.map(o => o.OrderID).filter(Boolean);
  const details = orderIds.length
    ? await mongoose.connection.db
      .collection('order_details')
      .find({ OrderID: { $in: orderIds } })
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
      o.TotalCarbonEmission = d.TotalCarbonEmission || 0;
      o.CarbonPointEarned = d.CarbonPointEarned || 0;
    }
    return o;
  });
};

// Get all orders (active)
router.get('/', asyncHandler(async (req, res) => {
  const orders = await mongoose.connection.db
    .collection('orders')
    .find({ status: { $ne: 'deleted' } })
    .sort({ createdAt: -1 })
    .toArray();

  const mapped = await attachOrderDetails(orders);

  res.json(mapped);
}));

// Get orders by CustomerID (explicit route used by admin detail)
router.get('/customer/:customerId', asyncHandler(async (req, res) => {
  const orders = await mongoose.connection.db
    .collection('orders')
    .find({ CustomerID: req.params.customerId, status: { $ne: 'deleted' } })
    .sort({ createdAt: -1 })
    .toArray();
  res.json({ success: true, orders: await attachOrderDetails(orders) });
}));

// Get order details by Specific OrderID or ID
router.get('/id/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { OrderID: req.params.id };
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
    code: req.body.code || '',
    promotionName: req.body.promotionName || '',
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
    promotion_id: req.body.promotion_id || req.body.promotionId || null,
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
  await createAdminNotification({
    type: 'new_order',
    customerId,
    orderId,
    orderTotal: totalAmount,
    title: `Đơn hàng mới #${orderId}`,
    message: `Có đơn hàng mới từ khách hàng ${customerId} với tổng giá trị ${totalAmount.toLocaleString('vi-VN')}₫.`,
  });
  await recordPromotionUsage(orderDetailDoc.promotion_id, orderId, customerId);
  await refreshCustomerMetricsAfterOrderChange(customerId, orderId, isDeliveredStatus(status));

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

  const result = await db.collection('orders').findOneAndUpdate(
    { OrderID: orderId },
    { $set: setData },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ message: 'Order not found' });
  }

  await refreshCustomerMetricsAfterOrderChange(result.CustomerID, result.OrderID, isDeliveredStatus(nextStatus));

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

  res.json({ orderId, paymentStatus, message: 'Payment status updated' });
}));

// Update order status/details
router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { OrderID: req.params.id };
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
        { OrderID: req.params.id },
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
  res.json(updatedOrder);
}));

// Soft delete order
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { OrderID: req.params.id };
  }

  await mongoose.connection.db.collection('orders').updateOne(query, {
    $set: { status: 'deleted', updatedAt: new Date() }
  });
  res.json({ success: true });
}));

module.exports = router;
