package com.veggo.app.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "veggo_preferences";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAccessToken(String token) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public void saveUserId(String userId) {
        preferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getCustomerId() {
        return getUserId();
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }
}
