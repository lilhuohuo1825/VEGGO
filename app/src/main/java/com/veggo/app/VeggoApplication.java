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

        // Seed xong trước khi auth/profile thao tác; mở DB trước để migration chạy,
        // rồi seed lại nếu destructive migration vừa reset cờ seeded.
        AssetDatabaseSeeder.seedIfNeededBlocking(this);
        com.veggo.app.core.database.VeggoDatabase.getInstance(this);
        AssetDatabaseSeeder.seedIfNeededBlocking(this);

        com.veggo.app.core.network.FirebaseSyncManager.getInstance(this).syncProducts();

        // Lên lịch kiểm tra nguyên liệu sắp hết hạn mỗi ngày
        FridgeExpiryScheduler.scheduleDailyCheck(this);
    }
}
