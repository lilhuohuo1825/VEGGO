package com.veggo.app.core.network;

public class ApiHttpException extends Exception {
    private final int code;

    public ApiHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
