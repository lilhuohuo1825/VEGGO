package com.veggo.app.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Consultation {
    private final String id;
    private final String question;
    private final String customerName;
    private final String customerId;
    private final String customerAvatarUrl;
    private final String answer;
    private final String status;
    private final String createdAt;
    private final String answeredAt;
    private final String answeredBy;
    private final int helpfulCount;
    private final List<String> helpfulLikeCustomerIds;
    private final List<ConsultationReply> replies;

    public Consultation(String id, String question, String customerName, String customerId,
                        String customerAvatarUrl, String answer, String status, String createdAt,
                        String answeredAt, String answeredBy, int helpfulCount,
                        List<String> helpfulLikeCustomerIds, List<ConsultationReply> replies) {
        this.id = id;
        this.question = question;
        this.customerName = customerName;
        this.customerId = customerId;
        this.customerAvatarUrl = customerAvatarUrl;
        this.answer = answer;
        this.status = status;
        this.createdAt = createdAt;
        this.answeredAt = answeredAt;
        this.answeredBy = answeredBy;
        this.helpfulCount = helpfulCount;
        this.helpfulLikeCustomerIds = helpfulLikeCustomerIds != null
                ? new ArrayList<>(helpfulLikeCustomerIds)
                : new ArrayList<>();
        this.replies = replies != null ? new ArrayList<>(replies) : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public String getCustomerName() { return customerName; }
    public String getCustomerId() { return customerId; }
    public String getCustomerAvatarUrl() { return customerAvatarUrl; }
    public String getAnswer() { return answer; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getAnsweredAt() { return answeredAt; }
    public String getAnsweredBy() { return answeredBy; }
    public int getHelpfulCount() { return helpfulCount; }
    public List<String> getHelpfulLikeCustomerIds() {
        return Collections.unmodifiableList(helpfulLikeCustomerIds);
    }
    public List<ConsultationReply> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public boolean isLikedBy(String customerId) {
        if (customerId == null || customerId.isEmpty()) {
            return false;
        }
        for (String likedBy : helpfulLikeCustomerIds) {
            if (customerId.equals(likedBy)) {
                return true;
            }
        }
        return false;
    }
}
