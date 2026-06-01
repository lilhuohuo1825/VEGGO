package com.veggo.app.core.utils;

import android.util.Patterns;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.trim().length() >= 9;
    }
}
