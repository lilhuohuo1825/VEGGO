package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PromotionTargetDto {
    @SerializedName("promotion_id")
    private String promotionId;

    @SerializedName("target_type")
    private String targetType;

    @SerializedName("target_ref")
    private List<String> targetRefs;

    @SerializedName(value = "target_groups", alternate = {"targetGroups"})
    private List<TargetGroupDto> targetGroups;

    public String getPromotionId() {
        return promotionId;
    }

    public String getTargetType() {
        return targetType;
    }

    public List<String> getTargetRefs() {
        return targetRefs;
    }

    public List<TargetGroupDto> getTargetGroups() {
        return targetGroups;
    }

    public static class TargetGroupDto {
        @SerializedName("target_type")
        private String targetType;

        @SerializedName("target_ref")
        private List<String> targetRefs;

        public String getTargetType() {
            return targetType;
        }

        public List<String> getTargetRefs() {
            return targetRefs;
        }
    }
}
