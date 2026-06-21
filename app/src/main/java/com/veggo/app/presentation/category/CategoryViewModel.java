package com.veggo.app.presentation.category;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.veggo.app.assets.AssetModels;
import com.veggo.app.domain.repository.CategoryRepository;

import java.util.List;

public class CategoryViewModel extends ViewModel {
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>();

    public CategoryViewModel(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
        refresh();
    }

    public void refresh() {
        categoryRepository.refreshCategories();
    }

    public LiveData<List<AssetModels.Category>> getCategories() {
        return categoryRepository.observeCategories();
    }

    public void selectCategory(String categoryId) {
        selectedCategoryId.setValue(categoryId);
    }

    public LiveData<String> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public LiveData<AssetModels.Category> getSelectedCategory() {
        return Transformations.switchMap(selectedCategoryId, id -> 
            Transformations.map(getCategories(), categories -> {
                for (AssetModels.Category category : categories) {
                    if (category.categoryId.equals(id)) {
                        return category;
                    }
                }
                return null;
            })
        );
    }
}
