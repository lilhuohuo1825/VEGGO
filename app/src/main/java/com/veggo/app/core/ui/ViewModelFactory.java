package com.veggo.app.core.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.PromotionRepository;
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
    private final PromotionRepository promotionRepository;

    public ViewModelFactory(ProductRepository productRepository) {
        this(productRepository, null, null, null);
    }

    public ViewModelFactory(CategoryRepository categoryRepository) {
        this(null, categoryRepository, null, null);
    }

    public ViewModelFactory(CartRepository cartRepository) {
        this(null, null, cartRepository, null);
    }

    public ViewModelFactory(CartRepository cartRepository, PromotionRepository promotionRepository) {
        this(null, null, cartRepository, promotionRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, CartRepository cartRepository) {
        this(productRepository, null, cartRepository, null);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository, CartRepository cartRepository, PromotionRepository promotionRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cartRepository = cartRepository;
        this.promotionRepository = promotionRepository;
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
            return (T) new CartViewModel(cartRepository, promotionRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
