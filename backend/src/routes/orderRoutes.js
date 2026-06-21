const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');
const { evaluateCustomerCertificate, isDeliveredStatus } = require('../services/certificateService');

const router = express.Router();

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

  // Fetch items and shipping info from order_details
  const orderDetail = await mongoose.connection.db.collection('order_details').findOne({ OrderID: order.OrderID });
  if (orderDetail) {
    order.items = orderDetail.items || [];
    order.shippingInfo = orderDetail.shippingInfo || {};
    order.promotion_id = orderDetail.promotion_id || null;
  } else {
    order.items = [];
    order.shippingInfo = {};
  }

  res.json(order);
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
      subtotal: order.subtotal || 0,
      shippingFee: order.shippingFee || 0,
      shippingDiscount: order.shippingDiscount || 0,
      discount: order.discount || 0,
      total: order.totalAmount || 0,
      status: order.status || 'pending',
      shippingAddress,
      warehouseId: shippingInfo.warehouse_id || '',
      items,
      createdAt,
    };
  });

  res.json(result);
}));

// Create order
router.post('/', asyncHandler(async (req, res) => {
  const newOrder = { ...req.body, createdAt: new Date(), updatedAt: new Date() };
  const result = await mongoose.connection.db.collection('orders').insertOne(newOrder);
  res.status(201).json({ _id: result.insertedId, ...newOrder });
}));

/**
 * PATCH /api/orders/:orderId/status
 * Cập nhật trạng thái đơn hàng (by OrderID field, e.g. ORD...)
 */
router.patch('/:orderId/status', asyncHandler(async (req, res) => {
  const { orderId } = req.params;
  const { status } = req.body;

  if (!status) {
    return res.status(400).json({ message: 'status is required' });
  }

  const db = mongoose.connection.db;
  const result = await db.collection('orders').findOneAndUpdate(
    { OrderID: orderId },
    {
      $set: {
        status,
        updatedAt: new Date(),
        [`routes.${status}`]: new Date(),
      }
    },
    { returnDocument: 'after' }
  );

  if (!result) {
    return res.status(404).json({ message: 'Order not found' });
  }

  if (status && isDeliveredStatus(status)) {
    try {
      await evaluateCustomerCertificate(result.CustomerID, result.OrderID);
    } catch (error) {
      console.error('[certificate evaluation] Failed:', error);
    }
  }

  res.json({ orderId, status, message: 'Order status updated' });
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

  // If status changed, update the dates under the routes field
  if (updateData.status) {
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
  if (updatedOrder?.CustomerID && updateData.status && isDeliveredStatus(updateData.status)) {
    try {
      await evaluateCustomerCertificate(updatedOrder.CustomerID, updatedOrder.OrderID);
    } catch (error) {
      console.error('[certificate evaluation] Failed:', error);
    }
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
