const express = require('express');
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const multer = require('multer');

const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');
const {
  notifyRecipeComment,
  notifyCommentLike,
  notifyNewFollower,
} = require('../utils/communityNotification');

const router = express.Router();

const uploadDir = path.join(__dirname, '..', '..', 'uploads', 'community');
fs.mkdirSync(uploadDir, { recursive: true });
const imageUpload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname || '').toLowerCase() || '.jpg';
      const safeExt = ['.jpg', '.jpeg', '.png', '.webp'].includes(ext) ? ext : '.jpg';
      cb(null, `${Date.now()}-${Math.round(Math.random() * 1e9)}${safeExt}`);
    },
  }),
  limits: { fileSize: 6 * 1024 * 1024, files: 8 },
  fileFilter: (req, file, cb) => {
    cb(null, /^image\//.test(file.mimetype || ''));
  },
});

const ACCOUNT_ID = 'account-thuc-quyen';
const ACCOUNT_NAME = 'Thuc Quyen';
const ACCOUNT_AVATAR_URL = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80';
const COMMUNITY_CACHE_TTL_MS = 15000;
const DEFAULT_RECIPE_LIMIT = 50;

let communityCookingCache = null;
let communityCookingCacheAt = 0;

router.get('/home', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const categories = (data.categories || []).map(normalizeCategory);
  const chefs = await topCommunityUsers(4);
  const recipes = (data.recipes || []).map(normalizeRecipe).slice(0, 4);

  res.json({ categories, chefs, recipes });
}));

