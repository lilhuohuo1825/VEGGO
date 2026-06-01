package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class CartDto {
    @SerializedName("_id")
    private String id;
    private String userId;
    private List<CartItemDto> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public static class CartItemDto {
        private ProductDto productId;
        private int quantity;

        public ProductDto getProduct() { return productId; }
        public void setProduct(ProductDto product) { this.productId = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
