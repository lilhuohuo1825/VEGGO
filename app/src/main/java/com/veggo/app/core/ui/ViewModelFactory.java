package com.veggo.app.core.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.presentation.category.CategoryViewModel;
import com.veggo.app.presentation.product.ProductViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ViewModelFactory(ProductRepository productRepository) {
        this(productRepository, null);
    }

    public ViewModelFactory(CategoryRepository categoryRepository) {
        this(null, categoryRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProductViewModel.class)) {
            return (T) new ProductViewModel(productRepository);
        } else if (modelClass.isAssignableFrom(CategoryViewModel.class)) {
            return (T) new CategoryViewModel(categoryRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
