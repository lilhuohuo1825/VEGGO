package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.veggo.app.core.database.AssetDatabaseSeeder;
import com.veggo.app.core.notification.FridgeExpiryScheduler;
import com.veggo.app.core.notification.RecurringConfirmationScheduler;
import com.veggo.app.di.AppModule;

public class VeggoApplication extends Application {
    private static VeggoApplication instance;

    public static VeggoApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Override CursorWindow size globally (100MB) to prevent SQLiteBlobTooBigException
        try {
            java.lang.reflect.Field field = android.database.CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 100 * 1024 * 1024); // 100MB
        } catch (Exception e) {
            android.util.Log.e("VeggoApplication", "Failed to override CursorWindow size", e);
        }

        // Prefetch image resolver cache on a background thread
        com.veggo.app.core.utils.ProductCatalogImageResolver.prefetchAll(this);

        FirebaseApp.initializeApp(this);

        // Seed xong trước khi auth/profile thao tác; mở DB trước để migration chạy,
        // rồi seed lại nếu destructive migration vừa reset cờ seeded.
        AssetDatabaseSeeder.seedIfNeededBlocking(this);
        com.veggo.app.core.database.VeggoDatabase.getInstance(this);
        AssetDatabaseSeeder.seedIfNeededBlocking(this);

        AppModule.provideProductRepository(this).refreshProducts();

        // Lên lịch kiểm tra nguyên liệu sắp hết hạn mỗi ngày
        FridgeExpiryScheduler.scheduleDailyCheck(this);
        RecurringConfirmationScheduler.scheduleDailyCheck(this);
        RecurringConfirmationScheduler.runCheckNow(this);
    }
}
