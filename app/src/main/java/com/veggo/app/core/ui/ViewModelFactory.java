package com.veggo.app.core.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ConsultationRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.domain.repository.RecipeRepository;
import com.veggo.app.domain.repository.ReviewRepository;
import com.veggo.app.presentation.cart.CartViewModel;
import com.veggo.app.presentation.category.CategoryViewModel;
import com.veggo.app.presentation.product.ProductViewModel;
import com.veggo.app.presentation.search.SearchViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ConsultationRepository consultationRepository;
    private final RecipeRepository recipeRepository;
    private final CartRepository cartRepository;

    public ViewModelFactory(ProductRepository productRepository) {
        this(productRepository, null, null, null, null, null);
    }

    public ViewModelFactory(CategoryRepository categoryRepository) {
        this(null, categoryRepository, null, null, null, null);
    }

    public ViewModelFactory(CartRepository cartRepository) {
        this(null, null, null, null, null, cartRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, ReviewRepository reviewRepository) {
        this(productRepository, null, reviewRepository, null, null, null);
    }

    public ViewModelFactory(ProductRepository productRepository, ReviewRepository reviewRepository,
                            ConsultationRepository consultationRepository) {
        this(productRepository, null, reviewRepository, consultationRepository, null, null);
    }

    public ViewModelFactory(ProductRepository productRepository, ReviewRepository reviewRepository,
                            ConsultationRepository consultationRepository, RecipeRepository recipeRepository) {
        this(productRepository, null, reviewRepository, consultationRepository, recipeRepository, null);
    }

    public ViewModelFactory(ProductRepository productRepository, ReviewRepository reviewRepository,
                            ConsultationRepository consultationRepository, RecipeRepository recipeRepository,
                            CartRepository cartRepository) {
        this(productRepository, null, reviewRepository, consultationRepository, recipeRepository, cartRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository,
                            ReviewRepository reviewRepository) {
        this(productRepository, categoryRepository, reviewRepository, null, null, null);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository,
                            ReviewRepository reviewRepository, ConsultationRepository consultationRepository) {
        this(productRepository, categoryRepository, reviewRepository, consultationRepository, null, null);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository,
                            CartRepository cartRepository) {
        this(productRepository, categoryRepository, null, null, null, cartRepository);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository,
                            ReviewRepository reviewRepository, ConsultationRepository consultationRepository,
                            RecipeRepository recipeRepository) {
        this(productRepository, categoryRepository, reviewRepository, consultationRepository, recipeRepository, null);
    }

    public ViewModelFactory(ProductRepository productRepository, CategoryRepository categoryRepository,
                            ReviewRepository reviewRepository, ConsultationRepository consultationRepository,
                            RecipeRepository recipeRepository, CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.consultationRepository = consultationRepository;
        this.recipeRepository = recipeRepository;
        this.cartRepository = cartRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProductViewModel.class)) {
            return (T) new ProductViewModel(productRepository, reviewRepository, consultationRepository, recipeRepository, cartRepository);
        } else if (modelClass.isAssignableFrom(CategoryViewModel.class)) {
            return (T) new CategoryViewModel(categoryRepository);
        } else if (modelClass.isAssignableFrom(SearchViewModel.class)) {
            return (T) new SearchViewModel(productRepository, categoryRepository);
        } else if (modelClass.isAssignableFrom(CartViewModel.class)) {
            return (T) new CartViewModel(cartRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
