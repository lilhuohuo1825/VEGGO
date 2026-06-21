package com.veggo.app.domain.model;

public class Consultation {
    private final String id;
    private final String question;
    private final String customerName;
    private final String customerId;
    private final String answer;
    private final String status;
    private final String createdAt;
    private final String answeredAt;
    private final String answeredBy;

    public Consultation(String id, String question, String customerName, String customerId,
                        String answer, String status, String createdAt, String answeredAt,
                        String answeredBy) {
        this.id = id;
        this.question = question;
        this.customerName = customerName;
        this.customerId = customerId;
        this.answer = answer;
        this.status = status;
        this.createdAt = createdAt;
        this.answeredAt = answeredAt;
        this.answeredBy = answeredBy;
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public String getCustomerName() { return customerName; }
    public String getCustomerId() { return customerId; }
    public String getAnswer() { return answer; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getAnsweredAt() { return answeredAt; }
    public String getAnsweredBy() { return answeredBy; }
}
