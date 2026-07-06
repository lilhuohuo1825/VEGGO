package com.veggo.app.core.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bổ sung CarbonSavingPoint / EmissionFactor từ products.json khi Room chưa có đủ metadata.
 */
public final class ProductCarbonMetadata {

    private static volatile Map<String, Info> bySku;
    private static volatile Map<String, Info> byId;

    private ProductCarbonMetadata() {
    }

    public static final class Info {
        public final double carbonSavingPoint;
        public final double emissionFactor;

        public Info(double carbonSavingPoint, double emissionFactor) {
            this.carbonSavingPoint = carbonSavingPoint;
            this.emissionFactor = emissionFactor;
        }
    }

    @NonNull
    public static Info resolve(
            @NonNull Context context,
            @Nullable String sku,
            @Nullable String productId,
            double fallbackCarbonSavingPoint
    ) {
        ensureLoaded(context);
        Info info = lookup(bySku, sku);
        if (info == null) {
            info = lookup(byId, productId);
        }
        if (info != null) {
            return info;
        }
        return new Info(Math.max(0, fallbackCarbonSavingPoint), 0);
    }

    private static void ensureLoaded(@NonNull Context context) {
        if (bySku != null && byId != null) {
            return;
        }
        synchronized (ProductCarbonMetadata.class) {
            if (bySku != null && byId != null) {
                return;
            }
            Map<String, Info> skuMap = new HashMap<>();
            Map<String, Info> idMap = new HashMap<>();
            try {
                List<AssetModels.Product> products = new AssetJsonLoader(context).readList(
                        AssetFiles.PRODUCTS,
                        AssetModels.Product.class
                );
                if (products != null) {
                    for (AssetModels.Product product : products) {
                        Info info = new Info(product.carbonSavingPoint, product.emissionFactor);
                        if (product.sku != null && !product.sku.trim().isEmpty()) {
                            skuMap.put(product.sku.trim(), info);
                        }
                        String id = product.objectId != null ? String.valueOf(product.objectId) : null;
                        if (id == null || id.trim().isEmpty()) {
                            id = product.sku;
                        }
                        if (id != null && !id.trim().isEmpty()) {
                            idMap.put(id.trim(), info);
                            idMap.put(id.trim().toLowerCase(Locale.US), info);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Fallback to product model values when asset metadata is unavailable.
            }
            bySku = skuMap;
            byId = idMap;
        }
    }

    @Nullable
    private static Info lookup(@Nullable Map<String, Info> map, @Nullable String key) {
        if (map == null || key == null) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Info info = map.get(trimmed);
        return info != null ? info : map.get(trimmed.toLowerCase(Locale.US));
    }
}
