package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewDto {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("fullname")
    private String fullName;
    
    @SerializedName("customer_id")
    private String customerId;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("rating")
    private float rating;
    
    @SerializedName("time")
    private String time;
    
    @SerializedName("images")
    private List<String> images;
    
    @SerializedName("order_id")
    private String orderId;

    @SerializedName("likes")
    private List<String> likes;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<String> getLikes() { return likes; }
    public void setLikes(List<String> likes) { this.likes = likes; }
}
