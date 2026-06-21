package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FridgeItemDto {
    @SerializedName("_id")
    private String id;
    private String userId;
    private String sku;
    private String orderId;
    private String name;
    private String image;
    private List<String> images;
    private double quantity;
    private String unit;
    private String source;
    private String locationCode;
    private String purchaseDate;
    private String expiryDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}
