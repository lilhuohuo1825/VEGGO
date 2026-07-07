package com.veggo.app.data.remote.dto;

import java.util.List;

public class WalletTransactionsResponseDto {
    private boolean success;
    private List<WalletTransactionDto> data;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public List<WalletTransactionDto> getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
