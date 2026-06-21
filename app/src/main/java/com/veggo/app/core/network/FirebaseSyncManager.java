package com.veggo.app.core.network;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.data.local.entity.ProductEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private final FirebaseFirestore firestore;
    private final VeggoDatabase database;
    private final ExecutorService executor;

    private static FirebaseSyncManager instance;

    private FirebaseSyncManager(Context context) {
        firestore = FirebaseFirestore.getInstance();
        database = VeggoDatabase.getInstance(context.getApplicationContext());
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncManager(context);
        }
        return instance;
    }

    /**
     * Đồng bộ bảng Products từ Firestore về Room Database
     */
    public void syncProducts() {
        Log.d(TAG, "Starting Product Sync from Firestore...");
        firestore.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe Firestore products", error);
                        return;
                    }

                    if (value != null) {
                        executor.execute(() -> processProductSnapshot(value));
                    }
                });
    }

    private void processProductSnapshot(QuerySnapshot snapshot) {
        List<ProductEntity> activeProducts = new ArrayList<>();
        List<String> inactiveProductIds = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Boolean isActive = doc.getBoolean("isActive");
            // Soft Delete check: Mặc định là true nếu null
            if (isActive != null && !isActive) {
                inactiveProductIds.add(doc.getId());
                continue;
            }

            try {
                // Ánh xạ dữ liệu
                String id = doc.getId();
                String name = doc.getString("name");
                Long priceL = doc.getLong("price");
                Long originalPriceL = doc.getLong("originalPrice");
                String sku = doc.getString("sku");
                String imageUrl = doc.getString("imageUrl");
                
                // Firestore might store numbers as Strings or vice-versa. 
                // Using doc.get() and manual conversion to be safe.
                Object weightObj = doc.get("weight");
                String weight = weightObj != null ? String.valueOf(weightObj) : null;

                Object weightOptionsRaw = doc.get("WeightOptions");
                List<String> weightOptions = new ArrayList<>();
                if (weightOptionsRaw instanceof List) {
                    List<?> list = (List<?>) weightOptionsRaw;
                    for (Object item : list) {
                        if (item != null) {
                            weightOptions.add(String.valueOf(item));
                        }
                    }
                }

                Double ratingD = doc.getDouble("rating");
                Long reviewCountL = doc.getLong("reviewCount");
                Long soldCountL = doc.getLong("soldCount");
                String desc = doc.getString("description");
                String origin = doc.getString("origin");
                String condition = doc.getString("condition");
                String fatContent = doc.getString("fatContent");
                String catId = doc.getString("categoryId");
                String subId = doc.getString("subcategoryId");

                long price = priceL != null ? priceL : 0;
                long originalPrice = originalPriceL != null ? originalPriceL : price;
                float rating = ratingD != null ? ratingD.floatValue() : 4.5f;
                int reviewCount = reviewCountL != null ? reviewCountL.intValue() : 0;
                int soldCount = soldCountL != null ? soldCountL.intValue() : 0;

                String weightOptionsJson = null;
                if (weightOptions != null && !weightOptions.isEmpty()) {
                    List<Double> values = new ArrayList<>();
                    for (String option : weightOptions) {
                        try {
                            values.add(Double.parseDouble(option));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (!values.isEmpty()) {
                        weightOptionsJson = new com.google.gson.Gson().toJson(values);
                    }
                }

                Double carbonPointD = doc.getDouble("CarbonSavingPoint");
                double carbonSavingPoint = carbonPointD != null ? carbonPointD : 0.0;

                ProductEntity entity = new ProductEntity(
                        id, name, price, originalPrice, sku, imageUrl, weightOptionsJson, weight,
                        rating, reviewCount, soldCount, desc, origin, condition, fatContent,
                        catId, subId, carbonSavingPoint
                );
                activeProducts.add(entity);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse Product " + doc.getId(), e);
            }
        }

        // Lưu vào Room
        database.runInTransaction(() -> {
            // Cập nhật các sản phẩm active
            for (ProductEntity product : activeProducts) {
                database.productDao().upsert(product);
            }
        });
        
        Log.d(TAG, "Đã đồng bộ xong " + activeProducts.size() + " products từ Firestore vào Room");
    }
}
