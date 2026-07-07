package com.veggo.app.data.remote.dto;

public class WalletResponseDto {
    private boolean success;
    private WalletDto data;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public WalletDto getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
