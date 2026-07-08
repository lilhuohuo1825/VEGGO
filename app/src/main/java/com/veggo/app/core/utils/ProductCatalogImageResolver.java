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
    private static final java.util.concurrent.ConcurrentHashMap<String, String> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean isPrefetched = false;

    private ProductCatalogImageResolver() {
    }

    public static void prefetchAll(Context context) {
        if (context == null || isPrefetched) {
            return;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                // 1. Prefetch from JSON assets
                AssetJsonLoader loader = new AssetJsonLoader(appContext);
                List<AssetModels.Product> assetProducts = loader.readList(AssetFiles.PRODUCTS, AssetModels.Product.class);
                if (assetProducts != null) {
                    for (AssetModels.Product product : assetProducts) {
                        if (product.sku != null && !product.sku.trim().isEmpty()) {
                            String imageUrl = ProductImageUtils.resolveImageUrl(product.image, null);
                            if (ProductImageUtils.isHttpUrl(imageUrl)) {
                                cache.put(product.sku.trim(), imageUrl);
                            }
                        }
                    }
                }

                // 2. Prefetch from Room database
                ProductDao productDao = VeggoDatabase.getInstance(appContext).productDao();
                List<ProductEntity> entities = productDao.getAllProductEntities();
                if (entities != null) {
                    for (ProductEntity entity : entities) {
                        if (entity.getSku() != null && !entity.getSku().trim().isEmpty()) {
                            String imageUrl = entity.getImageUrl();
                            if (ProductImageUtils.isHttpUrl(imageUrl)) {
                                cache.put(entity.getSku().trim(), ProductImageUtils.normalizeUrl(imageUrl));
                            }
                        }
                    }
                }
                isPrefetched = true;
            } catch (Exception e) {
                android.util.Log.e("ProductCatalogImageResolver", "Error prefetching product images", e);
            }
        }).start();
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
        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }
        String key = sku.trim();
        
        // 1. Check in-memory cache
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        if (context == null) {
            return null;
        }

        // 2. Fallback to database query (only if cache missed/not yet loaded)
        try {
            ProductDao productDao = VeggoDatabase.getInstance(context.getApplicationContext()).productDao();
            String productId = productDao.getProductIdBySku(key);
            if (productId != null) {
                ProductEntity entity = productDao.getProductById(productId);
                if (entity != null && ProductDisplayValidator.hasValidImage(entity.getImageUrl())) {
                    String url = ProductImageUtils.normalizeUrl(entity.getImageUrl());
                    if (url != null) {
                        cache.put(key, url);
                        return url;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Fallback to assets
        String assetUrl = resolveFromAssets(context, key);
        if (assetUrl != null) {
            cache.put(key, assetUrl);
        }
        return assetUrl;
    }

    public static void invalidateAssetCache() {
        cache.clear();
        isPrefetched = false;
    }

    @Nullable
    private static String resolveFromAssets(Context context, String sku) {
        ensureAssetCache(context);
        return cache.get(sku);
    }

    private static synchronized void ensureAssetCache(Context context) {
        if (isPrefetched) {
            return;
        }
        try {
            AssetJsonLoader loader = new AssetJsonLoader(context.getApplicationContext());
            List<AssetModels.Product> products = loader.readList(AssetFiles.PRODUCTS, AssetModels.Product.class);
            for (AssetModels.Product product : products) {
                if (product.sku == null || product.sku.trim().isEmpty()) {
                    continue;
                }
                String imageUrl = ProductImageUtils.resolveImageUrl(product.image, null);
                if (ProductImageUtils.isHttpUrl(imageUrl)) {
                    cache.put(product.sku.trim(), imageUrl);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
