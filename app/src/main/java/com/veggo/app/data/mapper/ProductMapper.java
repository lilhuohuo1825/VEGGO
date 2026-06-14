package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.projection.ProductItemProjection;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static Product fromDto(ProductDto dto) {
        return new Product(
                dto.getId(),
                dto.getName(),
                dto.getSku(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getImageUrl(),
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent()
        );
    }

    public static Product fromEntity(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getPrice(),
                entity.getOriginalPrice(),
                entity.getImageUrl(),
                entity.getWeight(),
                entity.getRating(),
                entity.getReviewCount(),
                entity.getSoldCount(),
                entity.getDescription(),
                entity.getOrigin(),
                entity.getCondition(),
                entity.getFatContent()
        );
    }

    public static Product fromProjection(ProductItemProjection projection) {
        return new Product(
                projection.getId(),
                projection.getName(),
                projection.getPrice(),
                projection.getImageUrl()
        );
    }

    public static ProductEntity toEntity(ProductDto dto) {
        return new ProductEntity(
                dto.getId(),
                dto.getName(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getSku(),
                dto.getImageUrl(),
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent()
        );
    }

    public static ProductEntity toEntity(Product product) {
        return new ProductEntity(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getSku(),
                product.getImageUrl(),
                product.getWeight(),
                product.getRating(),
                product.getReviewCount(),
                product.getSoldCount(),
                product.getDescription(),
                product.getOrigin(),
                product.getCondition(),
                product.getFatContent()
        );
    }
}
