package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SupportMessagesResponseDto {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<SupportMessageDto> data;

    public boolean isSuccess() { return success; }
    public List<SupportMessageDto> getData() { return data; }
}

