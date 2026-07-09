package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.core.utils.UserAvatarHelper;
import com.veggo.app.domain.model.Review;

import java.util.ArrayList;
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
                entity.getId(),
                null,
                entity.getReviewerName(),
                entity.getReviewTime(),
                entity.getRating(),
                entity.getContent(),
                entity.getAvatarUrl(),
                imageUrls,
                0,
                Collections.emptyList()
        );
    }

    public static Review fromDto(com.veggo.app.data.remote.dto.ReviewDto dto) {
        // Format date from ISO string if possible
        String formattedTime = dto.getTime();
        try {
            if (formattedTime != null && formattedTime.contains("T")) {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = isoFormat.parse(formattedTime);
                formattedTime = com.veggo.app.core.utils.DateFormatter.format(date);
            }
        } catch (Exception e) {
            // Fallback to original string
        }

        List<String> likes = dto.getLikes() != null ? dto.getLikes() : Collections.emptyList();

        return new Review(
                dto.getId(),
                dto.getCustomerId(),
                dto.getFullName(),
                formattedTime,
                dto.getRating(),
                dto.getContent(),
                UserAvatarHelper.resolveUrl(dto.getAvatarUrl()),
                resolveImageUrls(dto.getImages()),
                likes.size(),
                likes
        );
    }

    private static List<String> resolveImageUrls(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> resolved = new ArrayList<>();
        for (String image : images) {
            String url = UserAvatarHelper.resolveUrl(image);
            if (url != null && !url.trim().isEmpty()) {
                resolved.add(url);
            }
        }
        return resolved;
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
