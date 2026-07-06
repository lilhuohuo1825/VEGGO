package com.veggo.app.presentation.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.utils.JsonUtils;
import com.veggo.app.core.utils.ProductDisplayValidator;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.FlashSaleResponseDto;
import com.veggo.app.data.remote.dto.HomeProductResponse;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.repository.PromotionRepositoryImpl;
import com.veggo.app.domain.model.Banner;
import com.veggo.app.domain.model.Blog;
import com.veggo.app.domain.model.Category;
import com.veggo.app.domain.model.FlashSale;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Utility;
import com.veggo.app.domain.repository.PromotionRepository;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.remote.dto.CategoryDto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    // ─── Constants: tên tab gửi lên API ─────────────────────────────────
    public static final String TAB_POPULAR    = "popular";
    public static final String TAB_TRENDING   = "trending";
    public static final String TAB_NEWEST     = "newest";
    public static final String TAB_TOP_RATED  = "top_rated";
    public static final String TAB_PRICE_DESC = "price_desc";
    public static final String TAB_PRICE_ASC  = "price_asc";
    // ─── LiveData ────────────────────────────────────────────────────────
    private final PromotionRepository promotionRepository;
    private final ProductApi productApi;

    private final MutableLiveData<List<Product>> _products = new MutableLiveData<>();
    public LiveData<List<Product>> getProducts() { return _products; }

    /** Tab đang được chọn (default: popular) */
    private final MutableLiveData<String> _currentTab = new MutableLiveData<>(TAB_POPULAR);
    public LiveData<String> getCurrentTab() { return _currentTab; }

    /** true khi đang tải sản phẩm từ API */
    private final MutableLiveData<Boolean> _loadingProducts = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoadingProducts() { return _loadingProducts; }

    /** Thông báo lỗi (null nếu không có lỗi) */
    private final MutableLiveData<String> _productError = new MutableLiveData<>(null);
    public LiveData<String> getProductError() { return _productError; }

    private final MutableLiveData<Boolean> _isLastPage = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLastPage() { return _isLastPage; }

    private final MutableLiveData<List<Blog>> _blogs = new MutableLiveData<>();
    public LiveData<List<Blog>> getBlogs() { return _blogs; }

    private final MutableLiveData<List<Utility>> _utilities = new MutableLiveData<>();
    public LiveData<List<Utility>> getUtilities() { return _utilities; }

    public LiveData<List<Banner>> getBanners() { return promotionRepository.getAppBanners(); }

    private final MutableLiveData<List<FlashSale>> _flashSales = new MutableLiveData<>();
    public LiveData<List<FlashSale>> getFlashSales() { return _flashSales; }

    private final MutableLiveData<List<Recipe>> _recipes = new MutableLiveData<>();
    public LiveData<List<Recipe>> getRecipes() { return _recipes; }

    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>();
    public LiveData<List<Category>> getCategories() { return _categories; }

    private static final int PAGE_SIZE = 24;
    private int currentSkip = 0;

    // ─── Constructor ─────────────────────────────────────────────────────
    public HomeViewModel() {
        promotionRepository = new PromotionRepositoryImpl();
        productApi = ApiClient.createService(ProductApi.class);
    }

    // ─── Public API ──────────────────────────────────────────────────────

    public void loadHomeData(Context context) {
        Gson gson = new Gson();

        // Blogs
        String blogsJson = JsonUtils.loadJSONFromAsset(context, "blogs.json");
        if (blogsJson != null) {
            Type blogListType = new TypeToken<List<Blog>>() {}.getType();
            _blogs.setValue(gson.fromJson(blogsJson, blogListType));
        }

        loadUtilities();
        loadBannersFromApi();
        loadFlashSales(context);
        loadRecipes(context);
        loadCategories(context);

        // Tải sản phẩm từ backend (tab mặc định: popular)
        loadProductsFromApi(TAB_POPULAR, true);
    }

    public void selectTab(String tab) {
        if (tab.equals(_currentTab.getValue())) {
            return;
        }
        _currentTab.setValue(tab);
        currentSkip = 0;
        _isLastPage.setValue(false);
        loadProductsFromApi(tab, true);
    }

    /**
     * Gọi khi người dùng bấm "Xem thêm".
     */
    public void loadMoreProducts() {
        if (Boolean.TRUE.equals(_isLastPage.getValue()) || Boolean.TRUE.equals(_loadingProducts.getValue())) return;
        loadProductsFromApi(_currentTab.getValue(), false);
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    private void loadProductsFromApi(String tab, boolean isRefresh) {
        _loadingProducts.setValue(true);
        if (isRefresh) _productError.setValue(null);

        productApi.getHomeProducts(tab, PAGE_SIZE, currentSkip).enqueue(new Callback<HomeProductResponse>() {
            @Override
            public void onResponse(Call<HomeProductResponse> call, Response<HomeProductResponse> response) {
                _loadingProducts.setValue(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<ProductDto> dtos = response.body().getData();
                    List<Product> newProducts = new ArrayList<>();
                    if (dtos != null) {
                        for (ProductDto dto : dtos) {
                            if (!ProductDisplayValidator.isDisplayable(dto)) {
                                continue;
                            }
                            newProducts.add(mapDtoToProduct(dto));
                        }
                    }
                    
                    _isLastPage.setValue(newProducts.size() < PAGE_SIZE);
                    currentSkip += newProducts.size();

                    if (isRefresh) {
                        _products.setValue(newProducts);
                    } else {
                        List<Product> current = _products.getValue();
                        if (current == null) current = new ArrayList<>();
                        List<Product> updated = new ArrayList<>(current);
                        updated.addAll(newProducts);
                        _products.setValue(updated);
                    }
                } else {
                    if (isRefresh) _productError.setValue("Không thể tải sản phẩm. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<HomeProductResponse> call, Throwable t) {
                _loadingProducts.setValue(false);
                if (isRefresh) _productError.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /** Chuyển ProductDto → Product (domain model) */
    private Product mapDtoToProduct(ProductDto dto) {
        return new Product(
                dto.getId(),
                dto.getName(),
                dto.getSku(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getImageUrl(),
                dto.getWeightOptions(),
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent(),
                dto.getCategoryId(),
                dto.getSubcategoryId(),
                dto.getCarbonSavingPoint()
        );
    }

    private void loadUtilities() {
        List<Utility> utilityList = new ArrayList<>();
        utilityList.add(new Utility("1", "Tủ lạnh", R.drawable.ic_refrigerator));
        utilityList.add(new Utility("2", "Trợ lý AI", R.drawable.ic_ai));
        utilityList.add(new Utility("3", "Yêu thích", R.drawable.ic_heart_outline_white));
        utilityList.add(new Utility("4", "Điểm xanh", R.drawable.ic_yellow_cert));
        utilityList.add(new Utility("5", "Bài viết", R.drawable.ic_blog));
        _utilities.setValue(utilityList);
    }

    private void loadBannersFromApi() {
        promotionRepository.refreshBanners();
    }

    private void loadFlashSales(Context context) {
        PromotionApi promotionApi = ApiClient.createService(PromotionApi.class);
        promotionApi.getFlashSales().enqueue(new Callback<FlashSaleResponseDto>() {
            @Override
            public void onResponse(Call<FlashSaleResponseDto> call, Response<FlashSaleResponseDto> response) {
                FlashSaleResponseDto body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null && !body.getData().isEmpty()) {
                    List<FlashSale> flashSaleList = new ArrayList<>();
                    for (FlashSaleResponseDto.FlashSaleItemDto item : body.getData()) {
                        flashSaleList.add(new FlashSale(
                                item.getId(),
                                item.getName(),
                                item.getPrice(),
                                item.getOriginalPrice(),
                                item.getUnit(),
                                item.getDiscount(),
                                0,
                                item.getImageUrl(),
                                item.getRating()
                        ));
                    }
                    _flashSales.setValue(flashSaleList);
                } else {
                    loadFallbackFlashSales(context);
                }
            }

            @Override
            public void onFailure(Call<FlashSaleResponseDto> call, Throwable t) {
                loadFallbackFlashSales(context);
            }
        });
    }

    private void loadFallbackFlashSales(Context context) {
        new Thread(() -> {
            List<FlashSale> flashSaleList = buildAssetFlashSales(context);
            _flashSales.postValue(flashSaleList);
        }).start();
    }

    private List<FlashSale> buildAssetFlashSales(Context context) {
        AssetRepository repository = new AssetRepository(context);
        List<AssetModels.Promotion> promotions = repository.getPromotions();
        List<AssetModels.PromotionTarget> targets = repository.getPromotionTargets();
        List<AssetModels.Product> products = repository.getProducts();

        Map<String, AssetModels.Promotion> activeFlashPromoById = new HashMap<>();
        for (AssetModels.Promotion promotion : promotions) {
            if (isAssetFlashSalePromotion(promotion)) {
                activeFlashPromoById.put(promotion.promotionId, promotion);
            }
        }

        Map<String, AssetModels.Product> productBySku = new HashMap<>();
        for (AssetModels.Product product : products) {
            if (product.sku != null && !product.sku.trim().isEmpty()) {
                productBySku.put(product.sku, product);
            }
        }

        List<FlashSale> flashSaleList = new ArrayList<>();
        Set<String> addedSkus = new HashSet<>();
        for (AssetModels.PromotionTarget target : targets) {
            AssetModels.Promotion promotion = activeFlashPromoById.get(target.promotionId);
            if (promotion == null || !"Product".equalsIgnoreCase(target.targetType) || target.targetRef == null) {
                continue;
            }
            for (String sku : target.targetRef) {
                if (sku == null || addedSkus.contains(sku)) continue;
                AssetModels.Product product = productBySku.get(sku);
                if (product == null || product.status != null && !"Active".equalsIgnoreCase(product.status)) {
                    continue;
                }
                flashSaleList.add(new FlashSale(
                        assetProductId(product),
                        product.productName,
                        calculateAssetSalePrice(product.price, promotion),
                        product.price,
                        product.weight != null ? product.weight : product.unit,
                        formatAssetDiscount(promotion),
                        0,
                        firstImage(product.image),
                        (float) product.rating
                ));
                addedSkus.add(sku);
            }
        }
        return flashSaleList;
    }

    private boolean isAssetFlashSalePromotion(AssetModels.Promotion promotion) {
        if (!isActiveAssetPromotion(promotion)) return false;
        return "flashsale".equalsIgnoreCase(promotion.promotionKind == null ? "" : promotion.promotionKind.trim());
    }

    private boolean isActiveAssetPromotion(AssetModels.Promotion promotion) {
        if (promotion == null || promotion.promotionId == null || promotion.promotionId.trim().isEmpty()) return false;
        if (promotion.status != null && ("inactive".equalsIgnoreCase(promotion.status) || "expired".equalsIgnoreCase(promotion.status))) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long start = parseAssetDateMillis(promotion.startDate);
        Long end = parseAssetDateMillis(promotion.endDate);
        return (start == null || start <= now) && (end == null || end >= now);
    }

    private long calculateAssetSalePrice(long originalPrice, AssetModels.Promotion promotion) {
        int discountValue = Math.max(0, promotion.discountValue);
        if ("fixed".equalsIgnoreCase(promotion.discountType)) {
            return Math.max(0, originalPrice - discountValue);
        }
        if ("percent".equalsIgnoreCase(promotion.discountType) || discountValue > 0 && !"buy1get1".equalsIgnoreCase(promotion.discountType)) {
            return Math.max(0, Math.round(originalPrice * (100 - Math.min(100, discountValue)) / 100f));
        }
        return originalPrice;
    }

    private String formatAssetDiscount(AssetModels.Promotion promotion) {
        int discountValue = Math.max(0, promotion.discountValue);
        if ("fixed".equalsIgnoreCase(promotion.discountType)) {
            return "-" + String.format(java.util.Locale.US, "%,d", discountValue).replace(',', '.') + "đ";
        }
        if ("buy1get1".equalsIgnoreCase(promotion.discountType)) {
            return "Mua 1 tặng 1";
        }
        return "-" + discountValue + "%";
    }

    private Long parseAssetDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = format.parse(value);
            return date != null ? date.getTime() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String assetProductId(AssetModels.Product product) {
        return product.objectId != null && !product.objectId.trim().isEmpty() ? product.objectId : product.sku;
    }

    private String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? "" : images.get(0);
    }

    private void loadRecipes(Context context) {
        String dishesJson = JsonUtils.loadJSONFromAsset(context, "dishes.json");
        String instructionsJson = JsonUtils.loadJSONFromAsset(context, "instructions.json");

        if (dishesJson == null || instructionsJson == null) {
            List<Recipe> recipeList = new ArrayList<>();
            recipeList.add(new Recipe("1", "Salad ức gà áp chảo", "15 mins", R.drawable.onboarding_1, null));
            _recipes.setValue(recipeList);
            return;
        }

        Gson gson = new Gson();
        Type dishListType = new TypeToken<List<AssetModels.Dish>>() {}.getType();
        Type instructionListType = new TypeToken<List<AssetModels.Instruction>>() {}.getType();

        List<AssetModels.Dish> dishes = gson.fromJson(dishesJson, dishListType);
        List<AssetModels.Instruction> instructions = gson.fromJson(instructionsJson, instructionListType);

        Map<String, AssetModels.Instruction> instructionMap = new HashMap<>();
        if (instructions != null) {
            for (AssetModels.Instruction inst : instructions) {
                instructionMap.put(inst.id, inst);
            }
        }

        List<Recipe> recipeList = new ArrayList<>();
        if (dishes != null) {
            for (AssetModels.Dish dish : dishes) {
                AssetModels.Instruction inst = instructionMap.get(dish.id);
                if (inst != null) {
                    String videoId = extractYoutubeId(dish.video);
                    String thumbnailUrl = videoId != null
                            ? "https://img.youtube.com/vi/" + videoId + "/0.jpg" : null;
                    recipeList.add(new Recipe(dish.id, inst.dishName, inst.cookingTime,
                            0, thumbnailUrl, countIngredients(dish.ingredients)));
                }
            }
        }
        Collections.shuffle(recipeList);
        if (recipeList.size() > 10) {
            recipeList = new ArrayList<>(recipeList.subList(0, 10));
        }
        _recipes.setValue(recipeList);
    }

    private String extractYoutubeId(String url) {
        if (url == null) return null;
        try {
            if (url.contains("/embed/")) {
                String temp = url.split("/embed/")[1];
                if (temp.contains("?")) return temp.split("\\?")[0];
                return temp;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private int countIngredients(String ingredients) {
        if (ingredients == null || ingredients.trim().isEmpty()) {
            return 0;
        }
        String normalized = ingredients
                .replace("\r", "\n")
                .replace(";", ",")
                .replace("•", ",");
        String[] parts = normalized.split(",|\\n");
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void loadCategories(Context context) {
        String categoriesJson = JsonUtils.loadJSONFromAsset(context, "categories.json");
        if (categoriesJson != null) {
            Gson gson = new Gson();
            Type categoryDtoListType = new TypeToken<List<CategoryDto>>() {}.getType();
            List<CategoryDto> dtos = gson.fromJson(categoriesJson, categoryDtoListType);

            List<Category> categoryList = new ArrayList<>();
            for (CategoryDto dto : dtos) {
                int iconRes = getIconForCategory(dto.getCategoryID());
                categoryList.add(new Category(dto.getCategoryID(), dto.getCategoryName(), iconRes));
            }
            _categories.setValue(categoryList);
        } else {
            List<Category> fallbackList = new ArrayList<>();
            fallbackList.add(new Category("CAT008", "Trái cây", R.drawable.ic_fruit));
            fallbackList.add(new Category("CAT003", "Rau củ", R.drawable.ic_vegetable));
            _categories.setValue(fallbackList);
        }
    }

    private int getIconForCategory(String categoryId) {
        if (categoryId == null) return R.drawable.ic_vegetable;
        switch (categoryId) {
            case "CAT001": return R.drawable.ic_coffee_green;
            case "CAT002": return R.drawable.ic_grain_dark;
            case "CAT003": return R.drawable.ic_vegetable;
            case "CAT004": return R.drawable.ic_seaweed;
            case "CAT005": return R.drawable.ic_nutritous;
            case "CAT006": return R.drawable.ic_dryfood;
            case "CAT007": return R.drawable.ic_leaf;
            case "CAT008": return R.drawable.ic_fruit;
            default:       return R.drawable.ic_vegetable;
        }
    }
}
