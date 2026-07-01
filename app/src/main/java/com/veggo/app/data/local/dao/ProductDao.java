package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.projection.ProductItemProjection;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT id, name, price, imageUrl FROM products")
    LiveData<List<ProductItemProjection>> observeProducts();

    @Query("SELECT id, name, price, imageUrl FROM products LIMIT :limit")
    LiveData<List<ProductItemProjection>> observeProducts(int limit);

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    LiveData<ProductEntity> observeProductById(String productId);

    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    LiveData<List<ProductEntity>> observeProductsByCategory(String categoryId);

    @Query("SELECT * FROM products WHERE subcategoryId = :subcategoryId AND id != :excludeId LIMIT :limit")
    LiveData<List<ProductEntity>> observeRelatedBySubcategory(String subcategoryId, String excludeId, int limit);

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND id != :excludeId LIMIT :limit")
    LiveData<List<ProductEntity>> observeRelatedByCategory(String categoryId, String excludeId, int limit);

    @Query("SELECT * FROM products WHERE (subcategoryId = :subcategoryId OR categoryId = :categoryId) AND id != :excludeId ORDER BY (CASE WHEN subcategoryId = :subcategoryId THEN 0 ELSE 1 END) ASC, rating DESC LIMIT :limit")
    LiveData<List<ProductEntity>> observeRelatedMerged(String categoryId, String subcategoryId, String excludeId, int limit);

    @Query("SELECT * FROM products WHERE subcategoryId = :subcategoryId")
    LiveData<List<ProductEntity>> observeProductsBySubcategory(String subcategoryId);

    @Query("SELECT * FROM products WHERE id IN (:productIds)")
    List<ProductEntity> getProductsByIds(List<String> productIds);

    @Query("SELECT * FROM products")
    LiveData<List<ProductEntity>> observeAllProductEntities();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProductEntity product);

    @Query("DELETE FROM products")
    void clearAll();

    @Query("SELECT * FROM recipes WHERE productId = :productId")
    LiveData<List<com.veggo.app.data.local.entity.RecipeEntity>> observeRelatedRecipes(String productId);

    @Query("SELECT * FROM reviews WHERE productId = :productId")
    LiveData<List<com.veggo.app.data.local.entity.ReviewEntity>> observeProductReviews(String productId);

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    ProductEntity getProductById(String productId);

    @Query("SELECT id, name, price, imageUrl FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR categoryId IN (SELECT categoryId FROM products WHERE name LIKE '%' || :query || '%')")
    LiveData<List<ProductItemProjection>> searchProducts(String query);

    @Query("SELECT id FROM products WHERE sku = :sku LIMIT 1")
    String getProductIdBySku(String sku);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipes(List<com.veggo.app.data.local.entity.RecipeEntity> recipes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReviews(List<com.veggo.app.data.local.entity.ReviewEntity> reviews);

    @Query("UPDATE products SET rating = COALESCE((SELECT AVG(rating) FROM reviews WHERE productId = :productId), 0), reviewCount = (SELECT COUNT(*) FROM reviews WHERE productId = :productId) WHERE id = :productId")
    void updateProductReviewStats(String productId);
}
