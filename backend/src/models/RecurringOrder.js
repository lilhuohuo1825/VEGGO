const mongoose = require('mongoose');

const recurringProductSchema = new mongoose.Schema(
  {
    productId: { type: String, default: '' },
    sku: { type: String, default: '' },
    name: { type: String, default: '' },
    imageUrl: { type: String, default: '' },
    unit: { type: String, default: '' },
    quantity: { type: Number, default: 1 },
    selectedWeight: { type: Number, default: 1 },
    unitPrice: { type: Number, default: 0 },
    baseUnitPrice: { type: Number, default: 0 },
    baseCarbonSavingPoint: { type: Number, default: 0 },
    emissionFactor: { type: Number, default: 0 },
    hasWeightOptions: { type: Boolean, default: false },
    carbonPoints: { type: Number, default: 0 },
  },
  { _id: false }
);

const recurringOrderSchema = new mongoose.Schema(
  {
    recurringId: { type: String, required: true, unique: true, index: true },
    customerId: { type: String, required: true, index: true },
    name: { type: String, default: '' },
    frequency: { type: String, default: '' },
    deliveryDate: { type: String, default: '' },
    deliverySlot: { type: String, default: '' },
    receiverName: { type: String, default: '' },
    receiverPhone: { type: String, default: '' },
    city: { type: String, default: '' },
    district: { type: String, default: '' },
    ward: { type: String, default: '' },
    detailAddress: { type: String, default: '' },
    itemSummary: { type: String, default: '' },
    estimatedTotal: { type: Number, default: 0 },
    carbonPoints: { type: Number, default: 0 },
    items: { type: [recurringProductSchema], default: [] },
    status: { type: String, default: 'Active' },
    createdAt: { type: String, default: '' },
  },
  {
    timestamps: true,
    collection: 'recurring_orders',
  }
);

module.exports = mongoose.model('RecurringOrder', recurringOrderSchema);
