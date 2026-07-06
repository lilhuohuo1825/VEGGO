package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class CartDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("customerId")
    private String customerId;

    @SerializedName("items")
    private List<CartItemDto> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public static class CartItemDto {
        @SerializedName("sku")
        private String sku;

        @SerializedName("product")
        private ProductDto product;
        
        @SerializedName("quantity")
        private int quantity;

        @SerializedName("selectedWeight")
        private double selectedWeight;

        @SerializedName("price")
        private long price;

        @SerializedName(value = "originalPrice", alternate = {"base_price"})
        private long originalPrice;

        @SerializedName("updatedAt")
        private String updatedAt;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public ProductDto getProduct() { return product; }
        public void setProduct(ProductDto product) { this.product = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getSelectedWeight() { return selectedWeight; }
        public void setSelectedWeight(double selectedWeight) { this.selectedWeight = selectedWeight; }
        public long getPrice() { return price; }
        public void setPrice(long price) { this.price = price; }
        public long getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}
