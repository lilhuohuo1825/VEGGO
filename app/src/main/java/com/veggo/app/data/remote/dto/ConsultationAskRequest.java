package com.veggo.app.data.remote.dto;

public class ConsultationAskRequest {
    private final String question;
    private final String customerId;
    private final String customerName;
    private final String productName;

    public ConsultationAskRequest(String question, String customerId, String customerName, String productName) {
        this.question = question;
        this.customerId = customerId;
        this.customerName = customerName;
        this.productName = productName;
    }

    public String getQuestion() {
        return question;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }
}
