package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.CartItemEntity;

import java.util.List;

@Dao
public interface CartDao {
    @Query("SELECT * FROM cart_items")
    LiveData<List<CartItemEntity>> observeCartItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CartItemEntity item);

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    void deleteByProductId(String productId);

    @Query("DELETE FROM cart_items")
    void clearCart();
}
