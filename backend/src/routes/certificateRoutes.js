const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');
const {
  getCertificates,
  evaluateCustomerCertificate,
  evaluateAllDeliveredCustomers
} = require('../services/certificateService');

const router = express.Router();

const collection = () => mongoose.connection.db.collection('certificate_requests');

const mapRequest = (request) => ({
  ...request,
  id: request.requestId || request._id?.toString(),
  userId: request.CustomerID,
  requestedCer: request.requestedCertificateName || request.requestedCertificateID,
  totalPoints: request.carbonPointSnapshot || 0
});

router.get('/', asyncHandler(async (_req, res) => {
  const certificates = await getCertificates();
  res.json({ success: true, data: certificates });
}));

router.get('/requests', asyncHandler(async (req, res) => {
  const status = req.query.status || 'pending';
  const query = status === 'all' ? {} : { status };
  const rows = await collection().find(query).sort({ createdAt: -1 }).toArray();
  res.json({ success: true, data: rows.map(mapRequest) });
}));

router.get('/requests/customer/:customerId/latest', asyncHandler(async (req, res) => {
  const request = await collection()
    .find({ CustomerID: req.params.customerId })
    .sort({ updatedAt: -1, createdAt: -1 })
    .limit(1)
    .next();
  res.json({ success: true, data: request ? mapRequest(request) : null });
}));

router.post('/requests/evaluate/:customerId', asyncHandler(async (req, res) => {
  const result = await evaluateCustomerCertificate(req.params.customerId, req.body?.sourceOrderID || null);
  res.json(result);
}));

router.post('/requests/evaluate-all', asyncHandler(async (_req, res) => {
  const results = await evaluateAllDeliveredCustomers();
  res.json({
    success: true,
    evaluated: results.length,
    created: results.filter((result) => result.created).length,
    data: results
  });
}));

router.put('/requests/:id/approve', asyncHandler(async (req, res) => {
  const query = { $or: [{ requestId: req.params.id }] };
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query.$or.push({ _id: new mongoose.Types.ObjectId(req.params.id) });
  }

  const request = await collection().findOne(query);
  if (!request) {
    return res.status(404).json({ success: false, message: 'Certificate request not found' });
  }

  await mongoose.connection.db.collection('users').updateOne(
    { CustomerID: request.CustomerID },
    {
      $set: {
        CertificateID: request.requestedCertificateID,
        CertificateGrantedAt: new Date()
      }
    }
  );

  await collection().updateOne(query, {
    $set: {
      status: 'approved',
      reviewedAt: new Date(),
      reviewedBy: req.body?.reviewedBy || 'admin',
      updatedAt: new Date()
    }
  });

  const updated = await collection().findOne(query);
  res.json({ success: true, data: mapRequest(updated) });
}));

router.put('/requests/:id/reject', asyncHandler(async (req, res) => {
  const query = { $or: [{ requestId: req.params.id }] };
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query.$or.push({ _id: new mongoose.Types.ObjectId(req.params.id) });
  }

  const request = await collection().findOne(query);
  if (!request) {
    return res.status(404).json({ success: false, message: 'Certificate request not found' });
  }

  await collection().updateOne(query, {
    $set: {
      status: 'rejected',
      rejectReason: req.body?.reason || '',
      reviewedAt: new Date(),
      reviewedBy: req.body?.reviewedBy || 'admin',
      updatedAt: new Date()
    }
  });

  const updated = await collection().findOne(query);
  res.json({ success: true, data: mapRequest(updated) });
}));

module.exports = router;
