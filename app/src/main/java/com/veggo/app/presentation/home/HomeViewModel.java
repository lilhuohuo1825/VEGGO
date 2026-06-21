package com.veggo.app.presentation.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.utils.JsonUtils;
import com.veggo.app.R;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.FlashSaleResponseDto;
import com.veggo.app.domain.model.Blog;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Utility;
import com.veggo.app.domain.model.Banner;
import com.veggo.app.domain.model.FlashSale;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Category;
import com.veggo.app.data.remote.dto.CategoryDto;

import com.veggo.app.assets.AssetModels;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Product>> _products = new MutableLiveData<>();
    public LiveData<List<Product>> getProducts() { return _products; }

    private final MutableLiveData<List<Blog>> _blogs = new MutableLiveData<>();
    public LiveData<List<Blog>> getBlogs() { return _blogs; }

    private final MutableLiveData<List<Utility>> _utilities = new MutableLiveData<>();
    public LiveData<List<Utility>> getUtilities() { return _utilities; }

    private final MutableLiveData<List<Banner>> _banners = new MutableLiveData<>();
    public LiveData<List<Banner>> getBanners() { return _banners; }

    private final MutableLiveData<List<FlashSale>> _flashSales = new MutableLiveData<>();
    public LiveData<List<FlashSale>> getFlashSales() { return _flashSales; }

    private final MutableLiveData<List<Recipe>> _recipes = new MutableLiveData<>();
    public LiveData<List<Recipe>> getRecipes() { return _recipes; }

    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>();
    public LiveData<List<Category>> getCategories() { return _categories; }

    public void loadHomeData(Context context) {
        Gson gson = new Gson();

        // Đọc Products
        String productsJson = JsonUtils.loadJSONFromAsset(context, "products.json");
        if (productsJson != null) {
            Type productListType = new TypeToken<List<Product>>() {}.getType();
            _products.setValue(gson.fromJson(productsJson, productListType));
        }

        // Đọc Blogs/Recipes
        String blogsJson = JsonUtils.loadJSONFromAsset(context, "blogs.json");
        if (blogsJson != null) {
            Type blogListType = new TypeToken<List<Blog>>() {}.getType();
            _blogs.setValue(gson.fromJson(blogsJson, blogListType));
        }

        loadUtilities();
        loadBanners();
        loadFlashSales();
        loadRecipes(context);
        loadCategories(context);
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

    private void loadBanners() {
        List<Banner> bannerList = new ArrayList<>();
        bannerList.add(new Banner("1", R.drawable.banner_freeship, null));
        bannerList.add(new Banner("2", R.drawable.banner_chaohe, null));
        bannerList.add(new Banner("3", R.drawable.banner_nam_rom, null));
        bannerList.add(new Banner("4", R.drawable.banner_traicay, null));
        bannerList.add(new Banner("5", R.drawable.banner_coopselect, null));
        _banners.setValue(bannerList);
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
        flashSaleList.add(new FlashSale("68d1501b1108dd931e9631a6", "Táo Envy Mỹ", 120000, "1.5kg", "-20%", 0, "https://lh3.googleusercontent.com/voEE3B_IhofqhrkoWMN05xl_FqpvHnGOc0NoTCvD1A9IeGtCE0E8X_BAeAb4Y136YmxkUOCR0nGJSXW-KtekoNy38c6_sWurnQ=rw", 4.5f));
        flashSaleList.add(new FlashSale("68d150221108dd931e9631d4", "Bơ Sáp Đắk Lắk", 45000, "1kg", "-15%", 0, "https://lh3.googleusercontent.com/PKppN4rs6zjbBlbMk_AXcwjTk-40oORwBRW9njwjANV5gFgme2ioKCV4nKuTUNYck_V41-pBPfSeoSTu5rE9KQxSFKICMDWkYg=rw", 4.2f));
        flashSaleList.add(new FlashSale("68d150401108dd931e963284", "Nho đỏ Candy Mỹ", 350000, "450g", "-10%", 0, "https://lh3.googleusercontent.com/8P3BtXVPs972GoFMXDzALWkU7LnqpsnsLDwTfPgHa_MZQceIWsV9Lkvn10J-vzS9ChlFYiOOu4lyIYmUKzC9tlStOVr6gXVP=rw", 4.8f));
        _flashSales.setValue(flashSaleList);
    }

    private void loadRecipes(Context context) {
        String dishesJson = JsonUtils.loadJSONFromAsset(context, "dishes.json");
        String instructionsJson = JsonUtils.loadJSONFromAsset(context, "instructions.json");
        
        if (dishesJson == null || instructionsJson == null) {
            // Fallback
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
                    String thumbnailUrl = videoId != null ? "https://img.youtube.com/vi/" + videoId + "/0.jpg" : null;
                    
                    recipeList.add(new Recipe(
                        dish.id,
                        inst.dishName,
                        inst.cookingTime,
                        0,
                        thumbnailUrl
                    ));
                }
                if (recipeList.size() >= 10) break; // Limit for home screen
            }
        }
        _recipes.setValue(recipeList);
    }

    private String extractYoutubeId(String url) {
        if (url == null) return null;
        try {
            if (url.contains("/embed/")) {
                String temp = url.split("/embed/")[1];
                if (temp.contains("?")) {
                    return temp.split("\\?")[0];
                }
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
                categoryList.add(new Category(
                    dto.getCategoryID(),
                    dto.getCategoryName(),
                    iconRes
                ));
            }
            _categories.setValue(categoryList);
        } else {
            // Fallback nếu không đọc được file
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
            default: return R.drawable.ic_vegetable;
        }
    }
}