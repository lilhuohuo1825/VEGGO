package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProductDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("product_name")
    private String productName;

    private String sku;
    private long price;
    
    @SerializedName("base_price")
    private long originalPrice;

    private String unit;

    @SerializedName("image")
    private List<String> imageList;

    @SerializedName("WeightOptions")
    private List<Double> weightOptions;

    @SerializedName("CarbonSavingPoint")
    private double carbonSavingPoint;

    private String description;
    private int stock;
    private Boolean isActive;
    
    // Additional fields for mapping
    private float rating;
    private int reviewCount;
    private int soldCount;
    private String origin;
    private String condition;
    private String fatContent;
    private String categoryId;
    private String subcategoryId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return productName; }
    public String getName() { return productName; } // Alias for Mapper
    public void setProductName(String productName) { this.productName = productName; }
    public void setName(String name) { this.productName = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    
    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    public String getUnit() { return unit; }
    public String getWeight() { return unit; } // Alias
    public void setUnit(String unit) { this.unit = unit; }

    public List<String> getImageList() { return imageList; }
    public void setImageList(List<String> imageList) { this.imageList = imageList; }
    public void setImageUrl(String imageUrl) {
        if (imageUrl != null) {
            this.imageList = java.util.Collections.singletonList(imageUrl);
        }
    }
    public String getImageUrl() { return getFirstImage(); }

    public List<Double> getWeightOptions() { return weightOptions; }
    public void setWeightOptions(List<Double> weightOptions) { this.weightOptions = weightOptions; }

    public double getCarbonSavingPoint() { return carbonSavingPoint; }
    public void setCarbonSavingPoint(double carbonSavingPoint) { this.carbonSavingPoint = carbonSavingPoint; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getFatContent() { return fatContent; }
    public void setFatContent(String fatContent) { this.fatContent = fatContent; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }

    public String getFirstImage() {
        if (imageList != null && !imageList.isEmpty()) {
            return imageList.get(0);
        }
        return "";
    }
}
