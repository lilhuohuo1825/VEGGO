package com.veggo.app.core.utils;

import com.veggo.app.BuildConfig;

public final class Constants {
    /**
     * API URL lấy từ local.properties lúc build (Gradle → BuildConfig):
     * - dev.api.mode=emulator  → http://10.0.2.2:5001/api/
     * - dev.api.mode=physical  → http://{dev.api.host}:5001/api/
     * - hoặc api.base.url=http://<IP>:5001/api/
     *
     * Đổi IP Wi-Fi: sửa local.properties rồi Rebuild — không cần sửa file Java.
     */
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;

    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CARTS = "carts";
    public static final String COLLECTION_ORDERS = "orders";

    private Constants() {
    }
}
