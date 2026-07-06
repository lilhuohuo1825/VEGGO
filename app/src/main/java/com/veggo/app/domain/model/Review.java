package com.veggo.app.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Review {
    private final String id;
    private final String customerId;
    private final String reviewerName;
    private final String reviewTime;
    private final float rating;
    private final String content;
    private final String avatarUrl;
    private final List<String> imageUrls;
    private final int helpfulCount;
    private final List<String> likeCustomerIds;

    public Review(String id,
                    String customerId,
                    String reviewerName,
                    String reviewTime,
                    float rating,
                    String content,
                    String avatarUrl,
                    List<String> imageUrls,
                    int helpfulCount,
                    List<String> likeCustomerIds) {
        this.id = id;
        this.customerId = customerId;
        this.reviewerName = reviewerName;
        this.reviewTime = reviewTime;
        this.rating = rating;
        this.content = content;
        this.avatarUrl = avatarUrl;
        this.imageUrls = imageUrls;
        this.helpfulCount = helpfulCount;
        this.likeCustomerIds = likeCustomerIds != null
                ? new ArrayList<>(likeCustomerIds)
                : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getReviewerName() { return reviewerName; }
    public String getReviewTime() { return reviewTime; }
    public float getRating() { return rating; }
    public String getContent() { return content; }
    public String getAvatarUrl() { return avatarUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public int getHelpfulCount() { return helpfulCount; }
    public List<String> getLikeCustomerIds() {
        return Collections.unmodifiableList(likeCustomerIds);
    }

    public boolean isLikedBy(String viewerCustomerId) {
        if (viewerCustomerId == null || viewerCustomerId.trim().isEmpty()) {
            return false;
        }
        for (String likedBy : likeCustomerIds) {
            if (viewerCustomerId.equals(likedBy)) {
                return true;
            }
        }
        return false;
    }
}
