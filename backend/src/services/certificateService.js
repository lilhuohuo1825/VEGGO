const mongoose = require('mongoose');
const { getReviewCarbonPoints } = require('../utils/reviewCarbonRules');

const DEFAULT_CERTIFICATES = [
  {
    CertificateID: 'CER001',
    CertificateName: 'Green Starter',
    RequiredCarbonPoint: 100,
    CertificateDescription: 'Chứng nhận dành cho người dùng bắt đầu hành trình tiêu dùng xanh và lựa chọn các sản phẩm thân thiện với môi trường.',
    RewardDescription: 'Voucher giảm giá 5% cho đơn hàng tiếp theo',
    Status: true
  },
  {
    CertificateID: 'CER002',
    CertificateName: 'Eco Shopper',
    RequiredCarbonPoint: 300,
    CertificateDescription: 'Chứng nhận dành cho người dùng thường xuyên mua sản phẩm xanh và góp phần giảm lượng phát thải carbon.',
    RewardDescription: 'Miễn phí vận chuyển cho đơn hàng từ mức quy định',
    Status: true
  },
  {
    CertificateID: 'CER003',
    CertificateName: 'Carbon Saver',
    RequiredCarbonPoint: 700,
    CertificateDescription: 'Chứng nhận dành cho người dùng có mức đóng góp nổi bật trong việc giảm phát thải carbon thông qua hành vi mua sắm bền vững.',
    RewardDescription: 'Voucher giảm giá 10%',
    Status: true
  },
  {
    CertificateID: 'CER004',
    CertificateName: 'Green Hero',
    RequiredCarbonPoint: 1200,
    CertificateDescription: 'Chứng nhận dành cho người dùng tích cực lan tỏa lối sống xanh và duy trì mức tiêu dùng bền vững lâu dài.',
    RewardDescription: 'Ưu đãi thành viên xanh đặc biệt',
    Status: true
  },
  {
    CertificateID: 'CER005',
    CertificateName: 'Earth Guardian',
    RequiredCarbonPoint: 2000,
    CertificateDescription: 'Chứng nhận cao nhất dành cho người dùng có đóng góp xuất sắc trong việc thúc đẩy tiêu dùng xanh và bảo vệ môi trường.',
    RewardDescription: 'Quyền lợi VIP và ưu đãi độc quyền',
    Status: true
  }
];

const COMPLETED_ORDER_STATUSES = ['completed', 'unreview', 'reviewed'];
const CUSTOMER_TIERS = [
  { minSpent: 20000000, customerTiering: 'Vàng', customerType: 'VIP' },
  { minSpent: 5000000, customerTiering: 'Bạc', customerType: 'Premium' },
  { minSpent: 0, customerTiering: 'Đồng', customerType: 'Regular' }
];

const db = () => mongoose.connection.db;

const toNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};

const normalizeKey = (value) => String(value || '').trim().toLowerCase();

const isDeliveredStatus = (status) => COMPLETED_ORDER_STATUSES.includes(normalizeKey(status));

const resolveCustomerTier = (totalSpent) => (
  CUSTOMER_TIERS.find((tier) => totalSpent >= tier.minSpent) || CUSTOMER_TIERS[CUSTOMER_TIERS.length - 1]
);

const parseWeightKg = (value) => {
  if (value === null || value === undefined || value === '') return 0;
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0;

  const raw = String(value).trim().toLowerCase().replace(',', '.');
  const match = raw.match(/(\d+(?:\.\d+)?)/);
  if (!match) return 0;

  const amount = Number(match[1]);
  if (!Number.isFinite(amount)) return 0;
  if (raw.includes('mg')) return amount / 1000000;
  if (raw.includes('g') && !raw.includes('kg')) return amount / 1000;
  return amount;
};

const getItemQuantity = (item) => {
  const quantity = toNumber(item.quantity ?? item.Quantity ?? item.qty ?? item.Qty, 1);
  return quantity > 0 ? quantity : 1;
};

