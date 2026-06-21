package com.veggo.app.data.remote.dto;

public class ConsultationAskRequest {
    private final String question;
    private final String customerName;
    private final String productName;

    public ConsultationAskRequest(String question, String customerName, String productName) {
        this.question = question;
        this.customerName = customerName;
        this.productName = productName;
    }

    public String getQuestion() {
        return question;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }
}
