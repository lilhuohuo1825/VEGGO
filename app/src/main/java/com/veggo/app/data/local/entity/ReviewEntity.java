package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews")
public class ReviewEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String productId;
    private String reviewerName;
    private String reviewTime;
    private float rating;
    private String content;
    private String avatarUrl;
    // For simplicity in SQLite, we can store images as a comma-separated string or use a separate table
    private String imageUrlsJson; 

    public ReviewEntity(@NonNull String id, String productId, String reviewerName, String reviewTime, float rating, String content, String avatarUrl, String imageUrlsJson) {
        this.id = id;
        this.productId = productId;
        this.reviewerName = reviewerName;
        this.reviewTime = reviewTime;
        this.rating = rating;
        this.content = content;
        this.avatarUrl = avatarUrl;
        this.imageUrlsJson = imageUrlsJson;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getReviewTime() { return reviewTime; }
    public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getImageUrlsJson() { return imageUrlsJson; }
    public void setImageUrlsJson(String imageUrlsJson) { this.imageUrlsJson = imageUrlsJson; }
}
