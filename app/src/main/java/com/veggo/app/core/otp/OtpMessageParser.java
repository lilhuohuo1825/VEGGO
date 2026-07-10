package com.veggo.app.core.otp;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a 6-digit OTP code from VEGGO SMS/notification message bodies.
 */
public final class OtpMessageParser {

    private static final Pattern VEGGO_OTP_PATTERN = Pattern.compile(
            "(?i)VEGGO:.*?\\b(\\d{6})\\b",
            Pattern.DOTALL
    );
    private static final Pattern GENERIC_OTP_PATTERN = Pattern.compile(
            "(?i)(?:OTP|ma\\s*xac\\s*thuc|verification\\s*code|code)\\D{0,24}(\\d{6})\\b"
    );
    private static final Pattern STANDALONE_SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    private OtpMessageParser() {
    }

    @Nullable
    public static String extract(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        String normalized = message.trim();

        Matcher veggoMatcher = VEGGO_OTP_PATTERN.matcher(normalized);
        if (veggoMatcher.find()) {
            return veggoMatcher.group(1);
        }

        Matcher genericMatcher = GENERIC_OTP_PATTERN.matcher(normalized);
        if (genericMatcher.find()) {
            return genericMatcher.group(1);
        }

        Matcher digitsMatcher = STANDALONE_SIX_DIGITS.matcher(normalized);
        if (digitsMatcher.find()) {
            return digitsMatcher.group(1);
        }
        return null;
    }

    public static boolean isValidOtp(@Nullable String otp) {
        return otp != null && otp.matches("\\d{6}");
    }
}