const getProductKeyCandidates = (item) => {
  return [
    item.sku,
    item.SKU,
    item.productSku,
    item.product_sku,
    item.productId,
    item.product_id,
    item.ProductID,
    item._id,
    item.id,
    item.name,
    item.product_name,
    item.productName
  ].filter(Boolean).map(normalizeKey);
};

const buildProductLookup = (products) => {
  const lookup = new Map();
  products.forEach((product) => {
    [
      product.sku,
      product.SKU,
      product._id?.toString?.(),
      product.id,
      product.ProductID,
      product.product_name,
      product.name
    ].filter(Boolean).forEach((key) => {
      lookup.set(normalizeKey(key), product);
    });
  });
  return lookup;
};

const resolveProduct = (item, productLookup) => {
  for (const key of getProductKeyCandidates(item)) {
    if (productLookup.has(key)) return productLookup.get(key);
  }
  return null;
};

const getEmissionFactor = (item, product) => {
  return toNumber(
    item.EmissionFactor ??
      item.emissionFactor ??
      item.emission_factor ??
      product?.EmissionFactor ??
      product?.emissionFactor ??
      product?.emission_factor,
    0
  );
};

const getItemKg = (item, product) => {
  const explicitKg = toNumber(
    item.kg_nong_san ??
      item.kgNongSan ??
      item.weightKg ??
      item.WeightKg ??
      item.kg ??
      item.Kg,
    0
  );
  if (explicitKg > 0) return explicitKg;

  const quantity = getItemQuantity(item);
  const perUnitKg =
    parseWeightKg(item.selectedWeight) ||
    parseWeightKg(item.weight) ||
    parseWeightKg(item.Weight) ||
    parseWeightKg(item.unit) ||
    parseWeightKg(item.Unit) ||
    parseWeightKg(product?.weight) ||
    parseWeightKg(product?.Weight);
  if (perUnitKg > 0) return perUnitKg * quantity;

  const unit = normalizeKey(item.unit || item.Unit || product?.unit || product?.Unit);
  if (unit.includes('kg')) return quantity;
  if (unit === 'g' || unit.includes('gram')) return quantity / 1000;

  return 0;
};

const calculateItemCarbon = (item, product) => {
  const emissionFactor = getEmissionFactor(item, product);
  const kg = getItemKg(item, product);
  if (emissionFactor <= 0 || kg <= 0) {
    const submittedEmission = toNumber(item.TotalCarbonEmission ?? item.totalCarbonEmission, 0);
    const submittedPoint = toNumber(item.CarbonPointEarned ?? item.carbonPointEarned, 0);
    if (submittedEmission > 0 || submittedPoint > 0) {
      return {
        kg,
        emissionFactor,
        carbonEmission: submittedEmission > 0 ? submittedEmission : submittedPoint / 10,
        carbonPoint: submittedPoint > 0 ? submittedPoint : Math.round(submittedEmission * 10)
      };
    }
    return {
      kg,
      emissionFactor,
      carbonEmission: 0,
      carbonPoint: 0
    };
  }

  const carbonEmission = kg * emissionFactor;
  return {
    kg,
    emissionFactor,
    carbonEmission,
    carbonPoint: Math.round(carbonEmission * 10)
  };
};

async function getCertificates() {
  const rows = await db().collection('certificates')
    .find({ Status: { $ne: false } })
    .sort({ RequiredCarbonPoint: 1 })
    .toArray();

  return rows.length ? rows : DEFAULT_CERTIFICATES;
}

const certificateRank = (certificate, certificates) => {
  if (!certificate) return 0;
  const id = certificate.CertificateID || certificate;
  const found = certificates.find((item) => item.CertificateID === id);
  return found ? toNumber(found.RequiredCarbonPoint, 0) : 0;
};

const findEligibleCertificate = (points, certificates) => {
  let eligible = null;
  certificates.forEach((certificate) => {
    if (points >= toNumber(certificate.RequiredCarbonPoint, 0)) {
      eligible = certificate;
    }
  });
  return eligible;
};

