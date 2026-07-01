const express = require('express');
const mongoose = require('mongoose');
const Instruction = require('../models/Instruction');
const Dish = require('../models/Dish');
const recipeService = require('../services/recipeService');
const asyncHandler = require('../middleware/asyncHandler');
const {
  resolveInstructionDisplay,
  buildCookingSteps,
  buildUsage,
  parseIngredientsList,
} = require('../utils/recipeKeywords');

const router = express.Router();
const MAX_RELATED = 10;

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

  const { recipes } = await recipeService.findRelatedRecipesByProductName(productName, {
    limit: MAX_RELATED,
  });

  const results = recipes.map((recipe) => ({
    instructionId: recipe.instructionId,
    title: recipe.title,
    image: recipe.image,
    description: recipe.description,
    cookingTime: recipe.cookingTime,
    matchScore: recipe.coverageRatio,
  }));

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
