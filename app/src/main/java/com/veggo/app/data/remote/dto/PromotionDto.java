package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class PromotionDto {
    @SerializedName("_id")
    private Object id;

    @SerializedName("promotion_id")
    private String promotionId;

    private String name;

    @SerializedName("banner_data")
    private BannerDataDto bannerData;

    @SerializedName("bannerData")
    private BannerDataDto bannerDataCamel;

    private String imageUrl;

    @SerializedName("show_on_app")
    private Boolean showOnApp;

    @SerializedName("display_section")
    private String displaySection;

    private Boolean isActive;

    @SerializedName("status")
    private String status;

    private String code;
    private String description;
    
    @SerializedName("discount_value")
    private Double discountValue;
    
    @SerializedName("max_discount_value")
    private Double maxDiscountValue;
    
    @SerializedName("min_order_value")
    private Double minOrderValue;
    
    @SerializedName("usage_limit")
    private Integer usageLimit;
    
    @SerializedName("user_limit")
    private Integer userLimit;
    
    @SerializedName("is_first_order_only")
    private Boolean isFirstOrderOnly;
    
    @SerializedName("start_date")
    private String startDate;
    
    @SerializedName("end_date")
    private String endDate;
    
    private String scope;

    public String getId() {
        if (id instanceof String) {
            return (String) id;
        }
        if (id instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) id;
            if (map.containsKey("$oid")) {
                return String.valueOf(map.get("$oid"));
            }
        }
        return id != null ? id.toString() : null;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public String getName() {
        return name;
    }

    public BannerDataDto getBannerData() {
        return bannerData != null ? bannerData : bannerDataCamel;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Boolean getShowOnApp() {
        return showOnApp != null ? showOnApp : true;
    }

    public Boolean getActive() {
        if (isActive != null) return isActive;
        if (status != null) return "Active".equalsIgnoreCase(status);
        return true;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public Double getDiscountValue() { return discountValue; }
    public Double getMaxDiscountValue() { return maxDiscountValue; }
    public Double getMinOrderValue() { return minOrderValue; }
    public Integer getUsageLimit() { return usageLimit; }
    public Integer getUserLimit() { return userLimit; }
    public Boolean getIsFirstOrderOnly() { return isFirstOrderOnly; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getScope() { return scope; }
    public String getStatus() { return status; }

    public static class BannerDataDto {
        private String imageUrl;
        private String src;
        private Boolean showOnApp;

        public String getImageUrl() {
            return imageUrl;
        }

        public String getSrc() {
            return src;
        }

        public Boolean getShowOnApp() {
            return showOnApp;
        }
    }
}
