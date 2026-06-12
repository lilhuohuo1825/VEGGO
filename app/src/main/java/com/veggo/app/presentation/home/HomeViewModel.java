package com.veggo.app.presentation.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.core.utils.JsonUtils;
import com.veggo.app.R;
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
        utilityList.add(new Utility("1", "Tủ lạnh\nthôgmin", R.drawable.ic_refrigerator));
        utilityList.add(new Utility("2", "AI cá\nnhân hóa", R.drawable.ic_ai));
        utilityList.add(new Utility("3", "Thiết lập\nkhẩu vị", R.drawable.ic_taste));
        utilityList.add(new Utility("4", "Điểm\ncarbon", R.drawable.ic_yellow_cert));
        utilityList.add(new Utility("5", "Blog\n ", R.drawable.ic_blog));
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
        List<FlashSale> flashSaleList = new ArrayList<>();
        flashSaleList.add(new FlashSale("1", "Táo Envy Mỹ", 120000, "1kg", "-20%", R.drawable.onboarding_1, null, 4.5f));
        flashSaleList.add(new FlashSale("2", "Bơ Sáp Đắk Lắk", 45000, "1kg", "-15%", R.drawable.onboarding_2, null, 4.2f));
        flashSaleList.add(new FlashSale("3", "Nho Mẫu Đơn", 350000, "500g", "-10%", R.drawable.onboarding_3, null, 4.8f));
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
                // Map từ DTO sang Model (tạm thời gán icon mặc định vì JSON không có icon)
                categoryList.add(new Category(
                    dto.getCategoryID(),
                    dto.getCategoryName(),
                    R.drawable.ic_vegetable 
                ));
            }
            _categories.setValue(categoryList);
        } else {
            // Fallback nếu không đọc được file
            List<Category> fallbackList = new ArrayList<>();
            fallbackList.add(new Category("1", "Trái cây", R.drawable.ic_fruit));
            fallbackList.add(new Category("2", "Rau củ", R.drawable.ic_vegetable));
            _categories.setValue(fallbackList);
        }
    }
}