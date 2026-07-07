package com.veggo.app.core.utils;

public final class UrlUtils {
    private UrlUtils() {}

    public static String socketBaseUrlFromApiBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null) return null;
        String url = apiBaseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/api")) url = url.substring(0, url.length() - 4);
        return url;
    }
}

