package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ChatMessageRequestDto {
    @SerializedName("customerId")
    private final String customerId;

    @SerializedName("message")
    private final String message;

    @SerializedName("conversationId")
    private final String conversationId;

    public ChatMessageRequestDto(String customerId, String message, String conversationId) {
        this.customerId = customerId;
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    public String getConversationId() {
        return conversationId;
    }
}
