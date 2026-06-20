package com.veggo.app.presentation.blog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class BlogText {
    private BlogText() {
    }

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains("Ã") || value.contains("Ä") || value.contains("Æ")) {
            try {
                return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    public static String dateTime(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "";
        }
        try {
            Date date = Date.from(java.time.Instant.parse(isoDate));
            return new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(date);
        } catch (Exception exception) {
            return isoDate;
        }
    }

    public static String date(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "";
        }
        try {
            Date date = Date.from(java.time.Instant.parse(isoDate));
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        } catch (Exception exception) {
            return isoDate;
        }
    }

    public static String relativeTime(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "";
        }
        try {
            long diff = Math.max(0L, System.currentTimeMillis() - java.time.Instant.parse(isoDate).toEpochMilli());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes < 1) return "vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours < 24) return hours + " giờ trước";
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days < 7) return days + " ngày trước";
            return date(isoDate);
        } catch (Exception exception) {
            return date(isoDate);
        }
    }

    public static String objectIdValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("$oid") && !object.get("$oid").isJsonNull()) {
                return object.get("$oid").getAsString();
            }
        }
        return null;
    }
}
