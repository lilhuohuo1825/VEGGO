package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SupportConversationDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("customerId")
    private String customerId;

    @SerializedName("status")
    private String status;

    @SerializedName("lastMessageAt")
    private String lastMessageAt;

    @SerializedName("unreadCountUser")
    private int unreadCountUser;

    @SerializedName("unreadCountAdmin")
    private int unreadCountAdmin;

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getStatus() { return status; }
    public String getLastMessageAt() { return lastMessageAt; }
    public int getUnreadCountUser() { return unreadCountUser; }
    public int getUnreadCountAdmin() { return unreadCountAdmin; }
}