router.post('/uploads/images', imageUpload.array('images', 8), asyncHandler(async (req, res) => {
  const files = req.files || [];
  const baseUrl = process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`;
  res.status(201).json({
    images: files.map((file) => `${baseUrl}/uploads/community/${file.filename}`),
  });
}));

router.get('/categories', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  res.json((data.categories || []).map(normalizeCategory));
}));

router.get('/chefs', asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 0);
  res.json(await topCommunityUsers(limit));
}));

router.get('/users/:customerId', asyncHandler(async (req, res) => {
  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: req.params.customerId });
  if (!user) {
    return res.status(404).json({ message: 'Community user not found' });
  }
  res.json(normalizeUserAsChef(user));
}));

router.get('/recipes', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  let recipes = dedupeByKey(
    (data.recipes || []).map(normalizeRecipe),
    (recipe) => recipe.id
  );
  if (req.query.categoryId) {
    recipes = recipes.filter((recipe) => recipe.categoryId === req.query.categoryId);
  }
  if (req.query.chefId) {
    recipes = recipes.filter((recipe) => recipe.chefId === req.query.chefId);
  }

  const requestedLimit = Number(req.query.limit || 0);
  const limit = requestedLimit > 0 ? requestedLimit : DEFAULT_RECIPE_LIMIT;
  res.json(recipes.slice(0, limit));
}));

router.get('/saved-recipe-ids', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const customerId = req.query.customerId || req.query.accountId || ACCOUNT_ID;
  const cookbookIds = new Set(
    (data.cookbooks || [])
      .filter((cookbook) => cookbookBelongsToCustomer(cookbook, customerId))
      .map(cookbookRef)
  );
  const recipeIds = [...new Set(
    (data.cookbookRecipes || [])
      .filter((item) => cookbookIds.has(cookbookRef(item)))
      .map((item) => recipeRef(item))
      .filter(Boolean)
  )];
  res.json({ recipeIds });
}));

router.get('/recipes/drafts/:customerId', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const drafts = (data.recipeDrafts || [])
    .filter((item) => item.CustomerID === req.params.customerId)
    .sort((left, right) => dateValue(right.UpdatedAt || right.CreatedAt) - dateValue(left.UpdatedAt || left.CreatedAt))
    .map(normalizeRecipeDraft);
  res.json({ hasDraft: drafts.length > 0, draft: drafts[0] || null, drafts });
}));

router.post('/recipes/drafts', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const draft = buildRecipeDraft(req.body, data.recipeDrafts || []);
  if (!draft.CustomerID) {
    return res.status(400).json({ message: 'customerId is required' });
  }

  await patchCommunityCooking(
    { _id: data._id },
    {
      $push: { recipeDrafts: draft },
    }
  );
  res.status(201).json(normalizeRecipeDraft(draft));
}));

router.delete('/recipes/drafts/:draftId', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const draftId = req.params.draftId;
  const customerId = req.query.customerId || req.query.accountId || req.body?.customerId || req.body?.accountId;
  if (!draftId || !customerId) {
    return res.status(400).json({ message: 'draftId and customerId are required' });
  }

  const draft = (data.recipeDrafts || []).find((item) => draftRef(item) === draftId);
  if (!draft) {
    return res.status(404).json({ message: 'Recipe draft not found' });
  }
  if (draft.CustomerID !== customerId) {
    return res.status(403).json({ message: 'Only draft owner can delete this draft' });
  }

  await patchCommunityCooking(
    { _id: data._id },
    {
      $pull: {
        recipeDrafts: {
          DraftID: draftId,
          CustomerID: customerId,
        },
      },
    }
  );
  res.json({ ok: true });
}));

router.post('/recipes/publish', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const customerId = req.body.customerId || req.body.CustomerID;
  const title = req.body.title || req.body.Title;
  if (!customerId || !title) {
    return res.status(400).json({ message: 'customerId and title are required' });
  }

  const recipeId = nextRecipeId(data.recipes || []);
  const detailId = recipeId;
  const now = new Date();
  const ingredientsText = req.body.ingredientsText || req.body.ingredients || '';
  const ingredientItems = normalizeIngredientItems(req.body.ingredientItems || req.body.IngredientItems);
  const ingredientNames = ingredientItems.length
    ? ingredientItems.map((item) => item.displayName)
    : splitLines(ingredientsText);
  const matchedProducts = await matchProductsForIngredients(ingredientNames);
  const imageUrls = normalizeImageUrls(req.body.imageUrls || req.body.ImageUrls || req.body.imageUrl || req.body.ImageUrl);
  const categoryId = req.body.categoryId || req.body.CategoryID || firstCategoryId(data.categories || []);
  const recipe = {
    RecipeID: recipeId,
    Title: title,
    CustomerID: customerId,
    CategoryID: categoryId,
    TimeMinutes: Number(req.body.timeMinutes || 0),
    IngredientCount: ingredientNames.length,
    ImageUrl: imageUrls[0] || '',
    VideoUrl: req.body.videoUrl || '',
    Status: 'published',
    CreatedAt: now,
    UpdatedAt: now,
  };
  const detail = {
    RecipeID: detailId,
    Calories: Number(req.body.calories || 0),
    SaltLevel: req.body.saltLevel || '',
    SugarLevel: req.body.sugarLevel || '',
    Instructions: req.body.steps || '',
    VideoUrl: req.body.videoUrl || '',
  };
  const ingredients = ingredientNames.map((name, index) => {
    const selected = ingredientItems[index] || {};
    const matched = matchedProducts.get(name);
    return {
      IngredientID: `${recipeId}-ING${String(index + 1).padStart(2, '0')}`,
      RecipeID: recipeId,
      DisplayName: selected.displayName || name,
      Quantity: selected.quantity || '',
      SortOrder: index,
      ProductID: selected.productId || (matched?._id ? String(matched._id) : ''),
      ProductSku: selected.productSku || matched?.sku || '',
      IconUrl: selected.imageUrl || firstImage(matched?.image) || matched?.imageUrl || '',
    };
  });
  const galleries = imageUrls.map((imageUrl, index) => ({
    GalleryID: `${recipeId}-IMG${String(index + 1).padStart(2, '0')}`,
    RecipeID: recipeId,
    ImageUrl: imageUrl,
    SortOrder: index,
  }));

  await patchCommunityCooking(
    { _id: data._id },
    {
      $push: {
        recipes: recipe,
        recipeDetails: detail,
        recipeIngredients: { $each: ingredients },
        recipeGalleries: { $each: galleries },
      },
    }
  );
  await mongoose.connection.db.collection('users').updateOne(
    { CustomerID: customerId },
    { $inc: { recipeCount: 1 } }
  );
  res.status(201).json(normalizeRecipe(recipe));
}));

router.put('/recipes/:recipeId', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const recipeId = req.params.recipeId;
  const customerId = req.body.customerId || req.body.CustomerID;
  const recipe = (data.recipes || []).find((item) => recipeRef(item) === recipeId);
  if (!recipe) {
    return res.status(404).json({ message: 'Community recipe not found' });
  }
  if (!customerId || recipe.CustomerID !== customerId) {
    return res.status(403).json({ message: 'Only recipe owner can update this recipe' });
  }

  const ingredientsText = req.body.ingredientsText || req.body.ingredients || '';
  const ingredientItems = normalizeIngredientItems(req.body.ingredientItems || req.body.IngredientItems);
  const ingredientNames = ingredientItems.length
    ? ingredientItems.map((item) => item.displayName)
    : splitLines(ingredientsText);
  const matchedProducts = await matchProductsForIngredients(ingredientNames);
  const imageUrls = normalizeImageUrls(req.body.imageUrls || req.body.ImageUrls || req.body.imageUrl || req.body.ImageUrl);
  const now = new Date();
  const nextRecipe = {
    ...recipe,
    Title: req.body.title || req.body.Title || recipe.Title,
    CategoryID: req.body.categoryId || req.body.CategoryID || recipe.CategoryID,
    TimeMinutes: Number(req.body.timeMinutes ?? req.body.TimeMinutes ?? recipe.TimeMinutes ?? 0),
    IngredientCount: ingredientNames.length,
    ImageUrl: imageUrls[0] || recipe.ImageUrl || '',
    VideoUrl: req.body.videoUrl || req.body.VideoUrl || '',
    UpdatedAt: now,
  };
  const nextDetail = {
    RecipeID: recipeId,
    Calories: Number(req.body.calories || 0),
    SaltLevel: req.body.saltLevel || req.body.SaltLevel || '',
    SugarLevel: req.body.sugarLevel || req.body.SugarLevel || '',
    Instructions: req.body.steps || req.body.Steps || '',
    VideoUrl: req.body.videoUrl || req.body.VideoUrl || '',
  };
  const nextIngredients = ingredientNames.map((name, index) => {
    const selected = ingredientItems[index] || {};
    const matched = matchedProducts.get(name);
    return {
      IngredientID: `${recipeId}-ING${String(index + 1).padStart(2, '0')}`,
      RecipeID: recipeId,
      DisplayName: selected.displayName || name,
      Quantity: selected.quantity || '',
      SortOrder: index,
      ProductID: selected.productId || (matched?._id ? String(matched._id) : ''),
      ProductSku: selected.productSku || matched?.sku || '',
      IconUrl: selected.imageUrl || firstImage(matched?.image) || matched?.imageUrl || '',
    };
  });
  const nextGalleries = imageUrls.map((imageUrl, index) => ({
    GalleryID: `${recipeId}-IMG${String(index + 1).padStart(2, '0')}`,
    RecipeID: recipeId,
    ImageUrl: imageUrl,
    SortOrder: index,
  }));

  await patchCommunityCooking(
    { _id: data._id },
    {
      $set: {
        recipes: (data.recipes || []).map((item) => recipeRef(item) === recipeId ? nextRecipe : item),
        recipeDetails: upsertByRecipeId(data.recipeDetails || [], nextDetail),
        recipeIngredients: [
          ...(data.recipeIngredients || []).filter((item) => recipeRef(item) !== recipeId),
          ...nextIngredients,
        ],
        recipeGalleries: [
          ...(data.recipeGalleries || []).filter((item) => recipeRef(item) !== recipeId),
          ...nextGalleries,
        ],
      },
    }
  );
  res.json(normalizeRecipe(nextRecipe));
}));

router.delete('/recipes/:recipeId', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const recipeId = req.params.recipeId;
  const customerId = req.query.customerId || req.query.accountId || req.body?.customerId || req.body?.accountId;
  const recipe = (data.recipes || []).find((item) => recipeRef(item) === recipeId);
  if (!recipe) {
    return res.status(404).json({ message: 'Community recipe not found' });
  }
  if (!customerId || recipe.CustomerID !== customerId) {
    return res.status(403).json({ message: 'Only recipe owner can delete this recipe' });
  }

  const nextCookbookRecipes = (data.cookbookRecipes || []).filter((item) => recipeRef(item) !== recipeId);
  const removedByCookbook = (data.cookbookRecipes || []).reduce((result, item) => {
    if (recipeRef(item) === recipeId) {
      const cookbookId = cookbookRef(item);
      result[cookbookId] = (result[cookbookId] || 0) + 1;
    }
    return result;
  }, {});
  const nextCookbooks = (data.cookbooks || []).map((cookbook) => {
    const removedCount = removedByCookbook[cookbookRef(cookbook)] || 0;
    if (!removedCount) {
      return cookbook;
    }
    return {
      ...cookbook,
      RecipeCount: Math.max(0, (cookbook.RecipeCount ?? cookbook.recipeCount ?? 0) - removedCount),
      UpdatedAt: new Date(),
    };
  });

  await patchCommunityCooking(
    { _id: data._id },
    {
      $set: {
        recipes: (data.recipes || []).filter((item) => recipeRef(item) !== recipeId),
        recipeDetails: (data.recipeDetails || []).filter((item) => recipeRef(item) !== recipeId),
        recipeIngredients: (data.recipeIngredients || []).filter((item) => recipeRef(item) !== recipeId),
        recipeGalleries: (data.recipeGalleries || []).filter((item) => recipeRef(item) !== recipeId),
        recipeComments: (data.recipeComments || []).filter((item) => recipeRef(item) !== recipeId),
        cookbookRecipes: nextCookbookRecipes,
        cookbooks: nextCookbooks,
      },
    }
  );
  await mongoose.connection.db.collection('users').updateOne(
    { CustomerID: customerId },
    { $inc: { recipeCount: -1 } }
  );
  res.json({ ok: true });
}));

router.get('/recipes/:recipeId/detail', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const recipeId = req.params.recipeId;
  const viewerId = req.query.viewerId || req.query.customerId || req.query.accountId || '';
  const recipe = (data.recipes || []).map(normalizeRecipe).find((item) => item.id === recipeId);
  if (!recipe) {
    return res.status(404).json({ message: 'Community recipe not found' });
  }

  const chef = await userAsCommunityChef(recipe.chefId);
  const detail = normalizeRecipeDetail((data.recipeDetails || []).find((item) => recipeRef(item) === recipeId) || null);
  const ingredients = (data.recipeIngredients || [])
    .filter((item) => recipeRef(item) === recipeId)
    .sort(sortByOrder)
    .map(normalizeRecipeIngredient);
  const galleries = (data.recipeGalleries || [])
    .filter((item) => recipeRef(item) === recipeId)
    .sort(sortByOrder)
    .map(normalizeRecipeGallery);
  const savedComments = (data.recipeComments || [])
    .filter((item) => recipeRef(item) === recipeId)
    .sort(sortByOrder);
  const commentUsers = await usersByCustomerIds(savedComments.map((comment) => comment.CustomerID));
  const comments = savedComments.map((comment) => normalizeRecipeComment(comment, commentUsers.get(comment.CustomerID), viewerId));

  const productObjectIds = ingredients
    .map((ingredient) => ingredient.productId)
    .filter((productId) => mongoose.Types.ObjectId.isValid(productId))
    .map((productId) => new mongoose.Types.ObjectId(productId));

  const products = productObjectIds.length
    ? await Product.find({ _id: { $in: productObjectIds } }).lean()
    : [];

  const productsById = products.reduce((result, product) => {
    const id = String(product._id);
    result[id] = {
      id,
      name: product.name || product.productName || '',
      price: product.price || 0,
      originalPrice: product.originalPrice || product.basePrice || product.price || 0,
      sku: product.sku || '',
      imageUrl: product.imageUrl || firstImage(product.image) || '',
      weight: product.weight || product.unit || '',
      rating: product.rating || 0,
      reviewCount: product.reviewCount || 0,
      soldCount: product.soldCount || product.purchaseCount || 0,
      description: product.description || '',
      origin: product.origin || '',
      condition: product.condition || product.status || '',
      fatContent: product.fatContent || product.brand || '',
      categoryId: product.categoryId || '',
      subcategoryId: product.subcategoryId || '',
    };
    return result;
  }, {});

  res.json({ recipe, chef, detail, ingredients, productsById, galleries, comments });
}));

router.get('/recipes/:recipeId/saved', asyncHandler(async (req, res) => {
  const data = await getCommunityCooking();
  const customerId = req.query.customerId || req.query.accountId || ACCOUNT_ID;
  const cookbookIds = (data.cookbooks || [])
    .filter((cookbook) => cookbookBelongsToCustomer(cookbook, customerId))
    .map(cookbookRef);
  const saved = (data.cookbookRecipes || [])
    .some((item) => cookbookIds.includes(cookbookRef(item))
      && recipeIdsEquivalent(data, recipeRef(item), req.params.recipeId));
  res.json({ saved });
}));

router.get('/cookbooks', asyncHandler(async (req, res) => {
  const customerId = req.query.customerId || req.query.accountId || ACCOUNT_ID;
  const data = await ensureSeedCookbooksForCustomer(customerId);
  const cookbooks = dedupeByKey(
    (data.cookbooks || [])
      .filter((cookbook) => cookbookBelongsToCustomer(cookbook, customerId))
      .sort((left, right) => dateValue(right.UpdatedAt || right.CreatedAt) - dateValue(left.UpdatedAt || left.CreatedAt))
      .map(normalizeCookbook),
    (cookbook) => cookbook.id
  );
  res.json(cookbooks);
}));

router.get('/cookbooks/:cookbookId', asyncHandler(async (req, res) => {
  const cookbookId = req.params.cookbookId;
  const data = await getCommunityCooking();
  const cookbook = findCookbookInData(data, cookbookId);
  if (!cookbook) {
    return res.status(404).json({ message: 'Community cookbook not found' });
  }

  const canonicalCookbookId = cookbookRef(cookbook);
  const links = (data.cookbookRecipes || [])
    .filter((item) => cookbookRef(item) === canonicalCookbookId)
    .sort(sortByOrder);
  const linkRecipeIds = [...new Set(links.map((link) => recipeRef(link)).filter(Boolean))];
  const normalizedRecipes = (data.recipes || []).map(normalizeRecipe);
  const recipes = dedupeByKey(
    normalizedRecipes.filter((recipe) => linkRecipeIds.some((linkId) => recipeIdsEquivalent(data, recipe.id, linkId))),
    (recipe) => recipe.id
  );

  res.json({
    cookbook: normalizeCookbook(cookbook),
    recipes: linkRecipeIds
      .map((linkId) => recipes.find((recipe) => recipeIdsEquivalent(data, recipe.id, linkId)))
      .filter(Boolean),
  });
}));

router.post('/cookbooks', asyncHandler(async (req, res) => {
  const { title, description = '' } = req.body;
  const recipeId = String(req.body.recipeId || req.body.RecipeID || '').trim();
  const customerId = req.body.customerId || req.body.accountId || ACCOUNT_ID;
  if (!title) {
    return res.status(400).json({ message: 'title is required' });
  }

  const data = await getCommunityCooking();
  const rawRecipe = recipeId
    ? (data.recipes || []).find((item) => recipeMatchesId(item, recipeId))
    : null;
  const recipe = rawRecipe ? normalizeRecipe(rawRecipe) : null;
  const cookbookId = nextCookbookId(data.cookbooks || []);
  const now = new Date();
  const cookbook = {
    CookbookID: cookbookId,
    CustomerID: customerId,
    Title: title,
    Description: description,
    CoverImageUrl: recipe?.imageUrl || '',
    RecipeCount: recipe ? 1 : 0,
    CreatedAt: now,
    UpdatedAt: now,
  };
  const pushPayload = { cookbooks: cookbook };
  if (rawRecipe) {
    pushPayload.cookbookRecipes = {
      CookbookID: cookbookId,
      RecipeID: recipeRef(rawRecipe),
      SortOrder: 0,
      SavedAt: now,
    };
  }

  await patchCommunityCooking(
    { _id: data._id },
    {
      $push: pushPayload,
    }
  );
  res.status(201).json(normalizeCookbook(cookbook));
}));

router.post('/cookbooks/:cookbookId/recipes', asyncHandler(async (req, res) => {
  const body = req.body || {};
  const recipeId = String(body.recipeId || body.RecipeID || '').trim();
  const cookbookId = req.params.cookbookId;
  if (!recipeId) {
    return res.status(400).json({ message: 'recipeId is required' });
  }

  let data = await getCommunityCooking();
  let cookbook = findCookbookInData(data, cookbookId);
  if (!cookbook) {
    const ownerId = body.customerId || body.accountId || req.query.customerId || req.query.accountId || ACCOUNT_ID;
    data = await ensureSeedCookbooksForCustomer(ownerId);
    cookbook = findCookbookInData(data, cookbookId);
  }
  if (!cookbook) {
    return res.status(404).json({ message: 'Community cookbook not found' });
  }

  const rawRecipe = (data.recipes || []).find((item) => recipeMatchesId(item, recipeId));
  if (!rawRecipe) {
    return res.status(404).json({ message: 'Community recipe not found' });
  }

  const canonicalCookbookId = cookbookRef(cookbook);
  const canonicalRecipeId = recipeRef(rawRecipe);
  const recipe = normalizeRecipe(rawRecipe);
  const cookbookRecipes = [...(data.cookbookRecipes || [])];
  const alreadyLinked = cookbookRecipes.some((item) =>
    cookbookRef(item) === canonicalCookbookId
    && recipeIdsEquivalent(data, recipeRef(item), canonicalRecipeId)
  );

  if (!alreadyLinked) {
    cookbookRecipes.push({
      CookbookID: canonicalCookbookId,
      RecipeID: canonicalRecipeId,
      cookbookId: canonicalCookbookId,
      recipeId: canonicalRecipeId,
      SortOrder: cookbookRecipes.filter((item) => cookbookRef(item) === canonicalCookbookId).length,
      SavedAt: new Date(),
    });
  }

  const nextCookbooks = (data.cookbooks || []).map((item) => {
    if (cookbookRef(item) !== canonicalCookbookId) {
      return item;
    }
    const recipeCount = cookbookRecipes.filter((link) => cookbookRef(link) === canonicalCookbookId).length;
    const coverImage = item.CoverImageUrl || item.coverImageUrl || item.imageUrl || recipe.imageUrl || '';
    return {
      ...item,
      RecipeCount: recipeCount,
      CoverImageUrl: coverImage,
      imageUrl: coverImage,
      UpdatedAt: new Date(),
    };
  });

  await patchCommunityCooking(
    { _id: data._id },
    {
      $set: {
        cookbooks: nextCookbooks,
        cookbookRecipes,
      },
    }
  );

  res.json({ ok: true });
}));

router.delete('/cookbooks/recipes/:recipeId', asyncHandler(async (req, res) => {
  const recipeId = req.params.recipeId;
  const customerId = req.query.customerId || req.body.customerId || req.body.accountId || ACCOUNT_ID;
  if (!recipeId || !customerId) {
    return res.status(400).json({ message: 'recipeId and customerId are required' });
  }

  const data = await getCommunityCooking();
  const ownedCookbookIds = new Set((data.cookbooks || [])
    .filter((cookbook) => (cookbook.CustomerID || cookbook.customerId || cookbook.accountId || '') === customerId)
    .map(cookbookRef));
  if (!ownedCookbookIds.size) {
    return res.json({ ok: true, removed: false });
  }

  const cookbookRecipes = data.cookbookRecipes || [];
  const nextCookbookRecipes = cookbookRecipes.filter((item) =>
    !(ownedCookbookIds.has(cookbookRef(item)) && recipeIdsEquivalent(data, recipeRef(item), recipeId))
  );
  const removed = nextCookbookRecipes.length !== cookbookRecipes.length;
  if (!removed) {
    return res.json({ ok: true, removed: false });
  }

  const countsByCookbookId = nextCookbookRecipes.reduce((counts, item) => {
    const id = cookbookRef(item);
    counts[id] = (counts[id] || 0) + 1;
    return counts;
  }, {});
  const nextCookbooks = (data.cookbooks || []).map((cookbook) => {
    const id = cookbookRef(cookbook);
    if (!ownedCookbookIds.has(id)) {
      return cookbook;
    }
    return {
      ...cookbook,
      RecipeCount: countsByCookbookId[id] || 0,
      UpdatedAt: new Date(),
    };
  });

  await patchCommunityCooking(
    { _id: data._id },
    {
      $set: {
        cookbooks: nextCookbooks,
        cookbookRecipes: nextCookbookRecipes,
      },
    }
  );
  res.json({ ok: true, removed: true });
}));

router.get('/follows', asyncHandler(async (req, res) => {
  const { chefId, relationType } = req.query;
  if (!chefId || !relationType) {
    return res.status(400).json({ message: 'chefId and relationType are required' });
  }

  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: chefId });
  if (!user) {
    return res.json([]);
  }

  const ids = relationType === 'following'
    ? user.FollowingCustomerIDs || []
    : user.FollowerCustomerIDs || [];
  const users = await usersByCustomerIds(ids);
  res.json(ids.map((customerId) => userAsFollowItem(users.get(customerId), customerId, chefId, relationType)));
}));

router.get('/follows/counts', asyncHandler(async (req, res) => {
  const chefId = req.query.chefId;
  const viewerId = req.query.viewerId || req.query.customerId;
  if (!chefId) {
    return res.status(400).json({ message: 'chefId is required' });
  }

  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: chefId });
  const following = user?.FollowingCustomerIDs?.length ?? user?.followingCount ?? 0;
  const followers = user?.FollowerCustomerIDs?.length ?? user?.followerCount ?? 0;
  const isFollowing = viewerId
    ? !!(user?.FollowerCustomerIDs || []).includes(viewerId)
    : false;
  res.json({ following, followers, isFollowing });
}));

router.post('/follows/toggle', asyncHandler(async (req, res) => {
  const followerCustomerId = req.body.followerCustomerId || req.body.customerId;
  const followingCustomerId = req.body.followingCustomerId || req.body.chefId;
  if (!followerCustomerId || !followingCustomerId) {
    return res.status(400).json({ message: 'followerCustomerId and followingCustomerId are required' });
  }
  if (followerCustomerId === followingCustomerId) {
    return res.status(400).json({ message: 'Cannot follow yourself' });
  }

  const users = mongoose.connection.db.collection('users');
  const [follower, following] = await Promise.all([
    users.findOne({ CustomerID: followerCustomerId }),
    users.findOne({ CustomerID: followingCustomerId }),
  ]);
  if (!follower || !following) {
    return res.status(404).json({ message: 'Customer not found' });
  }

  const currentlyFollowing = (follower.FollowingCustomerIDs || []).includes(followingCustomerId);
  const followerUpdate = currentlyFollowing
    ? { $pull: { FollowingCustomerIDs: followingCustomerId } }
    : { $addToSet: { FollowingCustomerIDs: followingCustomerId } };
  const followingUpdate = currentlyFollowing
    ? { $pull: { FollowerCustomerIDs: followerCustomerId } }
    : { $addToSet: { FollowerCustomerIDs: followerCustomerId } };

  await Promise.all([
    users.updateOne({ CustomerID: followerCustomerId }, followerUpdate),
    users.updateOne({ CustomerID: followingCustomerId }, followingUpdate),
  ]);

  const [updatedFollower, updatedFollowing] = await Promise.all([
    users.findOne({ CustomerID: followerCustomerId }),
    users.findOne({ CustomerID: followingCustomerId }),
  ]);
  const followingIds = updatedFollower.FollowingCustomerIDs || [];
  const followerIds = updatedFollowing.FollowerCustomerIDs || [];
  await Promise.all([
    users.updateOne(
      { CustomerID: followerCustomerId },
      { $set: { followingCount: followingIds.length } }
    ),
    users.updateOne(
      { CustomerID: followingCustomerId },
      { $set: { followerCount: followerIds.length } }
    ),
  ]);

  if (!currentlyFollowing) {
    const followerName = follower.FullName || follower.name || followerCustomerId;
    await notifyNewFollower({
      followedCustomerId: followingCustomerId,
      followerId: followerCustomerId,
      followerName,
    });
  }

  res.json({
    ok: true,
    isFollowing: !currentlyFollowing,
    following: followingIds.length,
    followers: followerIds.length,
  });
}));

router.post('/comments', asyncHandler(async (req, res) => {
  const { recipeId, content } = req.body;
  const customerId = req.body.customerId || req.body.accountId || ACCOUNT_ID;
  if (!recipeId || !content || !customerId) {
    return res.status(400).json({ message: 'recipeId, customerId and content are required' });
  }

  const data = await getCommunityCooking();
  const recipe = (data.recipes || []).find((item) => recipeRef(item) === recipeId);
  if (!recipe) {
    return res.status(404).json({ message: 'Community recipe not found' });
  }

  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
  if (!user) {
    return res.status(404).json({ message: 'Customer not found' });
  }

  const sortOrder = (data.recipeComments || []).filter((item) => recipeRef(item) === recipeId).length;
  const commentId = nextRecipeCommentId(data.recipeComments || []);
  const comment = {
    CommentID: commentId,
    RecipeID: recipeId,
    CustomerID: customerId,
    Content: content,
    LikeCount: 0,
    CommentLikedCustomerIDs: [],
    SortOrder: sortOrder,
    CreatedAt: new Date(),
  };

  await patchCommunityCooking(
    { _id: data._id },
    { $push: { recipeComments: comment } }
  );

  const recipeOwnerId = recipe.CustomerID || recipe.customerId || recipe.chefId || '';
  const commenterName = user.FullName || user.name || customerId;
  await notifyRecipeComment({
    recipeOwnerId,
    commenterId: customerId,
    commenterName,
    recipeId,
    recipeTitle: recipe.Title || recipe.title || '',
    commentPreview: content,
  });

  res.status(201).json(normalizeRecipeComment(comment, user, customerId));
}));

router.post('/comments/:commentId/like', asyncHandler(async (req, res) => {
  const commentId = req.params.commentId;
  const customerId = req.body.customerId || req.body.accountId || ACCOUNT_ID;
  if (!commentId || !customerId) {
    return res.status(400).json({ message: 'commentId and customerId are required' });
  }

  const data = await getCommunityCooking();
  const comments = data.recipeComments || [];
  const comment = comments.find((item) => commentRef(item) === commentId);
  if (!comment) {
    return res.status(404).json({ message: 'Community comment not found' });
  }

  const likedIds = commentLikedCustomerIds(comment);
  const currentlyLiked = likedIds.includes(customerId);
  const nextLikedIds = currentlyLiked
    ? likedIds.filter((id) => id !== customerId)
    : [...likedIds, customerId];
  const currentLikeCount = Number(comment.LikeCount ?? comment.likeCount ?? likedIds.length) || 0;
  const nextComment = {
    ...comment,
    CommentLikedCustomerIDs: nextLikedIds,
    LikeCount: currentlyLiked ? Math.max(0, currentLikeCount - 1) : currentLikeCount + 1,
    UpdatedAt: new Date(),
  };

  await patchCommunityCooking(
    { _id: data._id },
    {
      $set: {
        recipeComments: comments.map((item) => commentRef(item) === commentId ? nextComment : item),
      },
    }
  );

  if (!currentlyLiked) {
    const recipeId = recipeRef(comment);
    const recipe = (data.recipes || []).find((item) => recipeRef(item) === recipeId);
    const liker = await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
    const likerName = liker?.FullName || liker?.name || customerId;
    await notifyCommentLike({
      commentOwnerId: comment.CustomerID || comment.customerId || '',
      likerId: customerId,
      likerName,
      recipeId,
      recipeTitle: recipe?.Title || recipe?.title || '',
    });
  }

  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: nextComment.CustomerID });
  res.json(normalizeRecipeComment(nextComment, user, customerId));
}));

function invalidateCommunityCache() {
  communityCookingCache = null;
  communityCookingCacheAt = 0;
}

async function getCommunityCooking(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && communityCookingCache && (now - communityCookingCacheAt) < COMMUNITY_CACHE_TTL_MS) {
    return communityCookingCache;
  }

  const collection = communityCollection();
  const data = await collection.findOne({});
  if (data && Array.isArray(data.recipes) && data.recipes.length > 0) {
    return storeCommunityCache(data);
  }

  const seedPath = path.join(__dirname, '..', '..', '..', 'app', 'src', 'main', 'assets', 'community_cooking.json');
  const seed = JSON.parse(fs.readFileSync(seedPath, 'utf8'));
  if (data) {
    await collection.updateOne(
      { _id: data._id },
      {
        $set: {
          categories: Array.isArray(data.categories) && data.categories.length > 0 ? data.categories : seed.categories,
          chefs: Array.isArray(data.chefs) && data.chefs.length > 0 ? data.chefs : seed.chefs,
          recipes: seed.recipes,
          recipeDetails: Array.isArray(data.recipeDetails) && data.recipeDetails.length > 0 ? data.recipeDetails : seed.recipeDetails,
          recipeIngredients: Array.isArray(data.recipeIngredients) && data.recipeIngredients.length > 0 ? data.recipeIngredients : seed.recipeIngredients,
          recipeGalleries: Array.isArray(data.recipeGalleries) && data.recipeGalleries.length > 0 ? data.recipeGalleries : seed.recipeGalleries,
          cookbooks: Array.isArray(data.cookbooks) && data.cookbooks.length > 0 ? data.cookbooks : seed.cookbooks,
          cookbookRecipes: Array.isArray(data.cookbookRecipes) && data.cookbookRecipes.length > 0 ? data.cookbookRecipes : seed.cookbookRecipes,
          SeededFromAsset: true,
          UpdatedAt: new Date(),
        },
      }
    );
    invalidateCommunityCache();
    return storeCommunityCache(await collection.findOne({ _id: data._id }));
  }

  seed.SeededFromAsset = true;
  seed.CreatedAt = new Date();
  await collection.insertOne(seed);
  const inserted = await collection.findOne({});
  communityCookingCache = inserted;
  communityCookingCacheAt = Date.now();
  return inserted;
}

function storeCommunityCache(data) {
  communityCookingCache = data;
  communityCookingCacheAt = Date.now();
  return data;
}

function communityCollection() {
  return mongoose.connection.db.collection('community_cooking');
}

async function patchCommunityCooking(filter, update) {
  const result = await communityCollection().updateOne(filter, update);
  invalidateCommunityCache();
  return result;
}

async function topCommunityUsers(limit = 0) {
  const query = { recipeCount: { $gt: 0 } };
  const cursor = mongoose.connection.db.collection('users')
    .find(query)
    .sort({ recipeCount: -1, followerCount: -1, FullName: 1 });
  if (limit > 0) {
    cursor.limit(limit);
  }
  const users = await cursor.toArray();
  return users.map(normalizeUserAsChef);
}

async function userAsCommunityChef(customerId) {
  if (!customerId) {
    return null;
  }
  const user = await mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
  return user ? normalizeUserAsChef(user) : null;
}

async function usersByCustomerIds(customerIds) {
  const uniqueIds = [...new Set((customerIds || []).filter(Boolean))];
  if (!uniqueIds.length) {
    return new Map();
  }
  const users = await mongoose.connection.db.collection('users')
    .find({ CustomerID: { $in: uniqueIds } })
    .toArray();
  return new Map(users.map((user) => [user.CustomerID, user]));
}

function normalizeUserAsChef(user) {
  return {
    id: user.CustomerID || String(user._id),
    name: user.FullName || user.name || 'Người dùng Veggo',
    recipeCount: user.recipeCount || 0,
    likes: user.followerCount || 0,
    avatarUrl: user.avatarUrl || '',
    imageUrl: user.avatarUrl || '',
  };
}

function userAsFollowItem(user, customerId, chefId, relationType) {
  return {
    id: `${chefId}-${relationType}-${customerId}`,
    chefId,
    relationType,
    name: user?.FullName || user?.name || customerId,
    location: user?.Address || '',
    avatarUrl: user?.avatarUrl || '',
    imageUrl: user?.avatarUrl || '',
    following: true,
  };
}

function normalizeCategory(category) {
  return {
    id: category.CategoryID || category.id || '',
    name: category.Name || category.name || '',
    recipeCount: category.RecipeCount ?? category.recipeCount ?? 0,
    imageUrl: category.ImageUrl || category.imageUrl || '',
    iconEmoji: category.IconEmoji || category.iconEmoji || '',
  };
}

function normalizeRecipe(recipe) {
  return {
    id: recipe.RecipeID || recipe.id || '',
    title: recipe.Title || recipe.title || '',
    categoryId: recipe.CategoryID || recipe.categoryId || '',
    chefId: recipe.CustomerID || recipe.chefId || '',
    timeMinutes: recipe.TimeMinutes ?? recipe.timeMinutes ?? 0,
    ingredientCount: recipe.IngredientCount ?? recipe.ingredientCount ?? 0,
    imageUrl: recipe.ImageUrl || recipe.imageUrl || '',
  };
}

function normalizeRecipeDraft(draft) {
  const imageUrls = normalizeImageUrls(draft.ImageUrls || draft.ImageUrl);
  return {
    draftId: draft.DraftID || '',
    customerId: draft.CustomerID || '',
    title: draft.Title || '',
    categoryId: draft.CategoryID || '',
    timeMinutes: draft.TimeMinutes ?? draft.timeMinutes ?? 0,
    imageUrl: imageUrls[0] || '',
    imageUrls,
    videoUrl: draft.VideoUrl || '',
    ingredientsText: draft.IngredientsText || '',
    ingredientItems: normalizeIngredientItems(draft.IngredientItems),
    steps: draft.Steps || '',
    calories: draft.Calories || 0,
    saltLevel: draft.SaltLevel || '',
    sugarLevel: draft.SugarLevel || '',
    createdAt: draft.CreatedAt || '',
    updatedAt: draft.UpdatedAt || '',
  };
}

function normalizeCookbook(cookbook) {
  return {
    id: cookbook.CookbookID || cookbook.id || '',
    customerId: cookbook.CustomerID || cookbook.customerId || cookbook.accountId || '',
    accountId: cookbook.CustomerID || cookbook.accountId || '',
    title: cookbook.Title || cookbook.title || '',
    recipeCount: cookbook.RecipeCount ?? cookbook.recipeCount ?? 0,
    imageUrl: cookbook.CoverImageUrl || cookbook.ImageUrl || cookbook.imageUrl || '',
  };
}

function normalizeRecipeDetail(detail) {
  if (!detail) {
    return null;
  }
  return {
    recipeId: detail.RecipeID || detail.recipeId || '',
    calories: detail.Calories ?? detail.calories ?? 0,
    saltLevel: detail.SaltLevel || detail.saltLevel || '',
    sugarLevel: detail.SugarLevel || detail.sugarLevel || '',
    rating: detail.Rating ?? detail.rating ?? 0,
    instructions: detail.Instructions || detail.instructions || '',
    videoUrl: detail.VideoUrl || detail.videoUrl || '',
  };
}

function normalizeRecipeIngredient(ingredient) {
  return {
    id: ingredient.IngredientID || ingredient.id || '',
    recipeId: recipeRef(ingredient),
    productId: ingredient.ProductID || ingredient.productId || '',
    productSku: ingredient.ProductSku || ingredient.productSku || '',
    displayName: ingredient.DisplayName || ingredient.displayName || '',
    iconUrl: ingredient.IconUrl || ingredient.iconUrl || '',
    iconEmoji: ingredient.IconEmoji || ingredient.iconEmoji || '',
    quantity: ingredient.Quantity || ingredient.quantity || '',
    sortOrder: ingredient.SortOrder ?? ingredient.sortOrder ?? 0,
  };
}

function normalizeRecipeGallery(gallery) {
  return {
    id: gallery.GalleryID || gallery.id || '',
    recipeId: recipeRef(gallery),
    imageUrl: gallery.ImageUrl || gallery.imageUrl || '',
    sortOrder: gallery.SortOrder ?? gallery.sortOrder ?? 0,
  };
}

function normalizeRecipeComment(comment, user, viewerId = '') {
  const likedIds = commentLikedCustomerIds(comment);
  return {
    id: comment.CommentID || comment.id || '',
    recipeId: recipeRef(comment),
    userName: user?.FullName || user?.name || comment.UserName || '',
    userImageUrl: user?.avatarUrl || comment.UserImageUrl || '',
    content: comment.Content || comment.content || '',
    likeCount: comment.LikeCount ?? comment.likeCount ?? likedIds.length,
    likedByCurrentUser: viewerId ? likedIds.includes(viewerId) : false,
    sortOrder: comment.SortOrder ?? comment.sortOrder ?? 0,
  };
}

function recipeRef(item) {
  return item.RecipeID || item.id || item.recipeId || item.recipeID || item.RecipeId || '';
}

function commentRef(item) {
  return item.CommentID || item.commentId || item.id || '';
}

function commentLikedCustomerIds(comment) {
  if (Array.isArray(comment.CommentLikedCustomerIDs)) {
    return [...new Set(comment.CommentLikedCustomerIDs.filter(Boolean))];
  }
  if (Array.isArray(comment.likedCustomerIds)) {
    return [...new Set(comment.likedCustomerIds.filter(Boolean))];
  }
  return [];
}

function draftRef(item) {
  return item.DraftID || item.draftId || item.id || '';
}

function cookbookRef(item) {
  return item.CookbookID || item.cookbookId || item.id || '';
}

function findCookbookInData(data, cookbookId) {
  if (!data || !cookbookId) {
    return null;
  }
  return (data.cookbooks || []).find((item) => cookbookRef(item) === cookbookId) || null;
}

function readCommunitySeed() {
  const seedPath = path.join(__dirname, '..', '..', '..', 'app', 'src', 'main', 'assets', 'community_cooking.json');
  return JSON.parse(fs.readFileSync(seedPath, 'utf8'));
}

async function ensureSeedCookbooksForCustomer(customerId) {
  const data = await getCommunityCooking();
  if (!customerId) {
    return data;
  }

  let seed;
  try {
    seed = readCommunitySeed();
  } catch (error) {
    return data;
  }

  const existingIds = new Set((data.cookbooks || []).map(cookbookRef).filter(Boolean));
  const seedCookbooks = (seed.cookbooks || []).filter((cookbook) => cookbookBelongsToCustomer(cookbook, customerId));
  const cookbooksToAdd = seedCookbooks.filter((cookbook) => !existingIds.has(cookbookRef(cookbook)));
  if (!cookbooksToAdd.length) {
    return data;
  }

  const cookbookIdsToAdd = new Set(cookbooksToAdd.map(cookbookRef));
  const cookbookRecipesToAdd = (seed.cookbookRecipes || []).filter((link) => cookbookIdsToAdd.has(cookbookRef(link)));
  const pushPayload = { cookbooks: { $each: cookbooksToAdd } };
  if (cookbookRecipesToAdd.length > 0) {
    pushPayload.cookbookRecipes = { $each: cookbookRecipesToAdd };
  }

  await patchCommunityCooking({ _id: data._id }, { $push: pushPayload });
  return getCommunityCooking(true);
}

function cookbookBelongsToCustomer(cookbook, customerId) {
  if (!cookbook || !customerId) {
    return false;
  }
  const ownerId = cookbook.CustomerID || cookbook.customerId || cookbook.accountId || '';
  return ownerId === customerId;
}

function findRecipeInData(data, recipeId) {
  if (!recipeId) {
    return null;
  }
  const raw = (data.recipes || []).find((item) => recipeMatchesId(item, recipeId));
  return raw ? normalizeRecipe(raw) : null;
}

function recipeMatchesId(item, recipeId) {
  if (!item || !recipeId) {
    return false;
  }
  const target = String(recipeId).trim();
  if (!target) {
    return false;
  }
  return recipeRef(item) === target
    || String(item.LegacyRecipeID || item.legacyRecipeId || '').trim() === target;
}

function recipeIdsEquivalent(data, leftId, rightId) {
  if (!leftId || !rightId) {
    return false;
  }
  if (leftId === rightId) {
    return true;
  }
  const left = (data.recipes || []).find((item) => recipeMatchesId(item, leftId));
  const right = (data.recipes || []).find((item) => recipeMatchesId(item, rightId));
  if (!left || !right) {
    return false;
  }
  return recipeRef(left) === recipeRef(right);
}

function dedupeByKey(items, keySelector) {
  const map = new Map();
  for (const item of items || []) {
    const key = keySelector(item);
    if (!key || map.has(key)) {
      continue;
    }
    map.set(key, item);
  }
  return [...map.values()];
}

function nextCookbookId(cookbooks) {
  const latestNumber = (cookbooks || []).reduce((max, cookbook) => {
    const id = cookbookRef(cookbook);
    const number = /^CB\d+$/.test(id) ? Number(id.slice(2)) : 0;
    return Number.isFinite(number) && number > max ? number : max;
  }, 0);
  const nextNumber = latestNumber + 1;
  return `CB${String(nextNumber).padStart(4, '0')}`;
}

function nextRecipeId(recipes) {
  const latestNumber = (recipes || []).reduce((max, recipe) => {
    const id = recipeRef(recipe);
    const number = /^R\d+$/.test(id) ? Number(id.slice(1)) : 0;
    return Number.isFinite(number) && number > max ? number : max;
  }, 0);
  return `R${String(latestNumber + 1).padStart(4, '0')}`;
}

function buildRecipeDraft(body, drafts) {
  const customerId = body.customerId || body.CustomerID || '';
  const now = new Date();
  const imageUrls = normalizeImageUrls(body.imageUrls || body.ImageUrls || body.imageUrl || body.ImageUrl);
  return {
    DraftID: nextDraftId(drafts),
    CustomerID: customerId,
    Title: body.title || body.Title || '',
    CategoryID: body.categoryId || body.CategoryID || '',
    TimeMinutes: Number(body.timeMinutes || body.TimeMinutes || 0),
    ImageUrl: imageUrls[0] || '',
    ImageUrls: imageUrls,
    VideoUrl: body.videoUrl || body.VideoUrl || '',
    IngredientsText: body.ingredientsText || body.ingredients || '',
    IngredientItems: normalizeIngredientItems(body.ingredientItems || body.IngredientItems),
    Steps: body.steps || body.Steps || '',
    Calories: Number(body.calories || 0),
    SaltLevel: body.saltLevel || body.SaltLevel || '',
    SugarLevel: body.sugarLevel || body.SugarLevel || '',
    CreatedAt: now,
    UpdatedAt: now,
  };
}

function nextDraftId(drafts) {
  const latestNumber = (drafts || []).reduce((max, draft) => {
    const id = draft.DraftID || draft.draftId || '';
    const number = /^DR\d+$/.test(id) ? Number(id.slice(2)) : 0;
    return Number.isFinite(number) && number > max ? number : max;
  }, 0);
  return `DR${String(latestNumber + 1).padStart(4, '0')}`;
}

function firstCategoryId(categories) {
  return categories?.[0]?.CategoryID || categories?.[0]?.id || '';
}

function splitLines(value) {
  return String(value || '')
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function normalizeImageUrls(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || '').trim()).filter(Boolean);
  }
  const single = String(value || '').trim();
  return single ? [single] : [];
}

function normalizeIngredientItems(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => ({
      productId: item.productId || item.ProductID || '',
      productSku: item.productSku || item.ProductSku || '',
      displayName: item.displayName || item.DisplayName || item.name || '',
      imageUrl: item.imageUrl || item.ImageUrl || '',
      quantity: item.quantity || item.Quantity || '',
    }))
    .filter((item) => item.displayName || item.productId);
}

function upsertByRecipeId(items, nextItem) {
  let replaced = false;
  const result = (items || []).map((item) => {
    if (recipeRef(item) !== nextItem.RecipeID) {
      return item;
    }
    replaced = true;
    return nextItem;
  });
  if (!replaced) {
    result.push(nextItem);
  }
  return result;
}

async function matchProductsForIngredients(ingredientNames) {
  const entries = await Promise.all((ingredientNames || []).map(async (name) => {
    const pattern = new RegExp(escapeRegex(name), 'i');
    const product = await Product.findOne({
      $or: [
        { name: pattern },
        { productName: pattern },
      ],
    }).lean();
    return [name, product || null];
  }));
  return new Map(entries.filter(([, product]) => product));
}

function escapeRegex(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function nextRecipeCommentId(comments) {
  const latestNumber = (comments || []).reduce((max, comment) => {
    const id = comment.CommentID || comment.id || '';
    const number = /^RCM\d+$/.test(id) ? Number(id.slice(3)) : 0;
    return Number.isFinite(number) && number > max ? number : max;
  }, 0);
  return `RCM${String(latestNumber + 1).padStart(4, '0')}`;
}

function dateValue(value) {
  const time = value ? new Date(value).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
}

function sortByOrder(left, right) {
  return (left.SortOrder ?? left.sortOrder ?? 0) - (right.SortOrder ?? right.sortOrder ?? 0);
}

function firstImage(images) {
  if (!Array.isArray(images)) {
    return '';
  }
  return images.find((image) => image && !image.startsWith('data:image/') && image.length <= 1000) || '';
}

module.exports = router;