const getNextRequestId = async () => {
  const latest = await db().collection('certificate_requests')
    .find({ requestId: /^CERTREQ\d+$/ })
    .project({ requestId: 1 })
    .toArray();

  const maxNumber = latest.reduce((max, row) => {
    const match = String(row.requestId || '').match(/^CERTREQ(\d+)$/);
    return match ? Math.max(max, Number(match[1])) : max;
  }, 0);

  return `CERTREQ${String(maxNumber + 1).padStart(6, '0')}`;
};

async function createCertificateEligibilityNotification(customerId, request) {
  if (!customerId || !request?.requestId) return;

  await db().collection('notifications').updateOne(
    {
      CustomerID: customerId,
      type: 'certificate_eligible',
      requestId: request.requestId
    },
    {
      $setOnInsert: {
        CustomerID: customerId,
        category: 'other',
        type: 'certificate_eligible',
        title: 'Bạn đủ điều kiện nhận chứng nhận',
        body: `Bạn đã đạt ${request.carbonPointSnapshot} C, đủ mốc ${request.requestedCertificateName}. Vui lòng chờ quản trị viên duyệt.`,
        action: 'Xem chứng nhận',
        iconText: '✓',
        targetType: 'certificate',
        targetId: request.requestedCertificateID,
        requestId: request.requestId,
        isRead: false,
        createdAt: new Date()
      }
    },
    { upsert: true }
  );
}

async function recalculateCustomerSpending(customerId) {
  const orders = await db().collection('orders')
    .find({ CustomerID: customerId, status: { $in: COMPLETED_ORDER_STATUSES } })
    .toArray();

  const totalSpent = orders.reduce((sum, order) => (
    sum + toNumber(order.totalAmount ?? order.total ?? order.subtotal, 0)
  ), 0);
  const tier = resolveCustomerTier(totalSpent);

  await db().collection('users').updateOne(
    { CustomerID: customerId },
    {
      $set: {
        TotalSpent: totalSpent,
        CustomerTiering: tier.customerTiering,
        CustomerType: tier.customerType,
        TotalSpentUpdatedAt: new Date()
      }
    }
  );

  return {
    CustomerID: customerId,
    totalSpent,
    completedOrderCount: orders.length,
    CustomerTiering: tier.customerTiering,
    CustomerType: tier.customerType
  };
}

async function getRedeemedCarbonFromLedger(customerId) {
  if (!customerId) return 0;
  try {
    const txs = await db().collection('wallet_transactions')
      .find({ customerId, type: 'carbon_watering', status: 'completed' })
      .toArray();
    return txs.reduce((sum, tx) => {
      const pts = toNumber(tx.carbonPoints, 0);
      return sum + Math.abs(pts);
    }, 0);
  } catch (e) {
    return 0;
  }
}

