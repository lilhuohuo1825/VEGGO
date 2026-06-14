package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ProductDto {
    @SerializedName("_id")
    private String id;
    private String name;
    private String sku;
    private String categoryId;
    private String description;
    private long price;
    private long originalPrice;
    private String unit;
    private String imageUrl;
    private int stock;
    private float rating;
    private Boolean isActive;
    private String weight;
    private float rating;
    private int reviewCount;
    private int soldCount;
    private String origin;
    private String condition;
    private String fatContent;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getFatContent() { return fatContent; }
    public void setFatContent(String fatContent) { this.fatContent = fatContent; }
}
