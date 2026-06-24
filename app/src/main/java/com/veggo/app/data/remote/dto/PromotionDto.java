package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class PromotionDto implements Serializable {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("promotion_id")
    private String promotionId;
    
    private String code;
    private String name;
    private String description;
    private String type;
    private String scope;
    
    @SerializedName("discount_type")
    private String discountType;
    
    @SerializedName("discount_value")
    private int discountValue;
    
    @SerializedName("max_discount_value")
    private int maxDiscountValue;
    
    @SerializedName("min_order_value")
    private int minOrderValue;
    
    @SerializedName("usage_limit")
    private int usageLimit;
    
    @SerializedName("user_limit")
    private int userLimit;
    
    @SerializedName("is_first_order_only")
    private boolean firstOrderOnly;
    
    @SerializedName("start_date")
    private String startDate;
    
    @SerializedName("end_date")
    private String endDate;
    
    private String status;
    
    @SerializedName("targets")
    private List<TargetDto> targets;
    
    @SerializedName("usage")
    private UsageDto usage;

    // Getters and Setters
    public String getId() { return id; }
    public String getPromotionId() { return promotionId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getScope() { return scope; }
    public String getDiscountType() { return discountType; }
    public int getDiscountValue() { return discountValue; }
    public int getMaxDiscountValue() { return maxDiscountValue; }
    public int getMinOrderValue() { return minOrderValue; }
    public int getUsageLimit() { return usageLimit; }
    public int getUserLimit() { return userLimit; }
    public boolean isFirstOrderOnly() { return firstOrderOnly; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public List<TargetDto> getTargets() { return targets; }
    public UsageDto getUsage() { return usage; }

    public static class TargetDto implements Serializable {
        @SerializedName("target_type")
        private String targetType;
        @SerializedName("target_ref")
        private List<String> targetRef;

        public String getTargetType() { return targetType; }
        public List<String> getTargetRef() { return targetRef; }
    }

    public static class UsageDto implements Serializable {
        @SerializedName("user_id")
        private List<String> userIds;
        @SerializedName("order_id")
        private List<String> orderIds;

        public List<String> getUserIds() { return userIds; }
        public List<String> getOrderIds() { return orderIds; }
    }
}
