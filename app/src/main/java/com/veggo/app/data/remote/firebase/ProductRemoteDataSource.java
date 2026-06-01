package com.veggo.app.data.remote.firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.veggo.app.core.utils.Constants;

public class ProductRemoteDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseFirestore productsCollection() {
        return firestore;
    }

    public String collectionName() {
        return Constants.COLLECTION_PRODUCTS;
    }
}
