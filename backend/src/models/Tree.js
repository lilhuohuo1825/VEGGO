const mongoose = require('mongoose');

const treeSchema = new mongoose.Schema(
  {
    customerId: { type: String, required: true, unique: true, index: true },
    status: { type: String, enum: ['none', 'seed', 'growing', 'mature'], default: 'none' },
    waterCount: { type: Number, default: 0 },
    // Total watering count across all seed cycles. Used to compute redeemed CarbonPoints reliably.
    totalWaterCount: { type: Number, default: 0 },
    treeName: { type: String, default: 'Cây Veggo Xanh' },
    plantedCount: { type: Number, default: 0 },
    // Bump when tree/carbon data is reset for ledger-based carbon sync.
    carbonSyncVersion: { type: Number, default: 0 }
  },
  {
    timestamps: true,
    collection: 'trees'
  }
);

module.exports = mongoose.model('Tree', treeSchema);
