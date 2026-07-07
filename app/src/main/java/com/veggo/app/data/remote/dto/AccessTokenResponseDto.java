package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AccessTokenResponseDto {
    @SerializedName("accessToken")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
