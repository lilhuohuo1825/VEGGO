package com.veggo.app.domain.model;

public class ConsultationReply {
    private final String id;
    private final String customerId;
    private final String customerName;
    private final String customerAvatarUrl;
    private final String content;
    private final boolean admin;
    private final String createdAt;

    public ConsultationReply(String id, String customerId, String customerName, String customerAvatarUrl,
                             String content, boolean admin, String createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerAvatarUrl = customerAvatarUrl;
        this.content = content;
        this.admin = admin;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerAvatarUrl() { return customerAvatarUrl; }
    public String getContent() { return content; }
    public boolean isAdmin() { return admin; }
    public String getCreatedAt() { return createdAt; }
}
