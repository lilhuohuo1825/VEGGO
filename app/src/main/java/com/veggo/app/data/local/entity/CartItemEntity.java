package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItemEntity {
    @PrimaryKey
    @NonNull
    private String productId;
    private int quantity;

    public CartItemEntity(@NonNull String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @NonNull public String getProductId() { return productId; }
    public void setProductId(@NonNull String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
