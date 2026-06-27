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
const notificationsCollection = () => mongoose.connection.db.collection('notifications');

const mapRequest = (request) => ({
  ...request,
  id: request.requestId || request._id?.toString(),
  userId: request.CustomerID,
  requestedCer: request.requestedCertificateName || request.requestedCertificateID,
  totalPoints: request.carbonPointSnapshot || 0
});

const matchesUserPromotionTarget = (target, user) => {
  if (!target || target.target_type !== 'User' || !user) return false;
  const refs = Array.isArray(target.target_ref) ? target.target_ref : [];
  if (!refs.length) return false;

  return refs.some((ref) => {
    const value = String(ref || '').trim();
    if (value.startsWith('tier:')) {
      const tier = value.slice('tier:'.length).toLowerCase();
      const tiering = String(user.CustomerTiering || user.CustomerType || '').trim().toLowerCase();
      return (
        (tier === 'bronze' && ['đồng', 'dong', 'bronze', 'regular'].includes(tiering)) ||
        (tier === 'silver' && ['bạc', 'bac', 'silver', 'premium'].includes(tiering)) ||
        (tier === 'gold' && ['vàng', 'vang', 'gold', 'vip'].includes(tiering))
      );
    }
    if (value.startsWith('certificate:')) {
      return String(user.CertificateID || '').trim() === value.slice('certificate:'.length);
    }
    return false;
  });
};

const notifyCertificateApproved = async (request) => {
  if (!request?.CustomerID) return;
  const certificateName = request.requestedCertificateName || request.requestedCertificateID || 'chứng nhận xanh';
  await notificationsCollection().updateOne(
    {
      CustomerID: request.CustomerID,
      type: 'certificate_approved',
      targetType: 'certificate',
      targetId: request.requestedCertificateID || request.requestId
    },
    {
      $setOnInsert: {
        CustomerID: request.CustomerID,
        OrderID: request.sourceOrderID || null,
        category: 'other',
        type: 'certificate_approved',
        title: 'Chứng nhận Carbon đã được duyệt',
        body: `Bạn đã được duyệt chứng nhận ${certificateName}. Hãy xem chứng nhận và ưu đãi đi kèm.`,
        action: 'Xem chứng nhận',
        iconText: '✓',
        targetType: 'certificate',
        targetId: request.requestedCertificateID || request.requestId,
        isRead: false,
        createdAt: new Date(),
        updatedAt: new Date()
      }
    },
    { upsert: true }
  );
};

const notifyEligiblePromotions = async (customerId) => {
  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
  if (!user) return;

  const targets = await mongoose.connection.db
    .collection('promotion_targets')
    .find({ target_type: 'User' })
    .toArray();
  const matchedPromotionIds = targets
    .filter((target) => matchesUserPromotionTarget(target, user))
    .map((target) => target.promotion_id)
    .filter(Boolean);

  if (!matchedPromotionIds.length) return;

  const now = new Date();
  const promotions = await mongoose.connection.db
    .collection('promotions')
    .find({
      promotion_id: { $in: matchedPromotionIds },
      isActive: { $ne: false },
      status: { $nin: ['Inactive', 'Expired', 'expired', 'đã kết thúc'] },
      $or: [
        { end_date: { $exists: false } },
        { end_date: null },
        { end_date: { $gte: now } }
      ]
    })
    .toArray();

  await Promise.all(promotions.map((promotion) => notificationsCollection().updateOne(
    {
      CustomerID: customerId,
      type: 'promotion_available',
      targetType: 'promotion',
      targetId: promotion.promotion_id
    },
    {
      $setOnInsert: {
        CustomerID: customerId,
        category: 'other',
        type: 'promotion_available',
        title: 'Bạn có ưu đãi mới',
        body: `Ưu đãi "${promotion.name || promotion.code || promotion.promotion_id}" hiện áp dụng cho hạng/chứng nhận của bạn.`,
        action: 'Xem ưu đãi',
        iconText: '%',
        targetType: 'promotion',
        targetId: promotion.promotion_id,
        isRead: false,
        createdAt: new Date(),
        updatedAt: new Date()
      }
    },
    { upsert: true }
  )));
};

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
        CertificateName: request.requestedCertificateName || '',
        CertificateStatus: 'approved',
        CertificateCarbonPointSnapshot: request.carbonPointSnapshot || 0,
        CertificateCarbonEmissionSnapshot: request.carbonEmissionSnapshot || 0,
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

  await notifyCertificateApproved(request);
  await notifyEligiblePromotions(request.CustomerID);

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
