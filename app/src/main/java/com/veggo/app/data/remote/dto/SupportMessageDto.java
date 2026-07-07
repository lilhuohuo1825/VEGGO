package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SupportMessageDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("senderType")
    private String senderType;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("text")
    private String text;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getSenderType() { return senderType; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }
}

