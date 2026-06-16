package com.veggo.app.core.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {
    private static final int BCRYPT_COST = 10;

    private PasswordUtils() {
    }

    @NonNull
    public static String hashPassword(@NonNull String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public static boolean verifyPassword(@NonNull String plainPassword, @Nullable String storedPassword) {
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return verifyBcrypt(plainPassword, storedPassword);
        }

        return hashPassword(plainPassword).equals(storedPassword);
    }

    @NonNull
    public static String hashPasswordForStorage(@NonNull String plainPassword) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(BCRYPT_COST));
    }

    private static boolean verifyBcrypt(@NonNull String plainPassword, @NonNull String storedPassword) {
        try {
            String normalizedHash = normalizeBcryptHash(storedPassword);
            return org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, normalizedHash);
        } catch (Exception exception) {
            return false;
        }
    }

    @NonNull
    private static String normalizeBcryptHash(@NonNull String storedPassword) {
        if (storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return "$2a$" + storedPassword.substring(4);
        }
        return storedPassword;
    }
}
