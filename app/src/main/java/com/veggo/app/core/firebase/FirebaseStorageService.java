package com.veggo.app.core.firebase;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseStorageService {
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public StorageReference productImagesRef() {
        return storage.getReference().child(FirebaseConfig.PRODUCT_IMAGES_PATH);
    }
}
