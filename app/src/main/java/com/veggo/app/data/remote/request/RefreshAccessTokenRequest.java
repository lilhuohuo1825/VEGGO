package com.veggo.app.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class RefreshAccessTokenRequest {
    @SerializedName("customerId")
    private final String customerId;

    @SerializedName("phone")
    private final String phone;

    public RefreshAccessTokenRequest(String customerId, String phone) {
        this.customerId = customerId;
        this.phone = phone;
    }
}
