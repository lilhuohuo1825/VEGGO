package com.veggo.app.core.utils;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.veggo.app.R;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chuẩn hóa URL ảnh sản phẩm và load qua Glide — dùng chung cho grid, detail, related.
 */
public final class ProductImageUtils {
    private ProductImageUtils() {
    }

    @Nullable
    public static String resolveImageUrl(@Nullable ProductDto dto) {
        if (dto == null) {
            return null;
        }
        return resolveImageUrl(dto.getImageRaw(), dto.getImageUrlRaw());
    }

    @Nullable
    public static String resolveImageUrl(@Nullable Object imageRaw, @Nullable Object imageUrlRaw) {
        String fromImage = extractHttpUrl(imageRaw);
        if (fromImage != null) {
            return fromImage;
        }
        return extractHttpUrl(imageUrlRaw);
    }

    @Nullable
    public static String resolveDisplayImage(
            @Nullable Context context,
            @Nullable Product product
    ) {
        if (product == null) {
            return null;
        }
        String imageUrl = product.getImageUrl();
        if (isHttpUrl(imageUrl)) {
            return imageUrl;
        }
        if (context != null) {
            return ProductCatalogImageResolver.resolveBySku(context, product.getSku());
        }
        return null;
    }

    @Nullable
    public static String resolveDisplayImage(
            @Nullable Context context,
            @Nullable ProductDto dto
    ) {
        if (dto == null) {
            return null;
        }
        String imageUrl = resolveImageUrl(dto);
        if (isHttpUrl(imageUrl)) {
            return imageUrl;
        }
        if (context != null) {
            return ProductCatalogImageResolver.resolveBySku(context, dto.getSku());
        }
        return null;
    }

    @Nullable
    public static String normalizeUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (trimmed.startsWith("//")) {
            trimmed = "https:" + trimmed;
        }
        return trimmed;
    }

    public static boolean isHttpUrl(@Nullable String url) {
        String normalized = normalizeUrl(url);
        if (normalized == null) {
            return false;
        }
        if (normalized.startsWith("data:")) {
            return false;
        }
        if (normalized.length() > 4096) {
            return false;
        }
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    public static void loadInto(Context context, @Nullable ImageView target, @Nullable String imageUrl) {
        loadInto(context, target, imageUrl, R.drawable.ic_leaf, R.drawable.ic_leaf);
    }

    public static void loadInto(
            Context context,
            @Nullable ImageView target,
            @Nullable String imageUrl,
            @DrawableRes int placeholderRes,
            @DrawableRes int errorRes
    ) {
        if (target == null || context == null) {
            return;
        }

        String normalized = normalizeUrl(imageUrl);
        if (!isHttpUrl(normalized)) {
            target.setImageResource(errorRes);
            return;
        }

        Glide.with(context)
                .load(normalized)
                .placeholder(placeholderRes)
                .error(errorRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(target);
    }

    public static void loadProductImage(Context context, ImageView target, Product product) {
        loadInto(context, target, resolveDisplayImage(context, product));
    }

    public static void clear(ImageView target) {
        if (target == null) {
            return;
        }
        Glide.with(target).clear(target);
    }

    @Nullable
    private static String extractHttpUrl(@Nullable Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            String normalized = normalizeUrl((String) raw);
            return isHttpUrl(normalized) ? normalized : null;
        }
        if (raw instanceof Collection<?>) {
            for (Object item : (Collection<?>) raw) {
                String nested = extractHttpUrl(item);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (raw.getClass().isArray()) {
            Object[] values = (Object[]) raw;
            for (Object value : values) {
                String nested = extractHttpUrl(value);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        String fallback = normalizeUrl(String.valueOf(raw));
        if (fallback != null && (fallback.startsWith("[") || fallback.startsWith("{"))) {
            return null;
        }
        return isHttpUrl(fallback) ? fallback : null;
    }

    @Nullable
    public static List<String> asImageList(@Nullable Object imageRaw, @Nullable Object imageUrlRaw) {
        String resolved = resolveImageUrl(imageRaw, imageUrlRaw);
        if (resolved == null) {
            return null;
        }
        List<String> images = new ArrayList<>();
        images.add(resolved);
        return images;
    }
}
