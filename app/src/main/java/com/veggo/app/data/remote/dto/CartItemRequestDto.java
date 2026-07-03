package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CartItemRequestDto {
    @SerializedName("productId")
    private String productId;

    @SerializedName("sku")
    private String sku;
    
    @SerializedName("quantity")
    private int quantity;
    
    @SerializedName("selectedWeight")
    private double selectedWeight;

    public CartItemRequestDto(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
        this.selectedWeight = 1.0;
    }

    public CartItemRequestDto(String sku, int quantity, double selectedWeight) {
        this.sku = sku;
        this.quantity = quantity;
        this.selectedWeight = selectedWeight;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSelectedWeight() { return selectedWeight; }
    public void setSelectedWeight(double selectedWeight) { this.selectedWeight = selectedWeight; }
}
