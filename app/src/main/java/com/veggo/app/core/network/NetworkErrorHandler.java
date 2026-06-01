package com.veggo.app.core.network;

public final class NetworkErrorHandler {
    private NetworkErrorHandler() {
    }

    public static String getMessage(Throwable throwable) {
        return throwable != null && throwable.getMessage() != null
                ? throwable.getMessage()
                : "Unknown network error";
    }
}
