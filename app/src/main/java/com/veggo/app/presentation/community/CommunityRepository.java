package com.veggo.app.presentation.community;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.data.local.entity.CommunityCategoryEntity;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityCookbookEntity;
import com.veggo.app.data.local.entity.CommunityCookbookRecipeEntity;
import com.veggo.app.data.local.entity.CommunityFollowEntity;
import com.veggo.app.data.local.entity.CommunityRecipeCommentEntity;
import com.veggo.app.data.local.entity.CommunityRecipeDetailEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;
import com.veggo.app.data.local.entity.CommunityRecipeGalleryEntity;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;
import com.veggo.app.data.local.entity.ProductEntity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    private static final String ASSET_FILE = "community_cooking.json";
    private static final String POSTS_ASSET_FILE = "community_posts.json";
    public static final String RELATION_FOLLOWING = "following";
    public static final String RELATION_FOLLOWER = "follower";
    public static final String ACCOUNT_ID = "account-thuc-quyen";
    public static final String ACCOUNT_NAME = "Th\u1ee5c Quy\u00ean";
    public static final String ACCOUNT_LOCATION = "Th\u1ee7 \u0110\u1ee9c";
    public static final String ACCOUNT_AVATAR_URL = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80";
    public static final String ACCOUNT_HERO_URL = "https://images.unsplash.com/photo-1506368249639-73a05d6f6488?auto=format&fit=crop&w=1200&q=80";
    public static final String ACCOUNT_RECIPE_CHEF_ID = "santana";

    private final Context context;
    private final VeggoDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CommunityRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = VeggoDatabase.getInstance(this.context);
    }

    public void loadHome(Callback<CommunityData> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(new CommunityData(
                    database.communityDao().getCategories(),
                    database.communityDao().getChefs(4),
                    database.communityDao().getRecipes(4)
            ));
        });
    }

    public void loadCategories(Callback<List<CommunityCategoryEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getCategories());
        });
    }

    public void loadChefs(Callback<List<CommunityChefEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getChefs());
        });
    }

    public void loadRecipes(Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getRecipes());
        });
    }

    public void loadRecipesByCategory(String categoryId, Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getRecipesByCategory(categoryId));
        });
    }

    public void loadRecipesByChef(String chefId, Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getRecipesByChef(chefId));
        });
    }

    public void loadAccountRecipes(Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getRecipesByChef(ACCOUNT_RECIPE_CHEF_ID));
        });
    }

    public void loadCookbooks(String accountId, Callback<List<CommunityCookbookEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getCookbooks(accountId));
        });
    }

    public void loadCookbookRecipes(String cookbookId, Callback<CookbookData> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(new CookbookData(
                    database.communityDao().getCookbook(cookbookId),
                    database.communityDao().getRecipesByCookbook(cookbookId)
            ));
        });
    }

    public void addRecipeToCookbook(String cookbookId, String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            ensureSeeded();
            boolean existed = database.communityDao().countCookbookRecipe(cookbookId, recipeId) > 0;
            database.runInTransaction(() -> {
                database.communityDao().insertCookbookRecipe(new CommunityCookbookRecipeEntity(cookbookId, recipeId, 999));
                if (!existed) {
                    database.communityDao().incrementCookbookRecipeCount(cookbookId);
                }
            });
            if (callback != null) {
                callback.onResult(true);
            }
        });
    }

    public void createCookbook(String title, String description, String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            ensureSeeded();
            CommunityRecipeEntity recipe = database.communityDao().getRecipe(recipeId);
            String id = "cookbook-" + UUID.randomUUID().toString();
            String imageUrl = recipe == null ? CommunityRepository.ACCOUNT_HERO_URL : recipe.getImageUrl();
            database.runInTransaction(() -> {
                database.communityDao().insertCookbook(new CommunityCookbookEntity(
                        id,
                        ACCOUNT_ID,
                        title,
                        1,
                        imageUrl
                ));
                database.communityDao().insertCookbookRecipe(new CommunityCookbookRecipeEntity(id, recipeId, 0));
            });
            if (callback != null) {
                callback.onResult(true);
            }
        });
    }

    public void loadFollows(String chefId, String relationType, Callback<List<CommunityFollowEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.communityDao().getFollows(chefId, relationType));
        });
    }

    public void loadFollowCounts(String chefId, Callback<FollowCounts> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(new FollowCounts(
                    database.communityDao().countFollows(chefId, RELATION_FOLLOWING),
                    database.communityDao().countFollows(chefId, RELATION_FOLLOWER)
            ));
        });
    }

    public void loadRecipeDetail(String recipeId, Callback<RecipeDetailData> callback) {
        executor.execute(() -> {
            ensureSeeded();
            CommunityRecipeEntity recipe = database.communityDao().getRecipe(recipeId);
            CommunityChefEntity chef = recipe == null ? null : database.communityDao().getChef(recipe.getChefId());
            CommunityRecipeDetailEntity detail = database.communityDao().getRecipeDetail(recipeId);
            List<CommunityRecipeIngredientEntity> ingredients = database.communityDao().getRecipeIngredients(recipeId);
            List<String> productIds = new ArrayList<>();
            for (CommunityRecipeIngredientEntity ingredient : ingredients) {
                productIds.add(ingredient.getProductId());
            }
            Map<String, ProductEntity> productsById = new HashMap<>();
            if (!productIds.isEmpty()) {
                for (ProductEntity product : database.productDao().getProductsByIds(productIds)) {
                    productsById.put(product.getId(), product);
                }
            }
            callback.onResult(new RecipeDetailData(
                    recipe,
                    chef,
                    detail,
                    ingredients,
                    productsById,
                    database.communityDao().getRecipeGallery(recipeId),
                    database.communityDao().getRecipeComments(recipeId)
            ));
        });
    }

    private void ensureSeeded() {
        if (database.communityDao().countCategories() > 0) {
            return;
        }
        try (InputStream inputStream = context.getAssets().open(ASSET_FILE);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CommunityData data = new Gson().fromJson(reader, CommunityData.class);
            database.runInTransaction(() -> {
                database.communityDao().insertCategories(data.categories);
                database.communityDao().insertChefs(data.chefs);
                database.communityDao().insertRecipes(data.recipes);
                if (data.cookbooks != null) {
                    database.communityDao().insertCookbooks(data.cookbooks);
                }
                if (data.cookbookRecipes != null) {
                    database.communityDao().insertCookbookRecipes(data.cookbookRecipes);
                }
                if (data.follows != null) {
                    database.communityDao().insertFollows(data.follows);
                }
                if (data.recipeDetails != null) {
                    database.communityDao().insertRecipeDetails(data.recipeDetails);
                }
                if (data.recipeIngredients != null) {
                    database.communityDao().insertRecipeIngredients(data.recipeIngredients);
                }
                if (data.recipeGalleries != null) {
                    database.communityDao().insertRecipeGalleries(data.recipeGalleries);
                }
                database.communityDao().insertRecipeComments(readComments(data.recipes));
            });
        } catch (Exception ignored) {
        }
    }

    private List<CommunityRecipeCommentEntity> readComments(List<CommunityRecipeEntity> recipes) {
        List<CommunityRecipeCommentEntity> comments = new ArrayList<>();
        if (recipes == null || recipes.isEmpty()) {
            return comments;
        }
        String[] names = {"Melanie Rose", "Jonathan Jose", "Nicky", "Moon Star", "Melanie", "Amelia Melanes"};
        String[] avatars = {
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160&h=160&fit=crop&crop=faces",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&h=160&fit=crop&crop=faces",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&h=160&fit=crop&crop=faces",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=160&h=160&fit=crop&crop=faces",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=160&h=160&fit=crop&crop=faces",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160&h=160&fit=crop&crop=faces"
        };
        try (InputStream inputStream = context.getAssets().open(POSTS_ASSET_FILE);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonArray posts = new Gson().fromJson(reader, JsonArray.class);
            int index = 0;
            for (JsonElement postElement : posts) {
                JsonObject post = postElement.getAsJsonObject();
                JsonArray postComments = post.getAsJsonArray("CommentsDetail");
                if (postComments == null) {
                    continue;
                }
                for (JsonElement commentElement : postComments) {
                    JsonObject comment = commentElement.getAsJsonObject();
                    String recipeId = recipes.get(index % recipes.size()).getId();
                    String id = stringValue(comment, "CommunityCommentID");
                    comments.add(new CommunityRecipeCommentEntity(
                            recipeId + "-" + id,
                            recipeId,
                            names[index % names.length],
                            avatars[index % avatars.length],
                            stringValue(comment, "Content"),
                            120 + (index * 31 % 360),
                            index
                    ));
                    index++;
                }
            }
        } catch (Exception ignored) {
        }
        return comments;
    }

    private String stringValue(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    public static class CommunityData {
        public List<CommunityCategoryEntity> categories;
        public List<CommunityChefEntity> chefs;
        public List<CommunityRecipeEntity> recipes;
        public List<CommunityCookbookEntity> cookbooks;
        public List<CommunityCookbookRecipeEntity> cookbookRecipes;
        public List<CommunityFollowEntity> follows;
        public List<CommunityRecipeDetailEntity> recipeDetails;
        public List<CommunityRecipeIngredientEntity> recipeIngredients;
        public List<CommunityRecipeGalleryEntity> recipeGalleries;

        public CommunityData(
                List<CommunityCategoryEntity> categories,
                List<CommunityChefEntity> chefs,
                List<CommunityRecipeEntity> recipes
        ) {
            this.categories = categories;
            this.chefs = chefs;
            this.recipes = recipes;
        }
    }

    public static class RecipeDetailData {
        public final CommunityRecipeEntity recipe;
        public final CommunityChefEntity chef;
        public final CommunityRecipeDetailEntity detail;
        public final List<CommunityRecipeIngredientEntity> ingredients;
        public final Map<String, ProductEntity> productsById;
        public final List<CommunityRecipeGalleryEntity> galleries;
        public final List<CommunityRecipeCommentEntity> comments;

        public RecipeDetailData(
                CommunityRecipeEntity recipe,
                CommunityChefEntity chef,
                CommunityRecipeDetailEntity detail,
                List<CommunityRecipeIngredientEntity> ingredients,
                Map<String, ProductEntity> productsById,
                List<CommunityRecipeGalleryEntity> galleries,
                List<CommunityRecipeCommentEntity> comments
        ) {
            this.recipe = recipe;
            this.chef = chef;
            this.detail = detail;
            this.ingredients = ingredients;
            this.productsById = productsById;
            this.galleries = galleries;
            this.comments = comments;
        }
    }

    public static class FollowCounts {
        public final int following;
        public final int followers;

        public FollowCounts(int following, int followers) {
            this.following = following;
            this.followers = followers;
        }
    }

    public static class CookbookData {
        public final CommunityCookbookEntity cookbook;
        public final List<CommunityRecipeEntity> recipes;

        public CookbookData(CommunityCookbookEntity cookbook, List<CommunityRecipeEntity> recipes) {
            this.cookbook = cookbook;
            this.recipes = recipes;
        }
    }
}
