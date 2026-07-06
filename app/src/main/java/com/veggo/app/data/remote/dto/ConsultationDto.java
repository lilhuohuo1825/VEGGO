package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ConsultationDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("sku")
    private String sku;

    @SerializedName("productName")
    private String productName;

    @SerializedName("questions")
    private List<QuestionDto> questions;

    public String getId() { return id; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public List<QuestionDto> getQuestions() { return questions; }

    public static class HelpfulLikeDto {
        @SerializedName("customerId")
        private String customerId;

        @SerializedName("customerName")
        private String customerName;

        public String getCustomerId() { return customerId; }
    }

    public static class ReplyDto {
        @SerializedName("_id")
        private String id;

        @SerializedName("customerId")
        private String customerId;

        @SerializedName("customerName")
        private String customerName;

        @SerializedName("customerAvatarUrl")
        private String customerAvatarUrl;

        @SerializedName("content")
        private String content;

        @SerializedName("isAdmin")
        private boolean admin;

        @SerializedName("createdAt")
        private String createdAt;

        public String getId() { return id; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerAvatarUrl() { return customerAvatarUrl; }
        public String getContent() { return content; }
        public boolean isAdmin() { return admin; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class QuestionDto {
        @SerializedName("_id")
        private String id;

        @SerializedName("question")
        private String question;

        @SerializedName("customerId")
        private String customerId;

        @SerializedName("customerName")
        private String customerName;

        @SerializedName("customerAvatarUrl")
        private String customerAvatarUrl;

        @SerializedName("answer")
        private String answer;

        @SerializedName("answeredBy")
        private String answeredBy;

        @SerializedName("answeredAt")
        private String answeredAt;

        @SerializedName("status")
        private String status;

        @SerializedName("createdAt")
        private String createdAt;

        @SerializedName("helpfulCount")
        private Integer helpfulCount;

        @SerializedName("helpfulLikes")
        private List<HelpfulLikeDto> helpfulLikes;

        @SerializedName("replies")
        private List<ReplyDto> replies;

        public String getId() { return id; }
        public String getQuestion() { return question; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerAvatarUrl() { return customerAvatarUrl; }
        public String getAnswer() { return answer; }
        public String getAnsweredBy() { return answeredBy; }
        public String getAnsweredAt() { return answeredAt; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public Integer getHelpfulCount() { return helpfulCount; }
        public List<HelpfulLikeDto> getHelpfulLikes() { return helpfulLikes; }
        public List<ReplyDto> getReplies() { return replies; }
    }
}
