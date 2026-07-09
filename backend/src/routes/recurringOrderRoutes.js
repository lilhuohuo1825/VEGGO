const express = require('express');
const mongoose = require('mongoose');
const RecurringOrder = require('../models/RecurringOrder');
const RecurringOrderOccurrence = require('../models/RecurringOrderOccurrence');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const notificationsCollection = () => mongoose.connection.db.collection('notifications');

function clean(value) {
  return String(value || '').trim();
}

function formatOrder(doc) {
  return {
    id: doc.recurringId,
    customerId: doc.customerId,
    name: doc.name || '',
    frequency: doc.frequency || '',
    deliveryDate: doc.deliveryDate || '',
    deliverySlot: doc.deliverySlot || '',
    receiverName: doc.receiverName || '',
    receiverPhone: doc.receiverPhone || '',
    city: doc.city || '',
    district: doc.district || '',
    ward: doc.ward || '',
    detailAddress: doc.detailAddress || '',
    itemSummary: doc.itemSummary || '',
    estimatedTotal: doc.estimatedTotal || 0,
    carbonPoints: doc.carbonPoints || 0,
    items: Array.isArray(doc.items) ? doc.items : [],
    status: doc.status || 'Active',
    createdAt: doc.createdAt || '',
  };
}

function formatOccurrence(doc) {
  return {
    orderId: doc.recurringOrderId,
    date: doc.date,
    status: doc.status || '',
    placedOrderId: doc.placedOrderId || '',
    confirmNotifiedAt: doc.confirmNotifiedAt || 0,
    deliveryReminderNotifiedAt: doc.deliveryReminderNotifiedAt || 0,
    notifiedAt: doc.notifiedAt || 0,
  };
}

function formatNotification(doc) {
  return {
    id: doc._id ? String(doc._id) : '',
    recurringOrderId: doc.recurringOrderId || '',
    occurrenceDate: doc.occurrenceDate || '',
    type: doc.type || '',
    title: doc.title || '',
    body: doc.body || doc.message || '',
    action: doc.action || '',
    createdAt: doc.createdAt ? new Date(doc.createdAt).getTime() : Date.now(),
    read: Boolean(doc.isRead),
    outsideApp: Boolean(doc.outsideApp),
  };
}

function buildOrderPayload(body) {
  const recurringId = clean(body.id || body.recurringId);
  const customerId = clean(body.customerId);
  if (!recurringId || !customerId) {
    return { error: 'Thiếu id hoặc customerId' };
  }

  return {
    payload: {
      recurringId,
      customerId,
      name: clean(body.name),
      frequency: clean(body.frequency),
      deliveryDate: clean(body.deliveryDate),
      deliverySlot: clean(body.deliverySlot),
      receiverName: clean(body.receiverName),
      receiverPhone: clean(body.receiverPhone),
      city: clean(body.city),
      district: clean(body.district),
      ward: clean(body.ward),
      detailAddress: clean(body.detailAddress),
      itemSummary: clean(body.itemSummary),
      estimatedTotal: Number(body.estimatedTotal || 0),
      carbonPoints: Number(body.carbonPoints || 0),
      items: Array.isArray(body.items) ? body.items : [],
      status: clean(body.status) || 'Active',
      createdAt: clean(body.createdAt),
    },
  };
}

router.get('/customer/:customerId', asyncHandler(async (req, res) => {
  const customerId = clean(req.params.customerId);
  const [orders, occurrences, notifications] = await Promise.all([
    RecurringOrder.find({ customerId }).sort({ createdAt: -1 }).lean(),
    RecurringOrderOccurrence.find({ customerId }).lean(),
    notificationsCollection()
      .find({
        CustomerID: customerId,
        type: { $in: ['recurring_confirm', 'recurring_delivery', 'recurring_skipped'] },
      })
      .sort({ createdAt: -1 })
      .limit(80)
      .toArray(),
  ]);

  res.json({
    orders: orders.map(formatOrder),
    occurrences: occurrences.map(formatOccurrence),
    notifications: notifications.map(formatNotification),
  });
}));

router.post('/', asyncHandler(async (req, res) => {
  const built = buildOrderPayload(req.body);
  if (built.error) {
    return res.status(400).json({ message: built.error });
  }

  const existing = await RecurringOrder.findOne({ recurringId: built.payload.recurringId });
  if (existing) {
    return res.status(409).json({ message: 'Đơn định kỳ đã tồn tại' });
  }

  const saved = await RecurringOrder.create(built.payload);
  res.status(201).json(formatOrder(saved));
}));

router.put('/:recurringId', asyncHandler(async (req, res) => {
  const recurringId = clean(req.params.recurringId);
  const built = buildOrderPayload({ ...req.body, id: recurringId, recurringId });
  if (built.error) {
    return res.status(400).json({ message: built.error });
  }

  const updated = await RecurringOrder.findOneAndUpdate(
    { recurringId },
    { $set: built.payload },
    { new: true, upsert: true }
  );

  res.json(formatOrder(updated));
}));

router.delete('/:recurringId', asyncHandler(async (req, res) => {
  const recurringId = clean(req.params.recurringId);
  await Promise.all([
    RecurringOrder.deleteOne({ recurringId }),
    RecurringOrderOccurrence.deleteMany({ recurringOrderId: recurringId }),
    notificationsCollection().deleteMany({ recurringOrderId: recurringId }),
  ]);
  res.json({ success: true });
}));

