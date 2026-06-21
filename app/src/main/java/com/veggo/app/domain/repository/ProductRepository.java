package com.veggo.app.domain.repository;

import androidx.lifecycle.LiveData;

import com.veggo.app.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    LiveData<List<Product>> observeProducts();
    LiveData<List<Product>> observeProducts(int limit);
    LiveData<Product> observeProductById(String productId);
    LiveData<List<com.veggo.app.domain.model.Recipe>> getRelatedRecipes(String productId);
    LiveData<List<com.veggo.app.domain.model.Review>> getProductReviews(String productId);
    LiveData<List<Product>> observeProductsByCategory(String categoryId);
    LiveData<List<Product>> observeProductsBySubcategory(String subcategoryId);
    LiveData<List<Product>> observeRelatedProducts(String currentProductId, String categoryId, String subcategoryId);
    LiveData<List<Product>> searchProducts(String query);
    void refreshProducts();
}
