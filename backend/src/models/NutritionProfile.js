const mongoose = require('mongoose');

const macroSchema = new mongoose.Schema(
  {
    calories: { type: Number, default: 0, min: 0 },
    protein: { type: Number, default: 0, min: 0 },
    carbs: { type: Number, default: 0, min: 0 },
    fat: { type: Number, default: 0, min: 0 },
    fiber: { type: Number, default: 0, min: 0 },
    sodium: { type: Number, default: 0, min: 0 },
    sugar: { type: Number, default: 0, min: 0 },
  },
  { _id: false }
);

const nutritionProfileSchema = new mongoose.Schema(
  {
    sku: { type: String, sparse: true, index: true },
    foodName: { type: String, required: true, trim: true, index: true },
    foodNameNormalized: { type: String, trim: true, index: true },
    per100g: { type: macroSchema, default: () => ({}) },
    vitamins: { type: mongoose.Schema.Types.Mixed, default: {} },
    minerals: { type: mongoose.Schema.Types.Mixed, default: {} },
    unit: { type: String, default: '100g' },
    source: { type: String, default: 'database' },
    isActive: { type: Boolean, default: true },
  },
  { timestamps: true, collection: 'nutrition_profiles' }
);

nutritionProfileSchema.pre('validate', function normalizeFoodName(next) {
  if (this.foodName) {
    this.foodNameNormalized = String(this.foodName).toLowerCase().trim();
  }
  next();
});

module.exports = mongoose.model('NutritionProfile', nutritionProfileSchema);
