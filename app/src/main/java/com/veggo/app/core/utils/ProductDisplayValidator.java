package com.veggo.app.core.utils;

import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.projection.ProductItemProjection;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

/**
 * Kiểm tra sản phẩm đủ điều kiện hiển thị trên UI (catalog, liên quan, tìm kiếm).
 * Chỉ cho phép sản phẩm có dữ liệu hợp lệ từ MongoDB/API — lọc bỏ bản ghi test/lỗi.
 */
public final class ProductDisplayValidator {
    private static final int MIN_NAME_LENGTH = 3;
    private static final long MAX_PRICE = 50_000_000L;

    private ProductDisplayValidator() {
    }

    public static boolean isDisplayable(ProductDto dto) {
        if (dto == null) {
            return false;
        }
        if (Boolean.FALSE.equals(dto.getActive())) {
            return false;
        }
        return hasValidId(dto.getId())
                && hasValidName(dto.getName())
                && hasValidPrice(dto.getPrice())
                && hasValidImage(ProductImageUtils.resolveImageUrl(dto));
    }

    public static boolean isDisplayable(ProductEntity entity) {
        if (entity == null) {
            return false;
        }
        return hasValidId(entity.getId())
                && hasValidName(entity.getName())
                && hasValidPrice(entity.getPrice())
                && hasValidImage(entity.getImageUrl());
    }

    public static boolean isDisplayable(Product product) {
        if (product == null) {
            return false;
        }
        return hasValidId(product.getId())
                && hasValidName(product.getName())
                && hasValidPrice(product.getPrice())
                && hasValidImage(product.getImageUrl());
    }

    public static boolean isDisplayable(ProductItemProjection projection) {
        if (projection == null) {
            return false;
        }
        return hasValidId(projection.getId())
                && hasValidName(projection.getName())
                && hasValidPrice(projection.getPrice())
                && hasValidImage(projection.getImageUrl());
    }

    public static boolean hasValidWeight(String weight) {
        if (weight == null) {
            return false;
        }
        String trimmed = weight.trim();
        return trimmed.length() >= 2 && hasValidName(trimmed);
    }

    private static boolean hasValidId(String id) {
        return id != null && !id.trim().isEmpty();
    }

    private static boolean hasValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH) {
            return false;
        }
        // Loại tên test kiểu "ss", "aaa"
        if (trimmed.matches("^(.)\\1*$") && trimmed.length() <= 4) {
            return false;
        }
        return true;
    }

    private static boolean hasValidPrice(long price) {
        return price > 0 && price <= MAX_PRICE;
    }

    public static boolean hasValidImage(String imageUrl) {
        return ProductImageUtils.isHttpUrl(imageUrl);
    }
}
