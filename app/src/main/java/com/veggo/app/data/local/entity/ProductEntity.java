package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private long price;
    private long originalPrice;
    private String sku;
    private String imageUrl;
    private String weight;
    private float rating;
    private int reviewCount;
    private int soldCount;
    private String description;
    private String origin;
    private String condition;
    private String fatContent;
    private String categoryId;
    private String subcategoryId;

    public ProductEntity(@NonNull String id, String name, long price, long originalPrice, 
                         String sku, String imageUrl, String weight, float rating, int reviewCount, 
                         int soldCount, String description, String origin, 
                         String condition, String fatContent, String categoryId, String subcategoryId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.sku = sku;
        this.imageUrl = imageUrl;
        this.weight = weight;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        this.description = description;
        this.origin = origin;
        this.condition = condition;
        this.fatContent = fatContent;
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getFatContent() { return fatContent; }
    public void setFatContent(String fatContent) { this.fatContent = fatContent; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }
}
