package com.veggo.app.data.remote.dto;

public class ConsultationLikeRequest {
    private final String customerId;
    private final String customerName;

    public ConsultationLikeRequest(String customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
    }

    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
}
