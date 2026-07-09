package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class WalletTransactionDto {
    private String transactionId;
    private String customerId;
    private double amount;
    private String type; // 'deposit', 'refund', 'payment', 'cashback'
    private String status; // 'pending', 'completed', 'failed'
    private String referenceId;
    private String description;
    private Integer carbonPoints;
    private String createdAt;

    public String getTransactionId() {
        return transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCarbonPoints() {
        return carbonPoints;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
