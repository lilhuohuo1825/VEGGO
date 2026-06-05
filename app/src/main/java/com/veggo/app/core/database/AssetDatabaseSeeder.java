package com.veggo.app.core.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.UserEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
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
