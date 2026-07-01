const {
  extractKeywords,
  normalizeText,
  ingredientsToSearchText,
  dishNameText,
  parseIngredientsList,
  resolveInstructionDisplay,
  buildCookingSteps,
  buildUsage,
  getInstructionKey,
  scoreDishAgainstKeywords,
  normalizeMatchScore,
} = require('./recipeKeywords');

function collectKeywordsFromProducts(products = []) {
  const keywords = new Set();

  for (const product of products) {
    const name = product?.name || product?.product_name || product?.productName || '';
    extractKeywords(name).forEach((keyword) => keywords.add(keyword));

    const ingredients = String(product?.ingredients || '').trim();
    if (ingredients) {
      extractKeywords(ingredients).forEach((keyword) => keywords.add(keyword));
    }
  }

  return [...keywords];
}

function collectKeywordsFromNames(names = []) {
  const keywords = new Set();

  for (const name of names) {
    extractKeywords(name).forEach((keyword) => keywords.add(keyword));
  }

  return [...keywords];
}

function buildInstructionLookup(instructions = []) {
  const byId = new Map();
  const byLegacyId = new Map();

  for (const instruction of instructions) {
    byId.set(String(instruction._id), instruction);
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

function countMatchedKeywords(recipeText, availableKeywords = []) {
  const normalizedRecipeText = normalizeText(recipeText);
  const matchedKeywords = [];

  for (const keyword of availableKeywords) {
    const normalizedKeyword = normalizeText(keyword);
    if (normalizedKeyword && normalizedRecipeText.includes(normalizedKeyword)) {
      matchedKeywords.push(keyword);
    }
  }

  return {
    matchedCount: matchedKeywords.length,
    matchedKeywords,
  };
}

function scoreRecipeAgainstAvailableProducts(dish, instruction, availableKeywords = []) {
  const ingredients = parseIngredientsList(dish);
  const ingredientsText = ingredientsToSearchText(dish);
  const nameText = dishNameText(dish, instruction);
  const recipeText = `${ingredientsText} ${nameText}`.trim();

  const ingredientMatch = countMatchedKeywords(recipeText, availableKeywords);
  const availableCount = availableKeywords.length || 1;
  const coverageRatio = ingredientMatch.matchedCount / availableCount;

  let weightedScore = 0;
  for (const keyword of ingredientMatch.matchedKeywords) {
    const normalizedKeyword = normalizeText(keyword);
    const inIngredients = normalizeText(ingredientsText).includes(normalizedKeyword);
    weightedScore += inIngredients ? 3 : 1;
  }

  return {
    matchedIngredientCount: ingredientMatch.matchedCount,
    matchedKeywords: ingredientMatch.matchedKeywords,
    coverageRatio: Math.round(coverageRatio * 1000) / 1000,
    weightedScore,
    totalIngredients: ingredients.length,
  };
}

function formatRecipeResult(instruction, dish, scoreMeta) {
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
    matchedIngredientCount: scoreMeta.matchedIngredientCount,
    matchedKeywords: scoreMeta.matchedKeywords,
    coverageRatio: scoreMeta.coverageRatio,
    matchScore: scoreMeta.weightedScore,
  };
}

function compareRecipeResults(left, right) {
  if (right.matchedIngredientCount !== left.matchedIngredientCount) {
    return right.matchedIngredientCount - left.matchedIngredientCount;
  }
  if (right.coverageRatio !== left.coverageRatio) {
    return right.coverageRatio - left.coverageRatio;
  }
  return right.matchScore - left.matchScore;
}

function buildScoreMetaFromDishKeywords(dish, instruction, keywords = []) {
  const ingredientsText = normalizeText(ingredientsToSearchText(dish));
  const nameText = normalizeText(dishNameText(dish, instruction));
  const matchedKeywords = keywords.filter((keyword) => {
    const normalizedKeyword = normalizeText(keyword);
    return ingredientsText.includes(normalizedKeyword) || nameText.includes(normalizedKeyword);
  });

  let weightedScore = 0;
  for (const keyword of matchedKeywords) {
    const normalizedKeyword = normalizeText(keyword);
    weightedScore += ingredientsText.includes(normalizedKeyword) ? 3 : 1;
  }

  return {
    matchedIngredientCount: matchedKeywords.length,
    matchedKeywords,
    coverageRatio: normalizeMatchScore(weightedScore, keywords.length || 1),
    weightedScore,
    totalIngredients: parseIngredientsList(dish).length,
  };
}

function rankRelatedRecipes(dishes, instructions, productName, limit = 10) {
  const keywords = extractKeywords(productName);
  if (!keywords.length) {
    return { keywords, recipes: [] };
  }

  const lookup = buildInstructionLookup(instructions);
  const grouped = new Map();

  for (const dish of dishes) {
    const instruction = resolveInstructionForDish(dish, lookup);
    if (!instruction) {
      continue;
    }

    const rawScore = scoreDishAgainstKeywords(dish, keywords, instruction);
    if (rawScore <= 0) {
      continue;
    }

    const key = getInstructionKey(instruction);
    const entry = grouped.get(key) || {
      instruction,
      dish,
      rawScore: 0,
      dishDescription: '',
      bestDishScore: 0,
    };

    entry.rawScore += rawScore;
    if (!entry.dishDescription && dish.Description) {
      entry.dishDescription = dish.Description;
    }
    if (rawScore >= entry.bestDishScore) {
      entry.dish = dish;
      entry.bestDishScore = rawScore;
    }
    grouped.set(key, entry);
  }

  const recipes = Array.from(grouped.values())
    .map(({ instruction, dish, rawScore, dishDescription }) => {
      const scoreMeta = buildScoreMetaFromDishKeywords(dish, instruction, keywords);
      scoreMeta.weightedScore = rawScore;
      scoreMeta.coverageRatio = normalizeMatchScore(rawScore, keywords.length);
      const result = formatRecipeResult(instruction, dish, scoreMeta);
      if (dishDescription && !result.description) {
        result.description = dishDescription;
      }
      return result;
    })
    .sort((left, right) => {
      if (right.coverageRatio !== left.coverageRatio) {
        return right.coverageRatio - left.coverageRatio;
      }
      return right.matchScore - left.matchScore;
    })
    .slice(0, limit);

  return { keywords, recipes };
}

function rankRecipes(dishes, instructions, availableKeywords, limit) {
  const lookup = buildInstructionLookup(instructions);
  const grouped = new Map();

  for (const dish of dishes) {
    const instruction = resolveInstructionForDish(dish, lookup);
    if (!instruction) continue;

    const scoreMeta = scoreRecipeAgainstAvailableProducts(dish, instruction, availableKeywords);
    if (scoreMeta.matchedIngredientCount <= 0) continue;

    const key = getInstructionKey(instruction);
    const existing = grouped.get(key);

    if (!existing || scoreMeta.weightedScore > existing.scoreMeta.weightedScore) {
      grouped.set(key, { instruction, dish, scoreMeta });
    } else if (existing) {
      existing.scoreMeta.matchedIngredientCount = Math.max(
        existing.scoreMeta.matchedIngredientCount,
        scoreMeta.matchedIngredientCount
      );
      existing.scoreMeta.matchedKeywords = [
        ...new Set([...existing.scoreMeta.matchedKeywords, ...scoreMeta.matchedKeywords]),
      ];
    }
  }

  return Array.from(grouped.values())
    .map(({ instruction, dish, scoreMeta }) => formatRecipeResult(instruction, dish, scoreMeta))
    .sort(compareRecipeResults)
    .slice(0, limit);
}

module.exports = {
  collectKeywordsFromProducts,
  collectKeywordsFromNames,
  buildInstructionLookup,
  resolveInstructionForDish,
  countMatchedKeywords,
  scoreRecipeAgainstAvailableProducts,
  buildScoreMetaFromDishKeywords,
  rankRelatedRecipes,
  formatRecipeResult,
  compareRecipeResults,
  rankRecipes,
};
