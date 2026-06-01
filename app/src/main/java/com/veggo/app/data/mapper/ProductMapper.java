package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static Product fromDto(ProductDto dto) {
        return new Product(dto.getId(), dto.getName(), dto.getPrice(), dto.getImageUrl());
    }

    public static Product fromEntity(ProductEntity entity) {
        return new Product(entity.getId(), entity.getName(), entity.getPrice(), entity.getImageUrl());
    }

    public static ProductEntity toEntity(ProductDto dto) {
        return new ProductEntity(dto.getId(), dto.getName(), dto.getPrice(), dto.getImageUrl());
    }
}
