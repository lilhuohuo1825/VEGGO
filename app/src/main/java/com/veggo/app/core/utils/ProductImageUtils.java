package com.veggo.app.core.utils;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.veggo.app.R;
import com.veggo.app.data.remote.dto.ProductDto;

import java.util.Collection;
import java.util.List;

/**
 * Chuẩn hóa URL ảnh sản phẩm và load qua Glide — dùng chung cho grid, detail, related.
 */
public final class ProductImageUtils {
    private static final int GLIDE_TIMEOUT_MS = 20_000;

    private ProductImageUtils() {
    }

    @Nullable
    public static String resolveImageUrl(@Nullable ProductDto dto) {
        if (dto == null) {
            return null;
        }
        return resolveImageUrl(dto.getImage(), dto.getImageUrl());
    }

    @Nullable
    public static String resolveImageUrl(@Nullable List<String> images, @Nullable Object imageUrlRaw) {
        String fromList = firstHttpUrl(images);
        if (fromList != null) {
            return normalizeUrl(fromList);
        }
        return normalizeUrl(coerceToUrl(imageUrlRaw));
    }

    @Nullable
    public static String resolveImageUrl(@Nullable Collection<?> images, @Nullable Object imageUrlRaw) {
        if (images != null) {
            for (Object item : images) {
                String url = normalizeUrl(coerceToUrl(item));
                if (isHttpUrl(url)) {
                    return url;
                }
            }
        }
        return normalizeUrl(coerceToUrl(imageUrlRaw));
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
        return normalized != null
                && (normalized.startsWith("http://") || normalized.startsWith("https://"));
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

        RequestOptions options = new RequestOptions()
                .placeholder(placeholderRes)
                .error(errorRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(GLIDE_TIMEOUT_MS);

        Glide.with(target)
                .load(normalized)
                .apply(options)
                .into(target);
    }

    public static void clear(ImageView target) {
        if (target == null) {
            return;
        }
        Glide.with(target).clear(target);
    }

    @Nullable
    private static String firstHttpUrl(@Nullable List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        for (String image : images) {
            String normalized = normalizeUrl(image);
            if (isHttpUrl(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    @Nullable
    private static String coerceToUrl(@Nullable Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            return (String) raw;
        }
        if (raw instanceof List<?>) {
            List<?> list = (List<?>) raw;
            if (!list.isEmpty()) {
                return coerceToUrl(list.get(0));
            }
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            return null;
        }
        return value;
    }
}
