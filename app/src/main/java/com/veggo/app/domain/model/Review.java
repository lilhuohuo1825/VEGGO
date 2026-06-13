package com.veggo.app.domain.model;

import java.util.List;

public class Review {
    private String reviewerName;
    private String reviewTime;
    private float rating;
    private String content;
    private String avatarUrl;
    private List<String> imageUrls;

    public Review(String reviewerName, String reviewTime, float rating, String content, String avatarUrl, List<String> imageUrls) {
        this.reviewerName = reviewerName;
        this.reviewTime = reviewTime;
        this.rating = rating;
        this.content = content;
        this.avatarUrl = avatarUrl;
        this.imageUrls = imageUrls;
    }

    // Getters
    public String getReviewerName() { return reviewerName; }
    public String getReviewTime() { return reviewTime; }
    public float getRating() { return rating; }
    public String getContent() { return content; }
    public String getAvatarUrl() { return avatarUrl; }
    public List<String> getImageUrls() { return imageUrls; }
}