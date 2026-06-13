package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.veggo.app.core.database.AssetDatabaseSeeder;

public class VeggoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        // Reseed to ensure new review/recipe relations are applied
        AssetDatabaseSeeder.reseed(this);
    }
}
