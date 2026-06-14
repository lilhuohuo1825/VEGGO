package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.veggo.app.core.database.AssetDatabaseSeeder;

public class VeggoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        
        // Bật đồng bộ dữ liệu Realtime từ Firebase xuống Room Database (Hiển thị UI)
        com.veggo.app.core.network.FirebaseSyncManager.getInstance(this).syncProducts();
        
        // Seed các dữ liệu cứng (Categories, v.v...) nếu là lần cài đặt đầu tiên
        com.veggo.app.core.database.AssetDatabaseSeeder.seedIfNeeded(this);
    }
}