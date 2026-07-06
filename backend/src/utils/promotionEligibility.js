const mongoose = require('mongoose');

const getPromotionTargetGroups = (target) => {
  if (!target) return [];
  if (Array.isArray(target.target_groups) && target.target_groups.length) {
    return target.target_groups
      .map((group) => ({
        target_type: group?.target_type,
        target_ref: Array.isArray(group?.target_ref) ? group.target_ref : [],
      }))
      .filter((group) => group.target_type);
  }
  if (target.target_type) {
    return [{
      target_type: target.target_type,
      target_ref: Array.isArray(target.target_ref) ? target.target_ref : [],
    }];
  }
  return [];
};

const matchesUserTargetRefs = (refs, user) => {
  if (!refs.length) return true;

  return refs.some((ref) => {
    const value = String(ref || '').trim();
    if (value.startsWith('tier:')) {
      const tier = value.slice('tier:'.length).toLowerCase();
      const tiering = String(user.CustomerTiering || user.CustomerType || '').trim().toLowerCase();
      return (
        (tier === 'bronze' && ['đồng', 'dong', 'bronze', 'regular'].includes(tiering))
        || (tier === 'silver' && ['bạc', 'bac', 'silver', 'premium'].includes(tiering))
        || (tier === 'gold' && ['vàng', 'vang', 'gold', 'vip'].includes(tiering))
      );
    }
    if (value.startsWith('certificate:')) {
      return String(user.CertificateID || '').trim() === value.slice('certificate:'.length);
    }
    return false;
  });
};

const matchesUserPromotionTarget = (target, user) => {
  if (!target) return true;
  const userGroups = getPromotionTargetGroups(target).filter((group) => group.target_type === 'User');
  if (!userGroups.length) return true;
  if (!user) return false;
  return userGroups.every((group) => matchesUserTargetRefs(group.target_ref, user));
};

const parsePromotionDate = (value) => {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'object' && value.$date) return new Date(value.$date);
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const isPromotionCurrentlyActive = (promo, now = new Date()) => {
  const status = String(promo.status || '').trim().toLowerCase();
  if (status === 'inactive' || status === 'expired' || status === 'đã kết thúc') return false;

  const startDate = parsePromotionDate(promo.start_date);
  const endDate = parsePromotionDate(promo.end_date);
  if (startDate && startDate > now) return false;
  if (endDate && endDate < now) return false;
  return promo.isActive !== false;
};

const getPromotionUsageCount = async (promotionId, customerId) => {
  const usage = await mongoose.connection.db.collection('promotion_usages').findOne(
    { promotion_id: promotionId },
    { projection: { user_id: 1, order_id: 1 } }
  );
  const userIds = Array.isArray(usage?.user_id) ? usage.user_id : [];
  const orderIds = Array.isArray(usage?.order_id) ? usage.order_id : [];
  const userUseCount = customerId
    ? userIds.filter((id) => String(id) === String(customerId)).length
    : 0;
  return {
    totalUses: orderIds.length,
    userUses: userUseCount,
  };
};

const validatePromotionForCustomer = async (promotionId, customerId) => {
  const cleanPromotionId = String(promotionId || '').trim();
  if (!cleanPromotionId) {
    return { ok: true };
  }
  if (!customerId) {
    return { ok: false, message: 'CustomerID is required to apply promotion' };
  }

  const promo = await mongoose.connection.db.collection('promotions').findOne({
    $or: [{ promotion_id: cleanPromotionId }, { code: cleanPromotionId }],
    isActive: { $ne: false },
  });
  if (!promo) {
    return { ok: false, message: 'Promotion not found or inactive' };
  }
  if (!isPromotionCurrentlyActive(promo)) {
    return { ok: false, message: 'Promotion is not active' };
  }

  const target = await mongoose.connection.db.collection('promotion_targets').findOne({
    promotion_id: promo.promotion_id,
  });
  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
  if (!user) {
    return { ok: false, message: 'User not found' };
  }
  if (!matchesUserPromotionTarget(target, user)) {
    return { ok: false, message: 'Promotion is not available for this account' };
  }

  const usage = await getPromotionUsageCount(promo.promotion_id, customerId);
  if (promo.usage_limit > 0 && usage.totalUses >= promo.usage_limit) {
    return { ok: false, message: 'Promotion usage limit reached' };
  }
  if (promo.user_limit > 0 && usage.userUses >= promo.user_limit) {
    return { ok: false, message: 'You have already used this promotion' };
  }

  return { ok: true, promotion: promo };
};

module.exports = {
  getPromotionTargetGroups,
  matchesUserTargetRefs,
  matchesUserPromotionTarget,
  validatePromotionForCustomer,
};
