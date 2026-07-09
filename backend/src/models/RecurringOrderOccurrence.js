const mongoose = require('mongoose');

const recurringOrderOccurrenceSchema = new mongoose.Schema(
  {
    recurringOrderId: { type: String, required: true, index: true },
    customerId: { type: String, required: true, index: true },
    date: { type: String, required: true },
    status: { type: String, default: '' },
    placedOrderId: { type: String, default: '' },
    confirmNotifiedAt: { type: Number, default: 0 },
    deliveryReminderNotifiedAt: { type: Number, default: 0 },
    notifiedAt: { type: Number, default: 0 },
  },
  {
    timestamps: true,
    collection: 'recurring_order_occurrences',
  }
);

recurringOrderOccurrenceSchema.index(
  { recurringOrderId: 1, date: 1 },
  { unique: true }
);

module.exports = mongoose.model('RecurringOrderOccurrence', recurringOrderOccurrenceSchema);
