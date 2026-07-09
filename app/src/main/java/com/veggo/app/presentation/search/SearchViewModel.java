package com.veggo.app.presentation.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.utils.TextSearchUtils;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    
    private final LiveData<List<Product>> productResults;
    private final MutableLiveData<List<FeatureResult>> featureResults = new MutableLiveData<>();
    private final LiveData<List<AssetModels.Category>> categories;

    public SearchViewModel(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        
        this.categories = categoryRepository.observeCategories();
        
        productResults = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return productRepository.searchProducts(query.trim());
        });
        
        // Initial features
        List<FeatureResult> allFeatures = new ArrayList<>();
        allFeatures.add(new FeatureResult("Tủ lạnh", "refrigerator"));
        allFeatures.add(new FeatureResult("Trợ lý AI", "ai_assistant"));
        allFeatures.add(new FeatureResult("Khẩu vị", "taste_profile"));
        allFeatures.add(new FeatureResult("Điểm xanh", "green_points"));
        allFeatures.add(new FeatureResult("Bài viết", "articles"));
        
        featureResults.setValue(allFeatures);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        
        // Filter features manually
        List<FeatureResult> allFeatures = new ArrayList<>();
        allFeatures.add(new FeatureResult("Tủ lạnh", "refrigerator"));
        allFeatures.add(new FeatureResult("Trợ lý AI", "ai_assistant"));
        allFeatures.add(new FeatureResult("Khẩu vị", "taste_profile"));
        allFeatures.add(new FeatureResult("Điểm xanh", "green_points"));
        allFeatures.add(new FeatureResult("Bài viết", "articles"));
        
        if (query == null || query.trim().isEmpty()) {
            featureResults.setValue(allFeatures);
        } else {
            List<FeatureResult> filtered = new ArrayList<>();
            for (FeatureResult f : allFeatures) {
                if (TextSearchUtils.matches(f.getName(), query)) {
                    filtered.add(f);
                }
            }
            featureResults.setValue(filtered);
        }
    }

    public List<AssetModels.Category> filterCategories(List<AssetModels.Category> categories, String query) {
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }
        if (query == null || query.trim().isEmpty()) {
            return categories;
        }
        List<AssetModels.Category> filtered = new ArrayList<>();
        for (AssetModels.Category category : categories) {
            if (category == null) {
                continue;
            }
            if (TextSearchUtils.matches(category.categoryName, query)) {
                filtered.add(category);
            }
        }
        return filtered;
    }

    public LiveData<List<Product>> getProductResults() {
        return productResults;
    }

    public LiveData<List<FeatureResult>> getFeatureResults() {
        return featureResults;
    }

    public LiveData<List<AssetModels.Category>> getCategories() {
        return categories;
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void refresh() {
        categoryRepository.refreshCategories();
        String current = searchQuery.getValue();
        if (current != null && !current.trim().isEmpty()) {
            searchQuery.setValue(current);
        }
    }

    public static class FeatureResult {
        private final String name;
        private final String type;

        public FeatureResult(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public String getType() { return type; }
    }
}
