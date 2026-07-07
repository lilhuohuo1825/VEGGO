package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SupportConversationsResponseDto {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<SupportConversationDto> data;

    public boolean isSuccess() { return success; }
    public List<SupportConversationDto> getData() { return data; }
}

