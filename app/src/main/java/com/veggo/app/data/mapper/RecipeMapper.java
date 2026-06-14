package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.RecipeEntity;
import com.veggo.app.domain.model.Recipe;

public final class RecipeMapper {
    private RecipeMapper() {}

    public static Recipe fromEntity(RecipeEntity entity) {
        return new Recipe(
                entity.getId(),
                entity.getName(),
                entity.getImageUrl(),
                entity.getCookingTime(),
                entity.getPrice(),
                entity.getRating(),
                entity.getReviewCount()
        );
    }

    public static RecipeEntity toEntity(Recipe domain, String productId) {
        return new RecipeEntity(
                domain.getId(),
                domain.getName(),
                domain.getImageUrl(),
                domain.getCookingTime(),
                domain.getPrice(),
                domain.getRating(),
                domain.getReviewCount(),
                domain.isBookmarked(),
                productId
        );
    }
}
