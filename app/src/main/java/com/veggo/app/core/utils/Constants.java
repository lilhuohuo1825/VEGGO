package com.veggo.app.core.utils;

public final class Constants {
    /**
     * Chọn môi trường chạy app:
     * - false → emulator: dùng 10.0.2.2 (mặc định)
     * - true  → máy thật: đặt PHYSICAL_DEVICE_HOST = IP LAN của Mac
     */
    private static final boolean USE_PHYSICAL_DEVICE = false;
    private static final String PHYSICAL_DEVICE_HOST = "10.43.177.109";

    // Emulator default.
    public static final String API_BASE_URL = USE_PHYSICAL_DEVICE
            ? "http://" + PHYSICAL_DEVICE_HOST + ":5001/api/"
            : "http://10.0.2.2:5001/api/";

    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CARTS = "carts";
    public static final String COLLECTION_ORDERS = "orders";

    private Constants() {
    }
}
