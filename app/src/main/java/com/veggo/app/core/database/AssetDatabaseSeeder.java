package com.veggo.app.core.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.UserEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AssetDatabaseSeeder {
    private static final String TAG = "AssetDatabaseSeeder";
    private static final String PREFS_NAME = "veggo_asset_database_seed";
    private static final String KEY_SEEDED_DATABASE_VERSION = "seeded_database_version";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private AssetDatabaseSeeder() {
    }

    public static void seedIfNeeded(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int seededVersion = preferences.getInt(KEY_SEEDED_DATABASE_VERSION, 0);
        if (seededVersion >= DatabaseManager.DATABASE_VERSION) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                seed(appContext);
                preferences.edit()
                        .putInt(KEY_SEEDED_DATABASE_VERSION, DatabaseManager.DATABASE_VERSION)
                        .apply();
            } catch (Exception exception) {
                Log.e(TAG, "Cannot seed SQLite database from JSON assets", exception);
            }
        });
    }

    public static void reseed(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                DatabaseManager.clearLocalData(appContext);
                seed(appContext);
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_SEEDED_DATABASE_VERSION, DatabaseManager.DATABASE_VERSION)
                        .apply();
            } catch (Exception exception) {
                Log.e(TAG, "Cannot reseed SQLite database from JSON assets", exception);
            }
        });
    }

    private static void seed(Context context) throws Exception {
        VeggoDatabase database = VeggoDatabase.getInstance(context);
        AssetJsonLoader loader = new AssetJsonLoader(context);

        List<ProductEntity> products = readProducts(loader);
        List<BlogEntity> blogs = readBlogs(loader);
        List<UserEntity> users = readUsers(loader);
        List<AssetRecordEntity> assetRecords = readAssetRecords(loader);

        database.runInTransaction(() -> {
            database.assetRecordDao().clearAll();
            if (!assetRecords.isEmpty()) {
                database.assetRecordDao().insertAll(assetRecords);
            }
            if (!products.isEmpty()) {
                database.productDao().insertAll(products);
            }
            if (!blogs.isEmpty()) {
                database.blogDao().insertAll(blogs);
            }
            for (UserEntity user : users) {
                database.userDao().upsert(user);
            }
        });
        AssetDataStore.replaceAll(context, assetRecords);
    }

    private static List<ProductEntity> readProducts(AssetJsonLoader loader) throws Exception {
        List<AssetModels.Product> assetProducts = loader.readList(
                AssetFiles.PRODUCTS,
                AssetModels.Product.class
        );
        List<ProductEntity> products = new ArrayList<>();
        for (AssetModels.Product assetProduct : assetProducts) {
            String id = firstNonEmpty(assetProduct.objectId, assetProduct.sku);
            if (id == null) {
                continue;
            }
            products.add(new ProductEntity(
                    id,
                    assetProduct.productName,
                    assetProduct.price,
                    firstImage(assetProduct.image)
            ));
        }
        return products;
    }

    private static List<BlogEntity> readBlogs(AssetJsonLoader loader) throws Exception {
        List<AssetModels.Blog> assetBlogs = loader.readList(
                AssetFiles.BLOGS,
                AssetModels.Blog.class
        );
        List<BlogEntity> blogs = new ArrayList<>();
        Set<String> usedImages = new HashSet<>();
        int fallbackIndex = 0;
        for (AssetModels.Blog assetBlog : assetBlogs) {
            String id = firstNonEmpty(objectIdValue(assetBlog.objectId), assetBlog.id);
            if (id == null) {
                continue;
            }
            String publishedAt = assetBlog.pubDate == null ? null : assetBlog.pubDate.date;
            String imageUrl = normalizedImage(assetBlog.img, assetBlog.categoryTag, usedImages, fallbackIndex);
            if (!imageUrl.equals(assetBlog.img)) {
                fallbackIndex++;
            }
            usedImages.add(imageUrl);
            blogs.add(new BlogEntity(
                    id,
                    imageUrl,
                    assetBlog.title,
                    assetBlog.excerpt,
                    publishedAt,
                    parseMillis(publishedAt),
                    assetBlog.author,
                    assetBlog.categoryTag,
                    assetBlog.content
            ));
        }
        return blogs;
    }

    private static String normalizedImage(String imageUrl, String category, Set<String> usedImages, int fallbackIndex) {
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !usedImages.contains(imageUrl)) {
            return imageUrl;
        }
        String[] pool = fallbackImages(category);
        for (int offset = 0; offset < pool.length; offset++) {
            String candidate = pool[Math.abs(fallbackIndex + offset) % pool.length];
            if (!usedImages.contains(candidate)) {
                return candidate;
            }
        }
        return pool[Math.abs(fallbackIndex) % pool.length];
    }

    private static String[] fallbackImages(String category) {
        String cleanCategory = category == null ? "" : category.toLowerCase();
        if (cleanCategory.contains("nấm") || cleanCategory.contains("náº¥m")) {
            return MUSHROOM_IMAGES;
        }
        if (cleanCategory.contains("trái") || cleanCategory.contains("trÃ¡i")) {
            return FRUIT_IMAGES;
        }
        if (cleanCategory.contains("rau")) {
            return VEGETABLE_IMAGES;
        }
        if (cleanCategory.contains("nông") || cleanCategory.contains("nÃ´ng")) {
            return FARM_IMAGES;
        }
        return NUTRITION_IMAGES;
    }

    private static final String[] VEGETABLE_IMAGES = {
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1566385101042-1a0aa0c1268c?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1598170845058-32b996a69f76?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] FRUIT_IMAGES = {
            "https://images.unsplash.com/photo-1610832958506-aa56368176cf?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1579613832125-5d34a13e691b?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] MUSHROOM_IMAGES = {
            "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1607877742574-a2f1f62f57f6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1512595765784-5ebad80772a6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1603048719539-9ecb4aa395e3?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] FARM_IMAGES = {
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1492496913980-501348b61469?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1523741543316-beb7fc7023d8?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] NUTRITION_IMAGES = {
            "https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1543362906-acfc16c67564?auto=format&fit=crop&w=1200&q=80"
    };

    private static List<UserEntity> readUsers(AssetJsonLoader loader) throws Exception {
        List<AssetModels.User> assetUsers = loader.readList(
                AssetFiles.USERS,
                AssetModels.User.class
        );
        List<UserEntity> users = new ArrayList<>();
        for (AssetModels.User assetUser : assetUsers) {
            String id = firstNonEmpty(assetUser.customerId, assetUser.objectId);
            if (id == null) {
                continue;
            }
            users.add(new UserEntity(id, assetUser.fullName, assetUser.email));
        }
        return users;
    }

    private static List<AssetRecordEntity> readAssetRecords(AssetJsonLoader loader) {
        List<AssetRecordEntity> records = new ArrayList<>();
        long importedAt = System.currentTimeMillis();

        for (String fileName : AssetFiles.SEED_FILES) {
            try {
                String collection = collectionName(fileName);
                JsonElement root = loader.readJson(fileName);
                if (root == null || root.isJsonNull()) {
                    continue;
                }

                if (root.isJsonArray()) {
                    JsonArray array = root.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        records.add(new AssetRecordEntity(
                                collection,
                                documentId(array.get(i), i),
                                array.get(i).toString(),
                                importedAt
                        ));
                    }
                } else if (root.isJsonObject()) {
                    records.add(new AssetRecordEntity(
                            collection,
                            documentId(root, 0),
                            root.toString(),
                            importedAt
                    ));
                } else {
                    records.add(new AssetRecordEntity(collection, "value", root.toString(), importedAt));
                }
            } catch (Exception exception) {
                Log.e(TAG, "Cannot seed asset file " + fileName, exception);
            }
        }
        return records;
    }

    private static String documentId(JsonElement element, int index) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String id = firstNonEmpty(
                    stringValue(object, "_id"),
                    firstNonEmpty(
                            stringValue(object, "id"),
                            firstNonEmpty(
                                    stringValue(object, "sku"),
                                    firstNonEmpty(
                                            stringValue(object, "OrderID"),
                                            firstNonEmpty(
                                                    stringValue(object, "CustomerID"),
                                                    stringValue(object, "promotion_id")
                                            )
                                    )
                            )
                    )
            );
            if (id != null) {
                return id;
            }
        }
        return "row_" + index;
    }

    private static String stringValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }

        JsonElement value = object.get(key);
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject nestedObject = value.getAsJsonObject();
            if (nestedObject.has("$oid") && !nestedObject.get("$oid").isJsonNull()) {
                return nestedObject.get("$oid").getAsString();
            }
        }
        return null;
    }

    private static String collectionName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, extensionIndex);
    }

    private static String firstImage(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0);
    }

    private static long parseMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private static String objectIdValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("$oid") && !object.get("$oid").isJsonNull()) {
                return object.get("$oid").getAsString();
            }
        }
        return null;
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}
