package com.veggo.app.core.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ProductEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tra cứu ảnh sản phẩm từ Room hoặc assets khi snapshot đơn hàng/giỏ hàng thiếu imageUrl.
 */
public final class ProductCatalogImageResolver {
    private static volatile Map<String, String> assetImageBySku;

    private ProductCatalogImageResolver() {
    }

    @Nullable
    public static String resolveCheckoutImage(
            Context context,
            @Nullable com.veggo.app.data.remote.dto.ProductDto product,
            @Nullable String sku
    ) {
        String fromProduct = product != null ? ProductImageUtils.resolveImageUrl(product) : null;
        if (ProductImageUtils.isHttpUrl(fromProduct)) {
            return fromProduct;
        }
        String fromCatalog = resolveBySku(context, sku);
        return ProductImageUtils.isHttpUrl(fromCatalog) ? fromCatalog : "";
    }

    @Nullable
    public static String resolveOrderItemImage(
            Context context,
            @Nullable String currentImage,
            @Nullable String sku
    ) {
        String normalized = ProductImageUtils.normalizeUrl(currentImage);
        if (ProductImageUtils.isHttpUrl(normalized)) {
            return normalized;
        }
        return resolveBySku(context, sku);
    }

    @Nullable
    public static String resolveBySku(@Nullable Context context, @Nullable String sku) {
        if (context == null || sku == null || sku.trim().isEmpty()) {
            return null;
        }
        String key = sku.trim();

        ProductDao productDao = VeggoDatabase.getInstance(context.getApplicationContext()).productDao();
        String productId = productDao.getProductIdBySku(key);
        if (productId != null) {
            ProductEntity entity = productDao.getProductById(productId);
            if (entity != null && ProductDisplayValidator.hasValidImage(entity.getImageUrl())) {
                return ProductImageUtils.normalizeUrl(entity.getImageUrl());
            }
        }

        return resolveFromAssets(context, key);
    }

    public static void invalidateAssetCache() {
        assetImageBySku = null;
    }

    @Nullable
    private static String resolveFromAssets(Context context, String sku) {
        ensureAssetCache(context);
        if (assetImageBySku == null) {
            return null;
        }
        String url = assetImageBySku.get(sku);
        return ProductImageUtils.isHttpUrl(url) ? url : null;
    }

    private static synchronized void ensureAssetCache(Context context) {
        if (assetImageBySku != null) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        try {
            AssetJsonLoader loader = new AssetJsonLoader(context.getApplicationContext());
            List<AssetModels.Product> products = loader.readList(AssetFiles.PRODUCTS, AssetModels.Product.class);
            for (AssetModels.Product product : products) {
                if (product.sku == null || product.sku.trim().isEmpty()) {
                    continue;
                }
                String imageUrl = ProductImageUtils.resolveImageUrl(product.image, null);
                if (ProductImageUtils.isHttpUrl(imageUrl)) {
                    map.put(product.sku.trim(), imageUrl);
                }
            }
        } catch (Exception ignored) {
        }
        assetImageBySku = map;
    }
}
