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

// Get orders by CustomerID
router.get('/:userId', asyncHandler(async (req, res) => {
  const orders = await mongoose.connection.db
    .collection('orders')
    .find({ CustomerID: req.params.userId, status: { $ne: 'deleted' } })
    .sort({ createdAt: -1 })
    .toArray();
  res.json(await attachOrderDetails(orders));
}));

// Create order
router.post('/', asyncHandler(async (req, res) => {
  const newOrder = { ...req.body, createdAt: new Date(), updatedAt: new Date() };
  const result = await mongoose.connection.db.collection('orders').insertOne(newOrder);
  res.status(201).json({ _id: result.insertedId, ...newOrder });
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