async function recalculateCustomerCarbon(customerId) {
  const orders = await db().collection('orders')
    .find({ CustomerID: customerId, status: { $in: COMPLETED_ORDER_STATUSES } })
    .toArray();

  const orderIds = orders.map((order) => order.OrderID).filter(Boolean);
  const details = orderIds.length
    ? await db().collection('order_details').find({ OrderID: { $in: orderIds } }).toArray()
    : [];
  const detailMap = new Map(details.map((detail) => [detail.OrderID, detail]));

  const skuSet = new Set();
  for (const order of orders) {
    const detail = detailMap.get(order.OrderID) || {};
    const items = Array.isArray(detail.items) ? detail.items : Array.isArray(order.items) ? order.items : [];
    items.forEach((item) => {
      const sku = String(item.sku || item.SKU || '').trim();
      if (sku) skuSet.add(sku);
    });
  }

  const products = skuSet.size
    ? await db().collection('products').find({ sku: { $in: [...skuSet] } }).toArray()
    : [];
  const productLookup = buildProductLookup(products);
  const reviewDocs = await db().collection('reviews')
    .find({ 'reviews.customer_id': customerId })
    .toArray();
  const reviewedReviewByOrder = new Map();
  reviewDocs.forEach((doc) => {
    const sku = String(doc.sku || '').trim();
    if (!sku || !Array.isArray(doc.reviews)) return;
    doc.reviews.forEach((review) => {
      if (String(review.customer_id || '').trim() !== customerId) return;
      const orderId = String(review.order_id || '').trim();
      if (!orderId) return;
      if (!reviewedReviewByOrder.has(orderId)) {
        reviewedReviewByOrder.set(orderId, new Map());
      }
      reviewedReviewByOrder.get(orderId).set(sku, review);
    });
  });

  let totalCarbonEmission = 0;
  let totalCarbonPoint = 0;
  const orderSummaries = [];

  for (const order of orders) {
    const detail = detailMap.get(order.OrderID) || {};
    const items = Array.isArray(detail.items) ? detail.items : Array.isArray(order.items) ? order.items : [];
    const orderReviews = reviewedReviewByOrder.get(order.OrderID) || new Map();

    let orderCarbonEmission = 0;
    let orderCarbonPoint = 0;
    const calculatedItems = items.map((item) => {
      const product = resolveProduct(item, productLookup);
      const calculation = calculateItemCarbon(item, product);
      const sku = String(item.sku || item.SKU || product?.sku || '').trim();
      const reviewForSku = orderReviews.get(sku);
      const reviewBonusPoint = reviewForSku ? getReviewCarbonPoints(reviewForSku) : 0;
      orderCarbonEmission += calculation.carbonEmission;
      orderCarbonPoint += calculation.carbonPoint + reviewBonusPoint;

      return {
        ...item,
        EmissionFactor: calculation.emissionFactor || item.EmissionFactor,
        CarbonKg: Number(calculation.kg.toFixed(3)),
        TotalCarbonEmission: Number(calculation.carbonEmission.toFixed(3)),
        CarbonPointEarned: calculation.carbonPoint,
        ReviewCarbonPointEarned: reviewBonusPoint
      };
    });

    totalCarbonEmission += orderCarbonEmission;
    totalCarbonPoint += orderCarbonPoint;

    if (detail.OrderID) {
      await db().collection('order_details').updateOne(
        { OrderID: order.OrderID },
        {
          $set: {
            items: calculatedItems,
            TotalCarbonEmission: Number(orderCarbonEmission.toFixed(3)),
            CarbonPointEarned: orderCarbonPoint,
            updated_at: new Date()
          }
        }
      );
    }

    orderSummaries.push({
      OrderID: order.OrderID,
      TotalCarbonEmission: Number(orderCarbonEmission.toFixed(3)),
      CarbonPointEarned: orderCarbonPoint
    });
  }

  // Redeemed carbon points from ledger (watering tree in VeggoPay donate).
  // IMPORTANT: use wallet_transactions, not tree.totalWaterCount, so balance stays
  // consistent even when history was missing or watering cost changed.
  const redeemedCarbonPoint = await getRedeemedCarbonFromLedger(customerId);

  const netCarbonPoint = Math.max(0, Math.round(totalCarbonPoint - redeemedCarbonPoint));

  await db().collection('users').updateOne(
    { CustomerID: customerId },
    {
      $set: {
        CarbonPoint: netCarbonPoint,
        TotalCarbonEmission: Number(totalCarbonEmission.toFixed(3)),
        CarbonPointUpdatedAt: new Date(),
        CarbonPointRedeemed: redeemedCarbonPoint
      }
    }
  );

  return {
    CustomerID: customerId,
    totalCarbonEmission: Number(totalCarbonEmission.toFixed(3)),
    totalCarbonPoint: netCarbonPoint,
    redeemedCarbonPoint,
    orders: orderSummaries
  };
}

