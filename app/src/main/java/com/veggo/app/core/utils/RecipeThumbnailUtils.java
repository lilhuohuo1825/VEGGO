package com.veggo.app.core.utils;

import androidx.annotation.Nullable;

public final class RecipeThumbnailUtils {
    private RecipeThumbnailUtils() {}

    @Nullable
    public static String resolveThumbnail(@Nullable String videoUrl) {
        String videoId = extractYoutubeId(videoUrl);
        if (videoId != null) {
            return "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";
        }
        return null;
    }

    @Nullable
    public static String extractYoutubeId(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        try {
            if (trimmed.contains("/embed/")) {
                String temp = trimmed.split("/embed/")[1];
                return stripQuery(temp);
            }
            if (trimmed.contains("youtu.be/")) {
                String temp = trimmed.split("youtu.be/")[1];
                return stripQuery(temp);
            }
            if (trimmed.contains("watch?v=")) {
                String temp = trimmed.split("watch\\?v=")[1];
                return stripQuery(temp);
            }
            if (trimmed.contains("/vi/")) {
                String temp = trimmed.split("/vi/")[1];
                return stripQuery(temp);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @Nullable
    private static String stripQuery(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int queryIndex = value.indexOf('?');
        String id = queryIndex >= 0 ? value.substring(0, queryIndex) : value;
        int slashIndex = id.indexOf('/');
        if (slashIndex >= 0) {
            id = id.substring(0, slashIndex);
        }
        return id.isEmpty() ? null : id;
    }
}
