package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.veggo.app.core.database.AssetDatabaseSeeder;

public class VeggoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Seed on a background thread FIRST — Room cannot be accessed on the main thread.
        // seedIfNeeded() uses an internal executor and is safe to call here.
        // Firebase sync starts after seeding to avoid overwriting freshly-seeded data.
        AssetDatabaseSeeder.seedIfNeeded(this);

        com.veggo.app.core.network.FirebaseSyncManager.getInstance(this).syncProducts();
    }
}
