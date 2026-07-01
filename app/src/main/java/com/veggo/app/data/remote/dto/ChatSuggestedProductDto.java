package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ChatSuggestedProductDto {
    @SerializedName(value = "id", alternate = {"_id", "productId"})
    private String id;
    private String sku;

    @SerializedName(value = "name", alternate = {"product_name"})
    private String name;

    private long price;
    private long originalPrice;
    private float rating;
    private int reviewCount;
    private String unit;
    private String weight;
    private String origin;
    private String image;

    public String getId() {
        return id;
    }

    public String getProductId() {
        if (id != null && !id.trim().isEmpty()) {
            return id;
        }
        return sku;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public long getOriginalPrice() {
        return originalPrice;
    }

    public float getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getUnit() {
        return unit;
    }

    public String getWeight() {
        return weight;
    }

    public String getOrigin() {
        return origin;
    }

    public String getImage() {
        return image;
    }
}
