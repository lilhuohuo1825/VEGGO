const mongoose = require('mongoose');

const instructionSchema = new mongoose.Schema(
  {
    title: { type: String, trim: true },
    image: { type: String, default: '' },
    description: { type: String, default: '' },
    // Legacy asset fields (instructions.json)
    ID: { type: String, index: true },
    DishName: { type: String, trim: true },
    Image: { type: String, default: '' },
    Ingredient: { type: String, default: '' },
    CookingTime: { type: String, default: '' },
    Difficulty: { type: String, default: '' },
    Servings: { type: String, default: '' },
  },
  { timestamps: false, strict: false }
);

instructionSchema.index({ title: 'text', DishName: 'text' });

module.exports = mongoose.model('Instruction', instructionSchema, 'instructions');
