const Instruction = require('../models/Instruction');
const Dish = require('../models/Dish');
const productService = require('./productService');
const { extractKeywords } = require('../utils/recipeKeywords');
const {
  collectKeywordsFromProducts,
  collectKeywordsFromNames,
  rankRecipes,
  rankRelatedRecipes,
} = require('../utils/recipeHelpers');
const {
  resolveInstructionDisplay,
  buildCookingSteps,
  buildUsage,
  parseIngredientsList,
} = require('../utils/recipeKeywords');

const DEFAULT_LIMIT = 8;

/**
 * RecipeService – gợi ý công thức dựa trên nguyên liệu hiện có.
 * Chỉ truy vấn MongoDB, không dùng AI.
 */
async function findRecipesFromProducts(products = [], options = {}) {
  const limit = Math.max(1, Math.min(Number(options.limit) || DEFAULT_LIMIT, 20));
  const availableKeywords = collectKeywordsFromProducts(products);

  if (!availableKeywords.length) {
    return {
      products,
      availableKeywords: [],
      recipes: [],
    };
  }

  const [dishes, instructions] = await Promise.all([
    Dish.find({}).lean(),
    Instruction.find({}).lean(),
  ]);

  const recipes = rankRecipes(dishes, instructions, availableKeywords, limit);

  return {
    products,
    availableKeywords,
    recipes,
  };
}

async function findRecipesFromCart(customerId, options = {}) {
  const products = await productService.getProductsFromCart(customerId);
  return findRecipesFromProducts(products, options);
}

async function findRecipesFromProductList(products = [], options = {}) {
  return findRecipesFromProducts(products, options);
}

async function findRecipesFromProductNames(names = [], options = {}) {
  const availableKeywords = collectKeywordsFromNames(names);

  if (!availableKeywords.length) {
    return {
      products: [],
      availableKeywords: [],
      recipes: [],
    };
  }

  const [dishes, instructions] = await Promise.all([
    Dish.find({}).lean(),
    Instruction.find({}).lean(),
  ]);

  const recipes = rankRecipes(
    dishes,
    instructions,
    availableKeywords,
    Math.max(1, Math.min(Number(options.limit) || DEFAULT_LIMIT, 20))
  );

  return {
    products: [],
    availableKeywords,
    recipes,
  };
}

async function findRelatedRecipesByProductName(productName, options = {}) {
  const limit = Math.max(1, Math.min(Number(options.limit) || DEFAULT_LIMIT, 20));
  const normalizedName = String(productName || '').trim();

  if (!normalizedName) {
    return {
      productName: '',
      keywords: [],
      recipes: [],
    };
  }

  const [dishes, instructions] = await Promise.all([
    Dish.find({}).lean(),
    Instruction.find({}).lean(),
  ]);

  const { keywords, recipes } = rankRelatedRecipes(dishes, instructions, normalizedName, limit);

  return {
    productName: normalizedName,
    keywords,
    recipes,
  };
}

async function searchRecipes(query, options = {}) {
  const result = await findRelatedRecipesByProductName(query, options);
  return {
    products: [],
    availableKeywords: result.keywords,
    recipes: result.recipes,
  };
}

async function getRecipeDetail(instructionId) {
  const instruction = await Instruction.findById(instructionId).lean()
    || await Instruction.findOne({ ID: instructionId }).lean();

  if (!instruction) {
    return null;
  }

  const dishes = await Dish.find({
    $or: [
      { instructionId: instruction._id },
      ...(instruction.ID ? [{ ID: instruction.ID }] : []),
    ],
  }).lean();

  const dish = dishes[0] || null;
  const display = resolveInstructionDisplay(instruction, dish?.Description);

  return {
    instructionId: String(instruction._id),
    title: display.title,
    image: display.image,
    description: display.description,
    cookingTime: instruction.CookingTime || instruction.cookingTime || '',
    difficulty: instruction.Difficulty || '',
    servings: instruction.Servings || '',
    ingredients: dish ? parseIngredientsList(dish) : [],
    steps: dish ? buildCookingSteps(dish) : '',
    usage: dish ? buildUsage(dish) : '',
  };
}

module.exports = {
  findRecipesFromProducts,
  findRecipesFromCart,
  findRecipesFromProductList,
  findRecipesFromProductNames,
  findRelatedRecipesByProductName,
  searchRecipes,
  getRecipeDetail,
  getRecipesForCartProducts: (products, options) => findRecipesFromProducts(products, options).then((r) => r.recipes),
  getRecipesForProductNames: (names, options) => findRecipesFromProductNames(names, options).then((r) => r.recipes),
};
