package com.veggo.app.core.utils;

public final class Constants {
    // Emulator default.
    // public static final String API_BASE_URL = "http://10.0.2.2:5001/api/";
    
    // Physical device alternatives (Uncomment the one matching your computer's IP if testing on a real phone):
    // public static final String API_BASE_URL = "http://10.43.182.30:5001/api/";
    public static final String API_BASE_URL = "http://10.0.1.184:5001/api/"; // <-- Your current Wi-Fi IP
    // public static final String API_BASE_URL = "http://172.20.10.2:5001/api/";
    // public static final String API_BASE_URL = "http://10.0.44.124:5001/api/";
    // public static final String API_BASE_URL = "http://10.0.23.230:5001/api/";

    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CARTS = "carts";
    public static final String COLLECTION_ORDERS = "orders";

    private Constants() {
    }
}
