package com.veggo.app.presentation.profile;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VeggoPayQrParser {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(0\\d{9,10})");

    private VeggoPayQrParser() {
    }

    static final class Payload {
        @Nullable final String phone;
        @Nullable final String amount;
        @Nullable final String description;

        Payload(@Nullable String phone, @Nullable String amount, @Nullable String description) {
            this.phone = phone;
            this.amount = amount;
            this.description = description;
        }
    }

    @Nullable
    static Payload parse(@NonNull String rawValue) {
        String raw = rawValue.trim();
        if (raw.isEmpty()) {
            return null;
        }

        Payload jsonPayload = parseJson(raw);
        if (jsonPayload != null) {
            return jsonPayload;
        }

        Payload uriPayload = parseUri(raw);
        if (uriPayload != null) {
            return uriPayload;
        }

        Matcher matcher = PHONE_PATTERN.matcher(raw);
        if (matcher.find()) {
            return new Payload(matcher.group(1), null, null);
        }

        return null;
    }

    @Nullable
    private static Payload parseJson(@NonNull String raw) {
        if (!raw.startsWith("{")) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            String phone = firstNonBlank(
                    json.optString("recipientPhone", null),
                    json.optString("phone", null),
                    json.optString("Phone", null)
            );
            String amount = firstNonBlank(
                    json.optString("amount", null),
                    json.optString("Amount", null)
            );
            String description = firstNonBlank(
                    json.optString("description", null),
                    json.optString("note", null),
                    json.optString("message", null)
            );
            if (TextUtils.isEmpty(phone)) {
                return null;
            }
            return new Payload(phone, amount, description);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Payload parseUri(@NonNull String raw) {
        try {
            Uri uri = Uri.parse(raw);
            String phone = firstNonBlank(
                    uri.getQueryParameter("recipientPhone"),
                    uri.getQueryParameter("phone")
            );
            if (TextUtils.isEmpty(phone)) {
                return null;
            }
            return new Payload(
                    phone,
                    uri.getQueryParameter("amount"),
                    firstNonBlank(uri.getQueryParameter("description"), uri.getQueryParameter("note"))
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
