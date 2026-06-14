package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.domain.model.Review;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ReviewMapper {
    private ReviewMapper() {}

    public static Review fromEntity(ReviewEntity entity) {
        List<String> imageUrls = entity.getImageUrlsJson() != null && !entity.getImageUrlsJson().isEmpty()
                ? Arrays.asList(entity.getImageUrlsJson().split(","))
                : Collections.emptyList();
        
        return new Review(
                entity.getReviewerName(),
                entity.getReviewTime(),
                entity.getRating(),
                entity.getContent(),
                entity.getAvatarUrl(),
                imageUrls
        );
    }

    public static ReviewEntity toEntity(Review domain, String productId, String id) {
        String imageUrlsJson = domain.getImageUrls() != null 
                ? String.join(",", domain.getImageUrls()) 
                : "";
                
        return new ReviewEntity(
                id,
                productId,
                domain.getReviewerName(),
                domain.getReviewTime(),
                domain.getRating(),
                domain.getContent(),
                domain.getAvatarUrl(),
                imageUrlsJson
        );
    }
}
