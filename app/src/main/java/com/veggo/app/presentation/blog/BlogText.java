package com.veggo.app.presentation.blog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
            return new SimpleDateFormat("dd MMM, yyyy - h:mm a", Locale.ENGLISH).format(date);
        } catch (Exception exception) {
            return isoDate;
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