async function evaluateCustomerCertificate(customerId, sourceOrderId = null) {
  const user = await db().collection('users').findOne({ CustomerID: customerId });
  if (!user) {
    return { success: false, message: 'User not found', CustomerID: customerId };
  }

  const spendingSummary = await recalculateCustomerSpending(customerId);
  const carbonSummary = await recalculateCustomerCarbon(customerId);
  const certificates = await getCertificates();
  const eligible = findEligibleCertificate(carbonSummary.totalCarbonPoint, certificates);
  if (!eligible) {
    return {
      success: true,
      created: false,
      reason: 'not_enough_points',
      ...spendingSummary,
      ...carbonSummary
    };
  }

  const currentRank = certificateRank(user.CertificateID, certificates);
  const eligibleRank = certificateRank(eligible, certificates);
  if (currentRank >= eligibleRank) {
    return {
      success: true,
      created: false,
      reason: 'already_has_certificate',
      requestedCertificateID: eligible.CertificateID,
      ...spendingSummary,
      ...carbonSummary
    };
  }

  const pending = await db().collection('certificate_requests').findOne({
    CustomerID: customerId,
    requestedCertificateID: eligible.CertificateID,
    status: 'pending'
  });
  if (pending) {
    return {
      success: true,
      created: false,
      reason: 'pending_exists',
      request: pending,
      ...spendingSummary,
      ...carbonSummary
    };
  }

  const rejected = await db().collection('certificate_requests').findOne(
    {
      CustomerID: customerId,
      requestedCertificateID: eligible.CertificateID,
      status: 'rejected',
      carbonPointSnapshot: { $gte: carbonSummary.totalCarbonPoint }
    },
    { sort: { reviewedAt: -1, updatedAt: -1, createdAt: -1 } }
  );
  if (rejected) {
    return {
      success: true,
      created: false,
      reason: 'rejected_exists',
      request: rejected,
      ...spendingSummary,
      ...carbonSummary
    };
  }

  const request = {
    requestId: await getNextRequestId(),
    CustomerID: customerId,
    currentCertificateID: user.CertificateID || null,
    requestedCertificateID: eligible.CertificateID,
    requestedCertificateName: eligible.CertificateName,
    requiredCarbonPoint: eligible.RequiredCarbonPoint,
    carbonPointSnapshot: carbonSummary.totalCarbonPoint,
    carbonEmissionSnapshot: carbonSummary.totalCarbonEmission,
    sourceOrderID: sourceOrderId || null,
    status: 'pending',
    createdAt: new Date(),
    updatedAt: new Date()
  };

  await db().collection('certificate_requests').insertOne(request);
  await createCertificateEligibilityNotification(customerId, request);

  return {
    success: true,
    created: true,
    request,
    ...spendingSummary,
    ...carbonSummary
  };
}

async function refreshCustomerOrderMetrics(customerId, sourceOrderId = null) {
  if (!customerId) {
    return { success: false, message: 'CustomerID is required' };
  }

  const user = await db().collection('users').findOne({ CustomerID: customerId });
  if (!user) {
    return { success: false, message: 'User not found', CustomerID: customerId };
  }

  const spendingSummary = await recalculateCustomerSpending(customerId);
  if (sourceOrderId) {
    return evaluateCustomerCertificate(customerId, sourceOrderId);
  }

  const carbonSummary = await recalculateCustomerCarbon(customerId);
  return {
    success: true,
    ...spendingSummary,
    ...carbonSummary
  };
}

async function evaluateAllDeliveredCustomers() {
  const customerIds = await db().collection('orders').distinct('CustomerID', {
    status: { $in: COMPLETED_ORDER_STATUSES },
    CustomerID: { $nin: [null, ''] }
  });

  const results = [];
  for (const customerId of customerIds) {
    results.push(await evaluateCustomerCertificate(customerId));
  }
  return results;
}

module.exports = {
  getCertificates,
  evaluateCustomerCertificate,
  refreshCustomerOrderMetrics,
  recalculateCustomerSpending,
  recalculateCustomerCarbon,
  getRedeemedCarbonFromLedger,
  evaluateAllDeliveredCustomers,
  isDeliveredStatus
};
