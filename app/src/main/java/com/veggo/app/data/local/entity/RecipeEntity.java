package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class RecipeEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String imageUrl;
    private String cookingTime;
    private String price;
    private float rating;
    private int reviewCount;
    private boolean isBookmarked;
    private String productId; // Foreign key or link to product

    public RecipeEntity(@NonNull String id, String name, String imageUrl, String cookingTime, String price, float rating, int reviewCount, boolean isBookmarked, String productId) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.cookingTime = cookingTime;
        this.price = price;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.isBookmarked = isBookmarked;
        this.productId = productId;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCookingTime() { return cookingTime; }
    public void setCookingTime(String cookingTime) { this.cookingTime = cookingTime; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
}
