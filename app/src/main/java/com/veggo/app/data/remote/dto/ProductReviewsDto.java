package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProductReviewsDto {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("sku")
    private String sku;
    
    @SerializedName("reviews")
    private List<ReviewDto> reviews;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public List<ReviewDto> getReviews() { return reviews; }
    public void setReviews(List<ReviewDto> reviews) { this.reviews = reviews; }
}
