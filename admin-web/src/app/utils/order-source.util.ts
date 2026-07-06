export type OrderSourceType = 'recurring' | 'fast' | 'standard';

export function resolveOrderSource(shippingInfo: any): OrderSourceType {
  if (!shippingInfo) {
    return 'standard';
  }
  if (shippingInfo.isRecurring || String(shippingInfo.orderSource || '').toLowerCase() === 'recurring') {
    return 'recurring';
  }
  const method = String(shippingInfo.deliveryMethod || '').toLowerCase();
  if (method === 'fast') {
    return 'fast';
  }
  return 'standard';
}

export function getOrderSourceLabel(source: OrderSourceType | string): string {
  switch (source) {
    case 'recurring':
      return 'Định kỳ';
    case 'fast':
      return 'Giao nhanh';
    default:
      return 'Tiêu chuẩn';
  }
}

export function getOrderSourceClass(source: OrderSourceType | string): string {
  switch (source) {
    case 'recurring':
      return 'order-source-recurring';
    case 'fast':
      return 'order-source-fast';
    default:
      return 'order-source-standard';
  }
}

export function formatDeliveryWindowText(shippingInfo: any): string {
  if (!shippingInfo) {
    return '';
  }
  if (shippingInfo.deliveryTimeText) {
    return String(shippingInfo.deliveryTimeText).trim();
  }
  const start = shippingInfo.deliveryWindowStart ? new Date(shippingInfo.deliveryWindowStart) : null;
  const end = shippingInfo.deliveryWindowEnd ? new Date(shippingInfo.deliveryWindowEnd) : null;
  if (!start || Number.isNaN(start.getTime())) {
    return '';
  }
  const datePart = start.toLocaleDateString('vi-VN');
  if (!end || Number.isNaN(end.getTime())) {
    return datePart;
  }
  const startTime = start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const endTime = end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return `${datePart}, ${startTime} - ${endTime}`;
}
