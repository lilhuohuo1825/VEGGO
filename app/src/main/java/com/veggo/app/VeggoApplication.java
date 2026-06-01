package com.veggo.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class VeggoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