router.put('/:recurringId/occurrences/:date', asyncHandler(async (req, res) => {
  const recurringId = clean(req.params.recurringId);
  const date = clean(req.params.date);
  const customerId = clean(req.body.customerId);
  if (!recurringId || !date || !customerId) {
    return res.status(400).json({ message: 'Thiếu recurringId, date hoặc customerId' });
  }

  const payload = {
    recurringOrderId: recurringId,
    customerId,
    date,
    status: clean(req.body.status),
    placedOrderId: clean(req.body.placedOrderId),
    confirmNotifiedAt: Number(req.body.confirmNotifiedAt || 0),
    deliveryReminderNotifiedAt: Number(req.body.deliveryReminderNotifiedAt || 0),
    notifiedAt: Number(req.body.notifiedAt || 0),
  };

  const saved = await RecurringOrderOccurrence.findOneAndUpdate(
    { recurringOrderId: recurringId, date },
    { $set: payload },
    { new: true, upsert: true }
  );

  res.json(formatOccurrence(saved));
}));

router.post('/sync', asyncHandler(async (req, res) => {
  const customerId = clean(req.body.customerId);
  if (!customerId) {
    return res.status(400).json({ message: 'Thiếu customerId' });
  }

  const orders = Array.isArray(req.body.orders) ? req.body.orders : [];
  const occurrences = Array.isArray(req.body.occurrences) ? req.body.occurrences : [];

  for (const order of orders) {
    const built = buildOrderPayload({ ...order, customerId });
    if (built.error) continue;
    await RecurringOrder.findOneAndUpdate(
      { recurringId: built.payload.recurringId },
      { $set: built.payload },
      { upsert: true }
    );
  }

  for (const occurrence of occurrences) {
    const recurringOrderId = clean(occurrence.orderId || occurrence.recurringOrderId);
    const date = clean(occurrence.date);
    if (!recurringOrderId || !date) continue;
    await RecurringOrderOccurrence.findOneAndUpdate(
      { recurringOrderId, date },
      {
        $set: {
          recurringOrderId,
          customerId,
          date,
          status: clean(occurrence.status),
          placedOrderId: clean(occurrence.placedOrderId),
          confirmNotifiedAt: Number(occurrence.confirmNotifiedAt || 0),
          deliveryReminderNotifiedAt: Number(occurrence.deliveryReminderNotifiedAt || 0),
          notifiedAt: Number(occurrence.notifiedAt || 0),
        },
      },
      { upsert: true }
    );
  }

  const [savedOrders, savedOccurrences] = await Promise.all([
    RecurringOrder.find({ customerId }).sort({ createdAt: -1 }).lean(),
    RecurringOrderOccurrence.find({ customerId }).lean(),
  ]);

  res.json({
    orders: savedOrders.map(formatOrder),
    occurrences: savedOccurrences.map(formatOccurrence),
  });
}));

router.post('/notifications', asyncHandler(async (req, res) => {
  const customerId = clean(req.body.customerId);
  const recurringOrderId = clean(req.body.recurringOrderId);
  const occurrenceDate = clean(req.body.occurrenceDate);
  const type = clean(req.body.type);
  const title = clean(req.body.title);
  const body = clean(req.body.body);
  const action = clean(req.body.action);
  const outsideApp = Boolean(req.body.outsideApp);

  if (!customerId || !recurringOrderId || !occurrenceDate || !type || !title) {
    return res.status(400).json({ message: 'Thiếu thông tin thông báo' });
  }

  const existing = await notificationsCollection().findOne({
    CustomerID: customerId,
    recurringOrderId,
    occurrenceDate,
    type,
    outsideApp,
  });

  if (existing) {
    await notificationsCollection().updateOne(
      { _id: existing._id },
      {
        $set: {
          title,
          body,
          message: body,
          action,
          isRead: false,
          updatedAt: new Date(),
        },
      }
    );
    const updated = await notificationsCollection().findOne({ _id: existing._id });
    return res.json(formatNotification(updated));
  }

  const now = new Date();
  const insertResult = await notificationsCollection().insertOne({
    CustomerID: customerId,
    category: 'orders',
    type,
    targetType: 'recurring_order',
    targetId: `${recurringOrderId}|${occurrenceDate}`,
    recurringOrderId,
    occurrenceDate,
    title,
    body,
    message: body,
    action,
    outsideApp,
    isRead: false,
    createdAt: now,
    updatedAt: now,
  });

  const saved = await notificationsCollection().findOne({ _id: insertResult.insertedId });
  res.status(201).json(formatNotification(saved));
}));

router.patch('/notifications/:notificationId/read', asyncHandler(async (req, res) => {
  const { notificationId } = req.params;
  if (!mongoose.Types.ObjectId.isValid(notificationId)) {
    return res.status(400).json({ message: 'notificationId không hợp lệ' });
  }

  const result = await notificationsCollection().findOneAndUpdate(
    { _id: new mongoose.Types.ObjectId(notificationId) },
    { $set: { isRead: true, readAt: new Date() } },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ message: 'Không tìm thấy thông báo' });
  }

  res.json(formatNotification(result));
}));

router.patch('/notifications/customer/:customerId/read-all', asyncHandler(async (req, res) => {
  const customerId = clean(req.params.customerId);
  const result = await notificationsCollection().updateMany(
    {
      CustomerID: customerId,
      type: { $in: ['recurring_confirm', 'recurring_delivery', 'recurring_skipped'] },
      isRead: { $ne: true },
    },
    { $set: { isRead: true, readAt: new Date() } }
  );
  res.json({ success: true, modifiedCount: result.modifiedCount || 0 });
}));

module.exports = router;
