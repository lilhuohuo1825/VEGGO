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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        loadFlashSales();
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
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent(),
                dto.getCategoryId(),
                dto.getSubcategoryId()
        );
    }

    private void loadUtilities() {
        List<Utility> utilityList = new ArrayList<>();
        utilityList.add(new Utility("1", "Tủ lạnh", R.drawable.ic_refrigerator));
        utilityList.add(new Utility("2", "Trợ lý AI", R.drawable.ic_ai));
        utilityList.add(new Utility("3", "Khẩu vị", R.drawable.ic_taste));
        utilityList.add(new Utility("4", "Điểm xanh", R.drawable.ic_yellow_cert));
        utilityList.add(new Utility("5", "Bài viết", R.drawable.ic_blog));
        _utilities.setValue(utilityList);
    }

    private void loadBannersFromApi() {
        promotionRepository.refreshBanners();
    }

    private void loadFlashSales() {
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
                                item.getUnit(),
                                item.getDiscount(),
                                0,
                                item.getImageUrl(),
                                item.getRating()
                        ));
                    }
                    _flashSales.setValue(flashSaleList);
                } else {
                    loadFallbackFlashSales();
                }
            }

            @Override
            public void onFailure(Call<FlashSaleResponseDto> call, Throwable t) {
                loadFallbackFlashSales();
            }
        });
    }

    private void loadFallbackFlashSales() {
        List<FlashSale> flashSaleList = new ArrayList<>();
        flashSaleList.add(new FlashSale("68d1501b1108dd931e9631a6", "Táo Envy Mỹ", 120000, "1.5kg", "-20%", 0,
                "https://lh3.googleusercontent.com/voEE3B_IhofqhrkoWMN05xl_FqpvHnGOc0NoTCvD1A9IeGtCE0E8X_BAeAb4Y136YmxkUOCR0nGJSXW-KtekoNy38c6_sWurnQ=rw", 4.5f));
        flashSaleList.add(new FlashSale("68d150221108dd931e9631d4", "Bơ Sáp Đắk Lắk", 45000, "1kg", "-15%", 0,
                "https://lh3.googleusercontent.com/PKppN4rs6zjbBlbMk_AXcwjTk-40oORwBRW9njwjANV5gFgme2ioKCV4nKuTUNYck_V41-pBPfSeoSTu5rE9KQxSFKICMDWkYg=rw", 4.2f));
        flashSaleList.add(new FlashSale("68d150401108dd931e963284", "Nho đỏ Candy Mỹ", 350000, "450g", "-10%", 0,
                "https://lh3.googleusercontent.com/8P3BtXVPs972GoFMXDzALWkU7LnqpsnsLDwTfPgHa_MZQceIWsV9Lkvn10J-vzS9ChlFYiOOu4lyIYmUKzC9tlStOVr6gXVP=rw", 4.8f));
        _flashSales.setValue(flashSaleList);
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
                    recipeList.add(new Recipe(dish.id, inst.dishName, inst.cookingTime, 0, thumbnailUrl));
                }
                if (recipeList.size() >= 10) break;
            }
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
