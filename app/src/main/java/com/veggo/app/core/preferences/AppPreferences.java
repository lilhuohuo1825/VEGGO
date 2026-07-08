package com.veggo.app.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AppPreferences {
    private static final String PREF_NAME = "veggo_app_preferences";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_CURRENT_PHONE = "currentPhone";
    private static final String KEY_CUSTOMER_ID = "currentCustomerId";
    private static final String KEY_FULL_NAME = "currentFullName";
    private static final String KEY_EMAIL = "currentEmail";
    private static final String KEY_AVATAR_URL = "currentAvatarUrl";
    private static final String KEY_BIRTHDAY = "currentBirthday";
    private static final String KEY_GENDER = "currentGender";

    private final SharedPreferences sharedPreferences;

    public AppPreferences(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLoginSession(@NonNull String phone, String customerId, String fullName, String email) {
        saveLoginSession(phone, customerId, fullName, email, null);
    }

    public void saveLoginSession(
            @NonNull String phone,
            String customerId,
            String fullName,
            String email,
            @Nullable String avatarUrl
    ) {
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_CURRENT_PHONE, phone)
                .putString(KEY_CUSTOMER_ID, customerId)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .apply();
    }

    public void saveProfileSession(
            @NonNull String phone,
            String fullName,
            String email,
            @Nullable String avatarUrl,
            @Nullable String birthday,
            @Nullable String gender
    ) {
        sharedPreferences.edit()
                .putString(KEY_CURRENT_PHONE, phone)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .putString(KEY_BIRTHDAY, birthday)
                .putString(KEY_GENDER, gender)
                .apply();
    }

    public void saveProfileSession(@NonNull String phone, String fullName, String email, @Nullable String avatarUrl) {
        saveProfileSession(phone, fullName, email, avatarUrl, null, null);
    }

    public void saveLoginSession(@NonNull String phone) {
        saveLoginSession(phone, null, null, null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    @Nullable
    public String getCurrentPhone() {
        return sharedPreferences.getString(KEY_CURRENT_PHONE, null);
    }

    @Nullable
    public String getCustomerId() {
        return sharedPreferences.getString(KEY_CUSTOMER_ID, null);
    }

    @Nullable
    public String getFullName() {
        return sharedPreferences.getString(KEY_FULL_NAME, null);
    }

    @Nullable
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    @Nullable
    public String getAvatarUrl() {
        return sharedPreferences.getString(KEY_AVATAR_URL, null);
    }

    @Nullable
    public String getBirthday() {
        return sharedPreferences.getString(KEY_BIRTHDAY, null);
    }

    @Nullable
    public String getGender() {
        return sharedPreferences.getString(KEY_GENDER, null);
    }

    public void logout() {
        clearLoginSession();
    }

    public void clearLoginSession() {
        sharedPreferences.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_CURRENT_PHONE)
                .remove(KEY_CUSTOMER_ID)
                .remove(KEY_FULL_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_AVATAR_URL)
                .remove(KEY_BIRTHDAY)
                .remove(KEY_GENDER)
                .apply();
    }

    public boolean isBiometricEnabled(String customerId) {
        if (customerId == null) return false;
        return sharedPreferences.getBoolean("biometric_enabled_" + customerId, false);
    }

    public void setBiometricEnabled(String customerId, boolean enabled) {
        if (customerId == null) return;
        sharedPreferences.edit()
                .putBoolean("biometric_enabled_" + customerId, enabled)
                .apply();
    }

    @Nullable
    public String getBiometricPin(String customerId) {
        if (customerId == null) return null;
        String encryptedPin = sharedPreferences.getString("biometric_pin_" + customerId, null);
        if (encryptedPin == null) return null;
        return com.veggo.app.core.security.BiometricCryptographyHelper.decrypt(encryptedPin);
    }

    public void setBiometricPin(String customerId, String pin) {
        if (customerId == null || pin == null) return;
        String encryptedPin = com.veggo.app.core.security.BiometricCryptographyHelper.encrypt(pin);
        sharedPreferences.edit()
                .putString("biometric_pin_" + customerId, encryptedPin)
                .apply();
    }

    public void clearBiometric(String customerId) {
        if (customerId == null) return;
        sharedPreferences.edit()
                .remove("biometric_enabled_" + customerId)
                .remove("biometric_pin_" + customerId)
                .apply();
    }
}
