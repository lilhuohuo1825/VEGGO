package com.veggo.app.core.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.utils.ProductCatalogImageResolver;
import com.veggo.app.core.utils.ProductDisplayValidator;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.RecipeEntity;
import com.veggo.app.data.local.entity.ReviewEntity;
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
    private static final String KEY_SEEDED_ASSET_VERSION = "seeded_asset_version";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private AssetDatabaseSeeder() {
    }

    public static void seedIfNeeded(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int seededVersion = preferences.getInt(KEY_SEEDED_ASSET_VERSION, 0);
        if (seededVersion >= DatabaseManager.ASSET_SEED_VERSION) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                seed(appContext);
                preferences.edit()
                        .putInt(KEY_SEEDED_ASSET_VERSION, DatabaseManager.ASSET_SEED_VERSION)
                        .apply();
            } catch (Exception exception) {
                Log.e(TAG, "Cannot seed SQLite database from JSON assets", exception);
            }
        });
    }

    public static void seedIfNeededBlocking(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int seededVersion = preferences.getInt(KEY_SEEDED_ASSET_VERSION, 0);
        if (seededVersion >= DatabaseManager.ASSET_SEED_VERSION) {
            return;
        }
        try {
            seed(appContext);
            preferences.edit()
                    .putInt(KEY_SEEDED_ASSET_VERSION, DatabaseManager.ASSET_SEED_VERSION)
                    .apply();
        } catch (Exception exception) {
            Log.e(TAG, "Cannot seed SQLite database from JSON assets", exception);
        }
    }

    public static void reseed(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                DatabaseManager.clearLocalData(appContext);
                seed(appContext);
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_SEEDED_ASSET_VERSION, DatabaseManager.ASSET_SEED_VERSION)
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
        List<RecipeEntity> recipes = readRecipes(loader, products);
        List<ReviewEntity> reviews = new ArrayList<>();
        applyReviewStats(products, reviews);

        database.runInTransaction(() -> {
            database.assetRecordDao().clearAll();
            if (!assetRecords.isEmpty()) {
                database.assetRecordDao().insertAll(assetRecords);
            }
            if (!products.isEmpty()) {
                database.productDao().insertAll(products);
            }
            if (!recipes.isEmpty()) {
                database.productDao().insertRecipes(recipes);
            }
            if (!reviews.isEmpty()) {
                database.productDao().insertReviews(reviews);
                for (ProductEntity product : products) {
                    database.productDao().updateProductReviewStats(product.getId());
                }
            }
            for (UserEntity user : users) {
                database.userDao().upsert(user);
            }
        });
        AssetDataStore.replaceAll(context, assetRecords);
        ProductCatalogImageResolver.invalidateAssetCache();
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

            // Xây dựng phần mô tả chi tiết từ nhiều nguồn để Product Detail render thành bảng.
            StringBuilder description = new StringBuilder();
            if (assetProduct.ingredients != null && !assetProduct.ingredients.isEmpty()) {
                description.append("Thành phần: ").append(assetProduct.ingredients).append("\n\n");
            }
            if (assetProduct.usage != null && !assetProduct.usage.isEmpty()) {
                description.append("Cách dùng: ").append(assetProduct.usage).append("\n\n");
            }
            if (assetProduct.storage != null && !assetProduct.storage.isEmpty()) {
                description.append("Cách bảo quản: ").append(assetProduct.storage).append("\n\n");
            }
            if (assetProduct.safetyWarning != null && !assetProduct.safetyWarning.isEmpty()) {
                description.append("Lưu ý khi sử dụng: ").append(assetProduct.safetyWarning).append("\n\n");
            }
            if (assetProduct.brand != null && !assetProduct.brand.isEmpty()) {
                description.append("Hãng: ").append(assetProduct.brand).append("\n\n");
            }
            if (assetProduct.producer != null && !assetProduct.producer.isEmpty()) {
                description.append("Nơi sản xuất: ").append(assetProduct.producer).append("\n\n");
            } else if (assetProduct.origin != null && !assetProduct.origin.isEmpty()) {
                description.append("Nơi sản xuất: ").append(assetProduct.origin).append("\n\n");
            }
            
            String finalDescription = description.toString().trim();
            if (finalDescription.isEmpty()) {
                finalDescription = "Chưa có mô tả chi tiết cho sản phẩm này.";
            }

            ProductEntity entity = new ProductEntity(
                    id,
                    assetProduct.productName != null ? assetProduct.productName : "Sản phẩm Veggo",
                    assetProduct.price,
                    assetProduct.basePrice > 0 ? assetProduct.basePrice : assetProduct.price,
                    assetProduct.sku,
                    firstImage(assetProduct.image),
                    assetProduct.weightOptions != null && !assetProduct.weightOptions.isEmpty()
                        ? new com.google.gson.Gson().toJson(parseWeightOptions(assetProduct.weightOptions))
                        : null,
                    assetProduct.weight != null ? assetProduct.weight : assetProduct.unit,
                    (float) assetProduct.rating,
                    assetProduct.liked,
                    assetProduct.purchaseCount,
                    finalDescription,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    assetProduct.origin != null ? assetProduct.origin : "Việt Nam",
                    assetProduct.status != null ? assetProduct.status : "Mới",
                    null,
                    assetProduct.categoryId,
                    assetProduct.subcategoryId,
                    assetProduct.brand != null ? assetProduct.brand : "Veggo",
                    assetProduct.carbonSavingPoint
            );
            if (ProductDisplayValidator.isDisplayable(entity)) {
                products.add(entity);
            }
        }
        return products;
    }

    private static void applyReviewStats(List<ProductEntity> products, List<ReviewEntity> reviews) {
        for (ProductEntity product : products) {
            float total = 0f;
            int count = 0;
            for (ReviewEntity review : reviews) {
                if (product.getId().equals(review.getProductId())) {
                    total += review.getRating();
                    count++;
                }
            }
            product.setRating(count > 0 ? total / count : 0f);
            product.setReviewCount(count);
        }
    }

    private static List<Double> parseWeightOptions(List<String> source) {
        List<Double> result = new ArrayList<>();
        for (String value : source) {
            try {
                result.add(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static List<RecipeEntity> readRecipes(AssetJsonLoader loader, List<ProductEntity> products) throws Exception {
        List<AssetModels.Instruction> assetInstructions = loader.readList(
                AssetFiles.INSTRUCTIONS,
                AssetModels.Instruction.class
        );
        List<RecipeEntity> recipes = new ArrayList<>();
        int productCount = products.size();
        
        for (int i = 0; i < assetInstructions.size(); i++) {
            AssetModels.Instruction asset = assetInstructions.get(i);
            String id = firstNonEmpty(asset.id, asset.dishName);
            if (id == null) continue;
            
            // Phân bổ công thức cho các sản phẩm để có dữ liệu hiển thị
            String linkedProductId = productCount > 0 ? products.get(i % productCount).getId() : null;
            
            recipes.add(new RecipeEntity(
                    id,
                    asset.dishName,
                    asset.image,
                    asset.cookingTime != null ? asset.cookingTime : "20min",
                    "6.00đ",
                    4.5f,
                    120,
                    false,
                    linkedProductId
            ));
        }
        return recipes;
    }

    private static List<ReviewEntity> generateSampleReviews(List<ProductEntity> products) {
        List<ReviewEntity> reviews = new ArrayList<>();
        String[] names = {"Ngọc Hân", "Trần Anh", "Lê Minh", "Hoàng Nam", "Thúy Vi"};
        String[] comments = {
            "Sản phẩm rất tươi ngon, giao hàng đúng hẹn.",
            "Chất lượng tuyệt vời, đóng gói rất kỹ lưỡng.",
            "Giá cả hợp lý, sẽ tiếp tục ủng hộ shop.",
            "Rất hài lòng với dịch vụ khách hàng.",
            "Thực phẩm sạch, nấu ăn rất yên tâm."
        };
        
        for (ProductEntity product : products) {
            for (int i = 0; i < 3; i++) {
                int idx = (product.getId().hashCode() + i) & 0x7FFFFFFF;
                reviews.add(new ReviewEntity(
                    "rev_" + product.getId() + "_" + i,
                    product.getId(),
                    names[idx % names.length],
                    (i + 1) + " ngày trước",
                    5.0f - (i * 0.5f),
                    comments[idx % comments.length],
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=100",
                    null
                ));
            }
        }
        return reviews;
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
        return ProductImageUtils.resolveImageUrl(images, null);
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
