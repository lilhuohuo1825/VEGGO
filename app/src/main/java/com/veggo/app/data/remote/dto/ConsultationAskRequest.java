package com.veggo.app.data.remote.dto;

public class ConsultationAskRequest {
    private final String question;
    private final String customerId;
    private final String customerName;
    private final String productName;
    private final String customerAvatarUrl;

    public ConsultationAskRequest(String question, String customerId, String customerName,
                                  String productName, String customerAvatarUrl) {
        this.question = question;
        this.customerId = customerId;
        this.customerName = customerName;
        this.productName = productName;
        this.customerAvatarUrl = customerAvatarUrl;
    }

    public String getQuestion() { return question; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getProductName() { return productName; }
    public String getCustomerAvatarUrl() { return customerAvatarUrl; }
}
