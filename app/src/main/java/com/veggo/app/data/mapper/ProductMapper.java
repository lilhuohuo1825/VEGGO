package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static Product fromDto(ProductDto dto) {
        Product product = new Product(dto.getId(), dto.getName(), dto.getPrice(), dto.getImageUrl());
        product.setRating(dto.getRating());
        return product;
    }

    public static Product fromEntity(ProductEntity entity) {
        Product product = new Product(entity.getId(), entity.getName(), entity.getPrice(), entity.getImageUrl());
        product.setRating(entity.getRating());
        return product;
    }

    public static ProductEntity toEntity(ProductDto dto) {
        return new ProductEntity(dto.getId(), dto.getName(), dto.getPrice(), dto.getImageUrl(), dto.getRating());
    }
}
