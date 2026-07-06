package com.veggo.app.data.mapper;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.projection.ProductItemProjection;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.domain.model.Product;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public final class ProductMapper {
    private static final Gson GSON = new Gson();

    private ProductMapper() {
    }

    private static List<Double> parseWeightOptions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            Double[] values = GSON.fromJson(json, Double[].class);
            if (values == null) {
                return null;
            }
            List<Double> list = new ArrayList<>();
            for (Double value : values) {
                if (value != null) {
                    list.add(value);
                }
            }
            return list;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String serializeWeightOptions(List<Double> weightOptions) {
        if (weightOptions == null || weightOptions.isEmpty()) {
            return null;
        }

        return GSON.toJson(weightOptions);
    }

    public static Product fromDto(ProductDto dto) {
        return new Product(
                dto.getId(),
                dto.getName(),
                dto.getSku(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getImageUrl(),
                dto.getWeightOptions(),
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getIngredients(),
                dto.getUsage(),
                dto.getStorage(),
                dto.getProducer(),
                dto.getResponsibleOrg(),
                dto.getSafetyWarning(),
                dto.getManufactureDate(),
                dto.getExpiryDate(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent(),
                dto.getCategoryId(),
                dto.getSubcategoryId(),
                dto.getBrand(),
                dto.getCarbonSavingPoint()
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
            parseWeightOptions(entity.getWeightOptionsJson()),
                entity.getWeight(),
                entity.getRating(),
                entity.getReviewCount(),
                entity.getSoldCount(),
                entity.getDescription(),
                entity.getIngredients(),
                entity.getUsage(),
                entity.getStorage(),
                entity.getProducer(),
                entity.getResponsibleOrg(),
                entity.getSafetyWarning(),
                entity.getManufactureDate(),
                entity.getExpiryDate(),
                entity.getOrigin(),
                entity.getCondition(),
                entity.getFatContent(),
                entity.getCategoryId(),
                entity.getSubcategoryId(),
                entity.getBrand(),
                entity.getCarbonSavingPoint()
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
                ProductImageUtils.resolveImageUrl(dto),
                serializeWeightOptions(dto.getWeightOptions()),
                dto.getWeight(),
                dto.getRating(),
                dto.getReviewCount(),
                dto.getSoldCount(),
                dto.getDescription(),
                dto.getIngredients(),
                dto.getUsage(),
                dto.getStorage(),
                dto.getProducer(),
                dto.getResponsibleOrg(),
                dto.getSafetyWarning(),
                dto.getManufactureDate(),
                dto.getExpiryDate(),
                dto.getOrigin(),
                dto.getCondition(),
                dto.getFatContent(),
                dto.getCategoryId(),
                dto.getSubcategoryId(),
                dto.getBrand(),
                dto.getCarbonSavingPoint()
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
                serializeWeightOptions(product.getWeightOptions()),
                product.getWeight(),
                product.getRating(),
                product.getReviewCount(),
                product.getSoldCount(),
                product.getDescription(),
                product.getIngredients(),
                product.getUsage(),
                product.getStorage(),
                product.getProducer(),
                product.getResponsibleOrg(),
                product.getSafetyWarning(),
                product.getManufactureDate(),
                product.getExpiryDate(),
                product.getOrigin(),
                product.getCondition(),
                product.getFatContent(),
                product.getCategoryId(),
                product.getSubcategoryId(),
                product.getBrand(),
                product.getCarbonSavingPoint()
        );
    }
}
