package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM products")
    LiveData<List<ProductEntity>> observeProducts();

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    LiveData<ProductEntity> observeProductById(String productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProductEntity product);

    @Query("DELETE FROM products")
    void clearAll();
}
