const ACTIVE_ORDER_STATUSES = new Set(['pending', 'confirmed', 'shipping']);

const parseDate = (value) => {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDeliveryWindow = (shippingInfo = {}) => {
  if (shippingInfo.deliveryTimeText) {
    return String(shippingInfo.deliveryTimeText).trim();
  }
  const start = parseDate(shippingInfo.deliveryWindowStart);
  const end = parseDate(shippingInfo.deliveryWindowEnd);
  if (!start) return '';
  const datePart = start.toLocaleDateString('vi-VN');
  if (!end) return datePart;
  const startTime = start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const endTime = end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return `${datePart}, ${startTime} - ${endTime}`;
};

const isRecurringShipping = (shippingInfo = {}) => Boolean(
  shippingInfo.isRecurring
  || String(shippingInfo.orderSource || '').toLowerCase() === 'recurring'
);

const hasScheduledWindow = (shippingInfo = {}) => {
  const method = String(shippingInfo.deliveryMethod || '').toLowerCase();
  return Boolean(
    shippingInfo.deliveryWindowStart
    || method === 'scheduled'
    || isRecurringShipping(shippingInfo)
  );
};

const buildReminderKey = (orderId, windowStart) => {
  const day = windowStart.toISOString().slice(0, 10);
  return `${orderId}|${day}`;
};

const insertReminderIfNeeded = async (db, order, shippingInfo) => {
  if (!order || !shippingInfo || !hasScheduledWindow(shippingInfo)) {
    return false;
  }

  const windowStart = parseDate(shippingInfo.deliveryWindowStart);
  if (!windowStart) {
    return false;
  }

  const now = new Date();
  const in24Hours = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  if (windowStart < now || windowStart > in24Hours) {
    return false;
  }

  if (!ACTIVE_ORDER_STATUSES.has(String(order.status || '').toLowerCase())) {
    return false;
  }

  const orderId = order.OrderID;
  const reminderKey = buildReminderKey(orderId, windowStart);
  const existing = await db.collection('admin_notifications').findOne({
    type: 'scheduled_delivery_reminder',
    reminderKey,
  });
  if (existing) {
    return false;
  }

  const recurring = isRecurringShipping(shippingInfo);
  const deliveryText = formatDeliveryWindow(shippingInfo);
  const recurringName = shippingInfo.recurringOrderName
    ? ` "${shippingInfo.recurringOrderName}"`
    : '';

  await db.collection('admin_notifications').insertOne({
    type: 'scheduled_delivery_reminder',
    customerId: order.CustomerID,
    orderId,
    orderTotal: order.totalAmount || 0,
    reminderKey,
    isRecurring: recurring,
    deliveryWindowStart: windowStart,
    deliveryTimeText: deliveryText,
    title: recurring
      ? `Nhắc giao đơn định kỳ #${orderId}`
      : `Nhắc giao đơn theo lịch #${orderId}`,
    message: recurring
      ? `Đơn định kỳ${recurringName} #${orderId} cần giao ${deliveryText || 'theo lịch'}. Vui lòng chuẩn bị giao đúng giờ.`
      : `Đơn tiêu chuẩn #${orderId} cần giao ${deliveryText || 'theo lịch'}. Vui lòng chuẩn bị giao đúng giờ.`,
    status: 'pending',
    read: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  });

  return true;
};

const processScheduledDeliveryReminders = async (db) => {
  const orders = await db.collection('orders')
    .find({ status: { $in: Array.from(ACTIVE_ORDER_STATUSES) } })
    .toArray();
  if (!orders.length) {
    return 0;
  }

  const orderMap = {};
  orders.forEach((order) => {
    if (order.OrderID) {
      orderMap[order.OrderID] = order;
    }
  });

  const orderIds = Object.keys(orderMap);
  const details = await db.collection('order_details')
    .find({ OrderID: { $in: orderIds } })
    .toArray();

  let created = 0;
  for (const detail of details) {
  // eslint-disable-next-line no-await-in-loop
    const inserted = await insertReminderIfNeeded(
      db,
      orderMap[detail.OrderID],
      detail.shippingInfo || {}
    );
    if (inserted) {
      created += 1;
    }
  }
  return created;
};

const processReminderForOrder = async (db, orderId) => {
  const order = await db.collection('orders').findOne({ OrderID: orderId });
  if (!order) {
    return false;
  }
  const detail = await db.collection('order_details').findOne({ OrderID: orderId });
  return insertReminderIfNeeded(db, order, detail?.shippingInfo || {});
};

module.exports = {
  processScheduledDeliveryReminders,
  processReminderForOrder,
  isRecurringShipping,
  formatDeliveryWindow,
};
