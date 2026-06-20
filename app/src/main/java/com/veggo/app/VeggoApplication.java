package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.veggo.app.core.database.AssetDatabaseSeeder;
import com.veggo.app.core.notification.FridgeExpiryScheduler;

public class VeggoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        com.veggo.app.core.network.FirebaseSyncManager.getInstance(this).syncProducts();

        // Seed xong trước khi auth/profile thao tác để tránh bị tiến trình nền ghi đè.
        AssetDatabaseSeeder.seedIfNeededBlocking(this);

        // Lên lịch kiểm tra nguyên liệu sắp hết hạn mỗi ngày
        FridgeExpiryScheduler.scheduleDailyCheck(this);
    }
}
