package com.veggo.app.data.remote.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veggo.app.core.utils.Constants;
import com.veggo.app.data.remote.dto.ProductDto;

import java.util.ArrayList;
import java.util.List;

public class ProductRemoteDataSource {
    private static final String TAG = "ProductRemoteData";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseFirestore productsCollection() {
        return firestore;
    }

    public String collectionName() {
        return Constants.COLLECTION_PRODUCTS;
    }

    public void getProducts(OnSuccessListener<List<ProductDto>> onSuccess, OnFailureListener onFailure) {
        firestore.collection(collectionName())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductDto> products = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        ProductDto product = document.toObject(ProductDto.class);
                        if (product == null) {
                            continue;
                        }

                        if (isBlank(product.getId())) {
                            Object explicitId = document.get("_id");
                            product.setId(explicitId != null ? String.valueOf(explicitId) : document.getId());
                        }

                        if (isBlank(product.getName())) {
                            Object name = firstNonNull(
                                    document.get("name"),
                                    document.get("productName"),
                                    document.get("title")
                            );
                            if (name != null) {
                                product.setName(String.valueOf(name));
                            }
                        }

                        if (isBlank(product.getImageUrl())) {
                            Object imageUrl = firstNonNull(
                                    document.get("imageUrl"),
                                    document.get("image_url"),
                                    document.get("image")
                            );
                            if (imageUrl != null) {
                                product.setImageUrl(String.valueOf(imageUrl));
                            }
                        }

                        Object price = document.get("price");
                        if (price instanceof Number) {
                            product.setPrice(((Number) price).longValue());
                        } else if (price instanceof String) {
                            product.setPrice(parsePrice((String) price));
                        }

                        Boolean active = product.getActive();
                        if (active == null) {
                            active = readBoolean(document, "isActive", "active");
                        }

                        if (!Boolean.FALSE.equals(active) && !isBlank(product.getName())) {
                            products.add(product);
                        }
                    }
                    Log.d(TAG, "Loaded " + products.size() + " products from Firestore collection " + collectionName());
                    onSuccess.onSuccess(products);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to load Firestore products from " + collectionName(), error);
                    onFailure.onFailure(error);
                });
    }

    private static Boolean readBoolean(DocumentSnapshot document, String firstKey, String secondKey) {
        Object first = document.get(firstKey);
        if (first instanceof Boolean) {
            return (Boolean) first;
        }

        Object second = document.get(secondKey);
        if (second instanceof Boolean) {
            return (Boolean) second;
        }
        return null;
    }

    private static Object firstNonNull(Object first, Object second, Object third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static long parsePrice(String value) {
        if (isBlank(value)) {
            return 0L;
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
