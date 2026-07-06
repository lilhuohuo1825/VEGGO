package com.veggo.app.presentation.community;

import android.content.Context;
import android.util.Log;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
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
import com.veggo.app.data.remote.api.CommunityApi;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.dto.ProductDto;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class CommunityRepository {
    private static final String TAG = "CommunityRepository";

    public interface Callback<T> {
        void onResult(T result);
    }

    public static final String RELATION_FOLLOWING = "following";
    public static final String RELATION_FOLLOWER = "follower";
    public static final String ACCOUNT_ID = "account-thuc-quyen";
    public static final String ACCOUNT_NAME = "Thục Quyên";
    public static final String ACCOUNT_LOCATION = "Thủ Đức";
    public static final String ACCOUNT_AVATAR_URL = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80";
    public static final String ACCOUNT_HERO_URL = "https://images.unsplash.com/photo-1506368249639-73a05d6f6488?auto=format&fit=crop&w=1200&q=80";
    public static final String ACCOUNT_RECIPE_CHEF_ID = "santana";
    private static final String COMMUNITY_ASSET_FILE = "community_cooking.json";

    private final CommunityApi api;
    private final ProductApi productApi;
    private final AppPreferences preferences;
    private final AssetJsonLoader assetLoader;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CommunityRepository(Context context) {
        this.api = ApiClient.createService(CommunityApi.class);
        this.productApi = ApiClient.createService(ProductApi.class);
        this.preferences = new AppPreferences(context);
        this.assetLoader = new AssetJsonLoader(context);
    }

    public void loadHome(Callback<CommunityData> callback) {
        executor.execute(() -> {
            CommunityData remote = execute(api.getHome());
            callback.onResult(withAssetFallback(remote));
        });
    }

    public void loadCategories(Callback<List<CommunityCategoryEntity>> callback) {
        executor.execute(() -> {
            List<CommunityCategoryEntity> remote = execute(api.getCategories());
            callback.onResult(isEmpty(remote) ? assetCommunityData().categories : remote);
        });
    }

    public void loadProducts(Callback<List<ProductDto>> callback) {
        executor.execute(() -> {
            List<ProductDto> remote = execute(productApi.getProducts("true"));
            callback.onResult(isEmpty(remote) ? assetProducts() : remote);
        });
    }

    public void loadChefs(Callback<List<CommunityChefEntity>> callback) {
        executor.execute(() -> {
            List<CommunityChefEntity> remote = execute(api.getChefs());
            callback.onResult(isEmpty(remote) ? assetCommunityData().chefs : remote);
        });
    }

    public void loadUser(String customerId, Callback<CommunityChefEntity> callback) {
        request(api.getUser(customerId), null, callback);
    }

    public void loadRecipes(Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            List<CommunityRecipeEntity> remote = execute(api.getRecipes(null, null, null));
            callback.onResult(isEmpty(remote) ? assetCommunityData().recipes : remote);
        });
    }

    public void loadRecipesByCategory(String categoryId, Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            List<CommunityRecipeEntity> remote = execute(api.getRecipes(categoryId, null, null));
            callback.onResult(isEmpty(remote) ? filterAssetRecipes(categoryId, null) : remote);
        });
    }

    public void loadRecipesByChef(String chefId, Callback<List<CommunityRecipeEntity>> callback) {
        executor.execute(() -> {
            List<CommunityRecipeEntity> remote = execute(api.getRecipes(null, chefId, null));
            callback.onResult(isEmpty(remote) ? filterAssetRecipes(null, chefId) : remote);
        });
    }

    public void loadDraft(Callback<DraftResponse> callback) {
        request(api.getRecipeDraft(currentCustomerId()), new DraftResponse(), callback);
    }

    public void saveDraft(RecipeDraft draft, Callback<Boolean> callback) {
        draft.customerId = currentCustomerId();
        request(api.saveRecipeDraft(draft), null, saved -> callback.onResult(saved != null));
    }

    public void deleteDraft(String draftId, Callback<Boolean> callback) {
        executor.execute(() -> {
            OkResponse response = execute(api.deleteRecipeDraft(draftId, currentCustomerId()));
            callback.onResult(response != null && response.ok);
        });
    }

    public void publishRecipe(RecipeDraft draft, Callback<Boolean> callback) {
        executor.execute(() -> {
            draft.customerId = currentCustomerId();
            preparePublicImageUrls(draft);
            CommunityRecipeEntity recipe = execute(api.publishRecipe(draft));
            callback.onResult(recipe != null);
        });
    }

    public void updateRecipe(String recipeId, RecipeDraft draft, Callback<Boolean> callback) {
        executor.execute(() -> {
            draft.customerId = currentCustomerId();
            preparePublicImageUrls(draft);
            CommunityRecipeEntity recipe = execute(api.updateRecipe(recipeId, draft));
            callback.onResult(recipe != null);
        });
    }

    public void deleteRecipe(String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            OkResponse response = execute(api.deleteRecipe(recipeId, currentCustomerId()));
            callback.onResult(response != null && response.ok);
        });
    }

    public void loadAccountRecipes(Callback<List<CommunityRecipeEntity>> callback) {
        loadRecipesByChef(currentCustomerId(), callback);
    }

    public void loadCookbooks(String accountId, Callback<List<CommunityCookbookEntity>> callback) {
        String customerId = firstNonBlank(accountId, currentCustomerId());
        executor.execute(() -> {
            List<CommunityCookbookEntity> remote = execute(api.getCookbooks(customerId));
            callback.onResult(isEmpty(remote) ? filterAssetCookbooks(customerId) : remote);
        });
    }

    public void loadCookbookRecipes(String cookbookId, Callback<CookbookData> callback) {
        executor.execute(() -> {
            CookbookData remote = execute(api.getCookbook(cookbookId));
            callback.onResult(remote == null || remote.cookbook == null ? assetCookbookData(cookbookId) : remote);
        });
    }

    public void addRecipeToCookbook(String cookbookId, String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            boolean success = execute(api.addRecipeToCookbook(cookbookId, new AddRecipeToCookbookRequest(recipeId))) != null;
            if (callback != null) {
                callback.onResult(success);
            }
        });
    }

    public void removeRecipeFromCookbooks(String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            OkResponse response = execute(api.removeRecipeFromCookbooks(recipeId, currentCustomerId()));
            callback.onResult(response != null && response.ok);
        });
    }

    public void createCookbook(String title, String description, String recipeId, Callback<Boolean> callback) {
        executor.execute(() -> {
            CreateCookbookRequest request = new CreateCookbookRequest(currentCustomerId(), title, description, recipeId);
            boolean success = execute(api.createCookbook(request)) != null;
            if (callback != null) {
                callback.onResult(success);
            }
        });
    }

    public void loadFollows(String chefId, String relationType, Callback<List<CommunityFollowEntity>> callback) {
        request(api.getFollows(chefId, relationType), new ArrayList<>(), callback);
    }

    public void loadFollowCounts(String chefId, Callback<FollowCounts> callback) {
        request(api.getFollowCounts(chefId, currentCustomerId()), new FollowCounts(), callback);
    }

    public void toggleFollow(String chefId, Callback<FollowCounts> callback) {
        request(api.toggleFollow(new ToggleFollowRequest(currentCustomerId(), chefId)), new FollowCounts(), callback);
    }

    public void isRecipeSaved(String recipeId, Callback<Boolean> callback) {
        request(api.getRecipeSavedStatus(recipeId, currentCustomerId()), null, status ->
                callback.onResult(status == null ? isRecipeSavedInAsset(recipeId, currentCustomerId()) : status.saved));
    }

    public void loadRecipeDetail(String recipeId, Callback<RecipeDetailData> callback) {
        request(api.getRecipeDetail(recipeId, currentCustomerId()), new RecipeDetailData(), callback);
    }

    public void createComment(String recipeId, String content, Callback<CommunityRecipeCommentEntity> callback) {
        request(api.createComment(new CreateCommentRequest(recipeId, currentCustomerId(), content)), null, callback);
    }

    public void toggleCommentLike(String commentId, Callback<CommunityRecipeCommentEntity> callback) {
        request(api.toggleCommentLike(commentId, new ToggleCommentLikeRequest(currentCustomerId())), null, callback);
    }

    private <T> void request(Call<T> call, T fallback, Callback<T> callback) {
        executor.execute(() -> {
            T result = execute(call);
            callback.onResult(result == null ? fallback : result);
        });
    }

    private <T> T execute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException ignored) {
        } catch (RuntimeException exception) {
            Log.w(TAG, "Community request failed", exception);
        }
        return null;
    }

    private List<ProductDto> assetProducts() {
        List<ProductDto> result = new ArrayList<>();
        try {
            List<AssetModels.Product> products = assetLoader.readList(AssetFiles.PRODUCTS, AssetModels.Product.class);
            for (AssetModels.Product item : products) {
                ProductDto dto = new ProductDto();
                dto.setId(firstNonEmpty(item.objectId, item.sku));
                dto.setName(firstNonEmpty(item.productName, "Sản phẩm Veggo"));
                dto.setBrand(item.brand);
                dto.setSku(item.sku);
                dto.setUnit(item.unit);
                dto.setWeight(item.weight);
                dto.setPrice(item.price);
                dto.setOriginalPrice(item.basePrice > 0 ? item.basePrice : item.price);
                dto.setImage(item.image);
                dto.setOrigin(item.origin);
                dto.setStatus(item.status);
                dto.setRating((float) item.rating);
                dto.setSoldCount(item.purchaseCount);
                dto.setLiked(item.liked);
                dto.setStock(item.stock);
                dto.setCategoryId(item.categoryId);
                dto.setSubcategoryId(item.subcategoryId);
                dto.setCarbonSavingPoint(item.carbonSavingPoint);
                dto.setEmissionFactor(item.emissionFactor);
                result.add(dto);
            }
        } catch (IOException exception) {
            Log.w(TAG, "Product asset fallback failed", exception);
        }
        return result;
    }

    private void preparePublicImageUrls(RecipeDraft draft) {
        if (draft.imageUrls == null || draft.imageUrls.isEmpty()) {
            return;
        }
        List<String> publicUrls = new ArrayList<>();
        List<MultipartBody.Part> uploadParts = new ArrayList<>();
        for (String imageUrl : draft.imageUrls) {
            if (isBlank(imageUrl)) {
                continue;
            }
            if (isPublicUrl(imageUrl)) {
                publicUrls.add(imageUrl);
                continue;
            }
            File file = new File(imageUrl);
            if (!file.exists()) {
                continue;
            }
            RequestBody body = RequestBody.create(file, MediaType.parse("image/jpeg"));
            uploadParts.add(MultipartBody.Part.createFormData("images", file.getName(), body));
        }
        if (!uploadParts.isEmpty()) {
            ImageUploadResponse response = execute(api.uploadImages(uploadParts));
            if (response != null && response.images != null) {
                publicUrls.addAll(response.images);
            }
        }
        draft.imageUrls = publicUrls;
        draft.imageUrl = publicUrls.isEmpty() ? "" : publicUrls.get(0);
    }

    private boolean isPublicUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    public String currentCustomerId() {
        return firstNonBlank(preferences.getCustomerId(), ACCOUNT_ID);
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CommunityData emptyCommunityData() {
        CommunityData data = new CommunityData();
        data.categories = new ArrayList<>();
        data.chefs = new ArrayList<>();
        data.recipes = new ArrayList<>();
        return data;
    }

    private CommunityData withAssetFallback(CommunityData remote) {
        CommunityData data = remote == null ? emptyCommunityData() : remote;
        CommunityData asset = null;
        if (isEmpty(data.categories)) {
            asset = assetCommunityData();
            data.categories = asset.categories;
        }
        if (isEmpty(data.chefs)) {
            asset = asset == null ? assetCommunityData() : asset;
            data.chefs = asset.chefs;
        }
        if (isEmpty(data.recipes)) {
            asset = asset == null ? assetCommunityData() : asset;
            data.recipes = asset.recipes;
        }
        return data;
    }

    private CommunityData assetCommunityData() {
        try {
            CommunityData data = assetLoader.readObject(COMMUNITY_ASSET_FILE, CommunityData.class);
            return data == null ? emptyCommunityData() : withNonNullLists(data);
        } catch (IOException ignored) {
            return emptyCommunityData();
        }
    }

    private CommunityData withNonNullLists(CommunityData data) {
        if (data.categories == null) {
            data.categories = new ArrayList<>();
        }
        if (data.chefs == null) {
            data.chefs = new ArrayList<>();
        }
        if (data.recipes == null) {
            data.recipes = new ArrayList<>();
        }
        if (data.cookbooks == null) {
            data.cookbooks = new ArrayList<>();
        }
        if (data.cookbookRecipes == null) {
            data.cookbookRecipes = new ArrayList<>();
        }
        return data;
    }

    private List<CommunityRecipeEntity> filterAssetRecipes(String categoryId, String chefId) {
        List<CommunityRecipeEntity> recipes = assetCommunityData().recipes;
        if (isBlank(categoryId) && isBlank(chefId)) {
            return recipes;
        }
        List<CommunityRecipeEntity> filtered = new ArrayList<>();
        for (CommunityRecipeEntity recipe : recipes) {
            boolean matchesCategory = isBlank(categoryId) || categoryId.equals(recipe.getCategoryId());
            boolean matchesChef = isBlank(chefId) || chefId.equals(recipe.getChefId());
            if (matchesCategory && matchesChef) {
                filtered.add(recipe);
            }
        }
        return filtered;
    }

    private List<CommunityCookbookEntity> filterAssetCookbooks(String customerId) {
        List<CommunityCookbookEntity> result = new ArrayList<>();
        for (CommunityCookbookEntity cookbook : assetCommunityData().cookbooks) {
            String ownerId = firstNonBlank(cookbook.getCustomerId(), cookbook.getAccountId());
            if (customerId.equals(ownerId)) {
                result.add(cookbook);
            }
        }
        return result;
    }

    private CookbookData assetCookbookData(String cookbookId) {
        CommunityData data = assetCommunityData();
        CommunityCookbookEntity selected = null;
        for (CommunityCookbookEntity cookbook : data.cookbooks) {
            if (cookbook.getId().equals(cookbookId)) {
                selected = cookbook;
                break;
            }
        }
        List<CommunityRecipeEntity> recipes = new ArrayList<>();
        if (selected != null) {
            for (CommunityCookbookRecipeEntity link : data.cookbookRecipes) {
                if (!cookbookId.equals(link.getCookbookId())) {
                    continue;
                }
                for (CommunityRecipeEntity recipe : data.recipes) {
                    if (recipe.getId().equals(link.getRecipeId())) {
                        recipes.add(recipe);
                        break;
                    }
                }
            }
        }
        return new CookbookData(selected, recipes);
    }

    private boolean isRecipeSavedInAsset(String recipeId, String customerId) {
        List<CommunityCookbookEntity> cookbooks = filterAssetCookbooks(customerId);
        if (cookbooks.isEmpty()) {
            return false;
        }
        for (CommunityCookbookRecipeEntity link : assetCommunityData().cookbookRecipes) {
            if (!recipeId.equals(link.getRecipeId())) {
                continue;
            }
            for (CommunityCookbookEntity cookbook : cookbooks) {
                if (cookbook.getId().equals(link.getCookbookId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
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

        public CommunityData() {
        }

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
        public CommunityRecipeEntity recipe;
        public CommunityChefEntity chef;
        public CommunityRecipeDetailEntity detail;
        public List<CommunityRecipeIngredientEntity> ingredients = new ArrayList<>();
        public Map<String, ProductEntity> productsById = new HashMap<>();
        public List<CommunityRecipeGalleryEntity> galleries = new ArrayList<>();
        public List<CommunityRecipeCommentEntity> comments = new ArrayList<>();

        public RecipeDetailData() {
        }

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
        public int following;
        public int followers;
        public boolean isFollowing;

        public FollowCounts() {
        }

        public FollowCounts(int following, int followers) {
            this.following = following;
            this.followers = followers;
        }
    }

    public static class SavedStatus {
        public boolean saved;
    }

    public static class CookbookData {
        public CommunityCookbookEntity cookbook;
        public List<CommunityRecipeEntity> recipes = new ArrayList<>();

        public CookbookData() {
        }

        public CookbookData(CommunityCookbookEntity cookbook, List<CommunityRecipeEntity> recipes) {
            this.cookbook = cookbook;
            this.recipes = recipes;
        }
    }

    public static class DraftResponse {
        public boolean hasDraft;
        public RecipeDraft draft;
        public List<RecipeDraft> drafts = new ArrayList<>();
    }

    public static class RecipeDraft {
        public String draftId;
        public String customerId;
        public String title;
        public String categoryId;
        public int timeMinutes;
        public String imageUrl;
        public List<String> imageUrls;
        public String videoUrl;
        public String ingredientsText;
        public List<RecipeIngredientDraft> ingredientItems;
        public String steps;
        public int calories;
        public String saltLevel;
        public String sugarLevel;
        public String createdAt;
        public String updatedAt;
    }

    public static class ImageUploadResponse {
        public List<String> images;
    }

    public static class RecipeIngredientDraft {
        public String productId;
        public String productSku;
        public String displayName;
        public String imageUrl;
        public String quantity;
    }

    public static class CreateCookbookRequest {
        public String customerId;
        public String accountId;
        public String title;
        public String description;
        public String recipeId;

        public CreateCookbookRequest(String customerId, String title, String description, String recipeId) {
            this.customerId = customerId;
            this.accountId = customerId;
            this.title = title;
            this.description = description;
            this.recipeId = recipeId;
        }
    }

    public static class AddRecipeToCookbookRequest {
        public String recipeId;

        public AddRecipeToCookbookRequest(String recipeId) {
            this.recipeId = recipeId;
        }
    }

    public static class ToggleFollowRequest {
        public String followerCustomerId;
        public String followingCustomerId;

        public ToggleFollowRequest(String followerCustomerId, String followingCustomerId) {
            this.followerCustomerId = followerCustomerId;
            this.followingCustomerId = followingCustomerId;
        }
    }

    public static class CreateCommentRequest {
        public String recipeId;
        public String customerId;
        public String content;

        public CreateCommentRequest(String recipeId, String customerId, String content) {
            this.recipeId = recipeId;
            this.customerId = customerId;
            this.content = content;
        }
    }

    public static class ToggleCommentLikeRequest {
        public String customerId;

        public ToggleCommentLikeRequest(String customerId) {
            this.customerId = customerId;
        }
    }

    public static class OkResponse {
        public boolean ok;
    }
}
