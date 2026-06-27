const express = require('express');
const mongoose = require('mongoose');
const Instruction = require('../models/Instruction');
const Dish = require('../models/Dish');
const asyncHandler = require('../middleware/asyncHandler');
const {
  extractKeywords,
  scoreDishAgainstKeywords,
  normalizeMatchScore,
  getInstructionKey,
  resolveInstructionDisplay,
  buildCookingSteps,
  buildUsage,
  parseIngredientsList,
} = require('../utils/recipeKeywords');

const router = express.Router();
const MAX_RELATED = 10;

function buildInstructionLookup(instructions) {
  const byId = new Map();
  const byLegacyId = new Map();

  for (const instruction of instructions) {
    byId.set(instruction._id.toString(), instruction);
    if (instruction.ID) {
      byLegacyId.set(instruction.ID, instruction);
    }
  }

  return { byId, byLegacyId };
}

function resolveInstructionForDish(dish, lookup) {
  if (dish.instructionId) {
    const found = lookup.byId.get(String(dish.instructionId));
    if (found) return found;
  }
  if (dish.ID) {
    return lookup.byLegacyId.get(dish.ID) || null;
  }
  return null;
}

async function findInstructionByParam(instructionId) {
  if (mongoose.Types.ObjectId.isValid(instructionId)) {
    const byObjectId = await Instruction.findById(instructionId).lean();
    if (byObjectId) return byObjectId;
  }
  return Instruction.findOne({ ID: instructionId }).lean();
}

async function findDishesForInstruction(instruction) {
  const instructionId = instruction._id;
  const legacyId = instruction.ID;

  const query = {
    $or: [
      { instructionId },
      ...(legacyId ? [{ ID: legacyId }] : []),
    ],
  };

  return Dish.find(query).lean();
}

// GET /api/recipes/related?productName=...
router.get('/related', asyncHandler(async (req, res) => {
  const productName = String(req.query.productName || '').trim();
  if (!productName) {
    return res.status(400).json({ message: 'productName is required' });
  }

  const keywords = extractKeywords(productName);
  if (!keywords.length) {
    return res.json([]);
  }

  const [dishes, instructions] = await Promise.all([
    Dish.find({}).lean(),
    Instruction.find({}).lean(),
  ]);

  const lookup = buildInstructionLookup(instructions);
  const grouped = new Map();

  for (const dish of dishes) {
    const instruction = resolveInstructionForDish(dish, lookup);
    if (!instruction) continue;

    const rawScore = scoreDishAgainstKeywords(dish, keywords, instruction);
    if (rawScore <= 0) continue;

    const key = getInstructionKey(instruction);
    const entry = grouped.get(key) || {
      instruction,
      rawScore: 0,
      dishDescription: '',
    };
    entry.rawScore += rawScore;
    if (!entry.dishDescription && dish.Description) {
      entry.dishDescription = dish.Description;
    }
    grouped.set(key, entry);
  }

  const results = Array.from(grouped.values())
    .map(({ instruction, rawScore, dishDescription }) => {
      const display = resolveInstructionDisplay(instruction, dishDescription);
      return {
        instructionId: instruction._id.toString(),
        title: display.title,
        image: display.image,
        description: display.description,
        cookingTime: instruction.CookingTime || instruction.cookingTime || '',
        matchScore: normalizeMatchScore(rawScore, keywords.length),
      };
    })
    .sort((a, b) => b.matchScore - a.matchScore)
    .slice(0, MAX_RELATED);

  res.json(results);
}));

// GET /api/recipes/:instructionId
router.get('/:instructionId', asyncHandler(async (req, res) => {
  const instruction = await findInstructionByParam(req.params.instructionId);
  if (!instruction) {
    return res.status(404).json({ message: 'Recipe not found' });
  }

  const dishes = await findDishesForInstruction(instruction);
  const display = resolveInstructionDisplay(instruction, dishes[0]?.Description);

  res.json({
    instruction: {
      title: display.title,
      image: display.image,
      description: display.description,
      cookingTime: instruction.CookingTime || '',
      difficulty: instruction.Difficulty || '',
      servings: instruction.Servings || '',
      video: dishes[0]?.Video || '',
    },
    dishes: dishes.map((dish) => ({
      dishName: dish.dishName || display.title,
      description: dish.Description || dish.description || display.description || '',
      ingredients: parseIngredientsList(dish),
      steps: buildCookingSteps(dish),
      usage: buildUsage(dish),
    })),
  });
}));

module.exports = router;
