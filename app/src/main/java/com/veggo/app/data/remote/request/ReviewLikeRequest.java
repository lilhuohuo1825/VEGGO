package com.veggo.app.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class ReviewLikeRequest {
    @SerializedName("customer_id")
    private final String customerId;

    public ReviewLikeRequest(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
