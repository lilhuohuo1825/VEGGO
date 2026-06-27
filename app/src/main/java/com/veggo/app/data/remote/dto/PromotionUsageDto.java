package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PromotionUsageDto {
    @SerializedName("promotion_id")
    private String promotionId;

    @SerializedName("user_id")
    private List<String> userIds;

    @SerializedName("order_id")
    private List<String> orderIds;

    public String getPromotionId() {
        return promotionId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public List<String> getOrderIds() {
        return orderIds;
    }
}
