package com.veggo.app.core.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.presentation.cart.CartViewModel;
import com.veggo.app.presentation.category.CategoryViewModel;
import com.veggo.app.presentation.product.ProductViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartRepository cartRepository;

    public ViewModelFactory(ProductRepository productRepository) {
        this(productRepository, null, null);
    }

    public ViewModelFactory(CategoryRepository categoryRepository) {
        this(null, categoryRepository, null);
    }

    public ViewModelFactory(CartRepository cartRepository) {
        this(null, null, cartRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository, CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cartRepository = cartRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProductViewModel.class)) {
            return (T) new ProductViewModel(productRepository, cartRepository);
        } else if (modelClass.isAssignableFrom(CategoryViewModel.class)) {
            return (T) new CategoryViewModel(categoryRepository);
        } else if (modelClass.isAssignableFrom(CartViewModel.class)) {
            return (T) new CartViewModel(cartRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
