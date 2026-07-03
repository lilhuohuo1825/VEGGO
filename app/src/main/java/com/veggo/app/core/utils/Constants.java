package com.veggo.app.core.utils;

public final class Constants {
    /**
     * Chọn môi trường chạy app:
     * - false → emulator: dùng 10.0.2.2 (mặc định)
     * - true  → máy thật: đặt PHYSICAL_DEVICE_HOST = IP LAN máy dev đang chạy backend
     */
    private static final boolean USE_PHYSICAL_DEVICE = false;

    // IP LAN các máy dev (bật USE_PHYSICAL_DEVICE = true và chọn IP tương ứng khi test máy thật)
    private static final String PHYSICAL_DEVICE_HOST = "10.43.177.109";
    // private static final String PHYSICAL_DEVICE_HOST = "10.0.1.184";
    // private static final String PHYSICAL_DEVICE_HOST = "10.43.182.30";
    // private static final String PHYSICAL_DEVICE_HOST = "172.20.10.2";
    // private static final String PHYSICAL_DEVICE_HOST = "10.0.44.124";
    // private static final String PHYSICAL_DEVICE_HOST = "10.0.23.230";

    // Emulator (Android Studio AVD) — đang dùng mặc định
    // public static final String API_BASE_URL = "http://10.0.2.2:5001/api/";

    // Máy thật — uncomment IP phù hợp hoặc dùng USE_PHYSICAL_DEVICE + PHYSICAL_DEVICE_HOST ở trên
    // public static final String API_BASE_URL = "http://10.0.1.184:5001/api/";
    // public static final String API_BASE_URL = "http://10.43.182.30:5001/api/";
    // public static final String API_BASE_URL = "http://10.43.177.109:5001/api/";
    // public static final String API_BASE_URL = "http://172.20.10.2:5001/api/";
    // public static final String API_BASE_URL = "http://10.0.44.124:5001/api/";
    // public static final String API_BASE_URL = "http://10.0.23.230:5001/api/";

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
