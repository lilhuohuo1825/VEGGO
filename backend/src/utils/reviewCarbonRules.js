const MIN_REVIEW_CONTENT_LENGTH = 50;
const REVIEW_EDIT_WINDOW_MS = 3 * 24 * 60 * 60 * 1000;

function parseReviewTime(review) {
  const raw = review?.time || review?.created_at || review?.createdAt;
  if (!raw) return null;
  if (raw instanceof Date) return raw;
  if (typeof raw === 'object' && raw.$date) return new Date(raw.$date);
  const parsed = new Date(raw);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function canEditReview(review, now = new Date()) {
  const created = parseReviewTime(review);
  if (!created) return false;
  return now.getTime() - created.getTime() <= REVIEW_EDIT_WINDOW_MS;
}

function getReviewEditDeadline(review) {
  const created = parseReviewTime(review);
  if (!created) return null;
  return new Date(created.getTime() + REVIEW_EDIT_WINDOW_MS);
}

function hasReviewMedia(review) {
  const images = Array.isArray(review?.images) ? review.images : [];
  return images.some((url) => String(url || '').trim().length > 0);
}

function isPremiumReview(review) {
  const rating = Number(review?.rating);
  const content = String(review?.content || '').trim();
  return Number.isFinite(rating)
    && rating >= 1
    && rating <= 5
    && content.length >= MIN_REVIEW_CONTENT_LENGTH
    && hasReviewMedia(review);
}

function getReviewCarbonPoints(review) {
  const rating = Number(review?.rating);
  if (!Number.isFinite(rating) || rating < 1 || rating > 5) {
    return 0;
  }
  return isPremiumReview(review) ? 2 : 1;
}

function sumOrderReviewCarbonPoints(reviewDocs, customerId, orderId, requiredSkus) {
  let total = 0;
  const customer = String(customerId || '').trim();
  const order = String(orderId || '').trim();
  if (!customer || !order) {
    return total;
  }

  reviewDocs.forEach((doc) => {
    const sku = String(doc.sku || '').trim();
    if (!sku || !requiredSkus.includes(sku) || !Array.isArray(doc.reviews)) {
      return;
    }
    const match = doc.reviews.find((item) =>
      String(item.customer_id || '').trim() === customer
      && String(item.order_id || '').trim() === order
    );
    if (match) {
      total += getReviewCarbonPoints(match);
    }
  });

  return total;
}

module.exports = {
  MIN_REVIEW_CONTENT_LENGTH,
  REVIEW_EDIT_WINDOW_MS,
  parseReviewTime,
  canEditReview,
  getReviewEditDeadline,
  hasReviewMedia,
  isPremiumReview,
  getReviewCarbonPoints,
  sumOrderReviewCarbonPoints,
};
