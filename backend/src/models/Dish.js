const mongoose = require('mongoose');

const dishSchema = new mongoose.Schema(
  {
    instructionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Instruction', index: true },
    ingredients: { type: mongoose.Schema.Types.Mixed, default: [] },
    cookingSteps: { type: String, default: '' },
    dishName: { type: String, trim: true },
    // Legacy asset fields (dishes.json)
    ID: { type: String, index: true },
    Ingredients: { type: String, default: '' },
    Description: { type: String, default: '' },
    Preparation: { type: String, default: '' },
    Cooking: { type: String, default: '' },
    Serving: { type: String, default: '' },
    Video: { type: String, default: '' },
    Tips: { type: String, default: '' },
  },
  { timestamps: false, strict: false }
);

dishSchema.index({ ingredients: 'text', Ingredients: 'text', dishName: 'text' });

module.exports = mongoose.model('Dish', dishSchema, 'dishes');
