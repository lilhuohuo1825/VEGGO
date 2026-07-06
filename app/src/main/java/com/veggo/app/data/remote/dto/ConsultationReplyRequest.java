package com.veggo.app.data.remote.dto;

public class ConsultationReplyRequest {
    private final String content;
    private final String customerId;
    private final String customerName;
    private final String customerAvatarUrl;

    public ConsultationReplyRequest(String content, String customerId, String customerName,
                                    String customerAvatarUrl) {
        this.content = content;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerAvatarUrl = customerAvatarUrl;
    }

    public String getContent() { return content; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerAvatarUrl() { return customerAvatarUrl; }
}
