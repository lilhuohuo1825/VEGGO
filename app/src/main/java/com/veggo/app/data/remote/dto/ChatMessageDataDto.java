package com.veggo.app.data.remote.dto;

import java.util.List;
import java.util.Map;

public class ChatMessageDataDto {
    private String conversationId;
    private String reply;
    private String intent;
    private double confidence;
    private List<String> servicesUsed;
    private List<ChatSuggestedProductDto> suggestedProducts;
    private List<ChatSuggestedRecipeDto> suggestedRecipes;
    private Map<String, Object> nutritionSummary;

    public String getConversationId() {
        return conversationId;
    }

    public String getReply() {
        return reply;
    }

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public List<String> getServicesUsed() {
        return servicesUsed;
    }

    public List<ChatSuggestedProductDto> getSuggestedProducts() {
        return suggestedProducts;
    }

    public List<ChatSuggestedRecipeDto> getSuggestedRecipes() {
        return suggestedRecipes;
    }

    public Map<String, Object> getNutritionSummary() {
        return nutritionSummary;
    }
}
