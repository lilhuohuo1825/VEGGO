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
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public List<QuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDto> questions) { this.questions = questions; }

    public static class QuestionDto {
        @SerializedName("_id")
        private String id;
        
        @SerializedName("question")
        private String question;
        
        @SerializedName("customerId")
        private String customerId;
        
        @SerializedName("customerName")
        private String customerName;
        
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

        public String getId() { return id; }
        public String getQuestion() { return question; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getAnswer() { return answer; }
        public String getAnsweredBy() { return answeredBy; }
        public String getAnsweredAt() { return answeredAt; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
    }
}
