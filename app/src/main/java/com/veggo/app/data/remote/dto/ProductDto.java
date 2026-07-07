package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.veggo.app.core.utils.ProductImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO ánh xạ từ response product MongoDB. Hỗ trợ cả field cũ và mới để các
 * luồng Product, Checkout và Home dùng chung mapper.
 */
public class ProductDto {
    @SerializedName(value = "_id", alternate = {"id"})
    private String id;

    @SerializedName(value = "product_name", alternate = {"name"})
    private String name;

    private String brand;
    private String sku;
    private String unit;
    private String weight;
    private String origin;
    private String status;
    private long price;

    @SerializedName("originalPrice")
    private long originalPrice;

    @SerializedName(value = "image", alternate = {"imageList"})
    private Object image;

    @SerializedName(value = "imageUrl")
    private Object imageUrl;

    @SerializedName(value = "WeightOptions", alternate = {"weightOptions"})
    private List<Double> weightOptions;

    @SerializedName(value = "CarbonSavingPoint", alternate = {"carbonSavingPoint"})
    private double carbonSavingPoint;

    @SerializedName(value = "EmissionFactor", alternate = {"emissionFactor"})
    private double emissionFactor;

    private String description;
    private String ingredients;
    private String usage;
    private String storage;
    private String producer;

    @SerializedName("responsible_org")
    private String responsibleOrg;

    @SerializedName("safety_warning")
    private String safetyWarning;

    @SerializedName("manufacture_date")
    private String manufactureDate;

    @SerializedName("expiry_date")
    private String expiryDate;

    private int stock;

    @SerializedName(value = "isActive", alternate = {"active"})
    private Boolean active;

    private float rating;
    private int reviewCount;

    @SerializedName(value = "purchase_count", alternate = {"soldCount"})
    private int soldCount;

    private int liked;
    private String condition;
    private String fatContent;

    @SerializedName(value = "CategoryID", alternate = {"categoryId"})
    private String categoryId;

    @SerializedName(value = "SubcategoryID", alternate = {"subcategoryId"})
    private String subcategoryId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return name; }
    public String getName() { return name; }
    public void setProductName(String productName) { this.name = productName; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getWeight() {
        return weight != null && !weight.trim().isEmpty() ? weight : unit;
    }
    public void setWeight(String weight) { this.weight = weight; }

    public List<String> getImageList() {
        return ProductImageUtils.asImageList(image, imageUrl);
    }

    public List<String> getImage() {
        return getImageList();
    }

    public Object getImageRaw() {
        return image;
    }

    public Object getImageUrlRaw() {
        return imageUrl;
    }

    public void setImageList(List<String> imageList) {
        this.image = imageList;
    }

    public void setImage(List<String> imageList) {
        this.image = imageList;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            this.image = imageUrl;
        }
    }

    public String getImageUrl() {
        return ProductImageUtils.resolveImageUrl(image, imageUrl);
    }

    public List<Double> getWeightOptions() { return weightOptions; }
    public void setWeightOptions(List<Double> weightOptions) { this.weightOptions = weightOptions; }

    public double getCarbonSavingPoint() { return carbonSavingPoint; }
    public void setCarbonSavingPoint(double carbonSavingPoint) { this.carbonSavingPoint = carbonSavingPoint; }

    public double getEmissionFactor() { return emissionFactor; }
    public void setEmissionFactor(double emissionFactor) { this.emissionFactor = emissionFactor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }

    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }

    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }

    public String getResponsibleOrg() { return responsibleOrg; }
    public void setResponsibleOrg(String responsibleOrg) { this.responsibleOrg = responsibleOrg; }

    public String getSafetyWarning() { return safetyWarning; }
    public void setSafetyWarning(String safetyWarning) { this.safetyWarning = safetyWarning; }

    public String getManufactureDate() { return manufactureDate; }
    public void setManufactureDate(String manufactureDate) { this.manufactureDate = manufactureDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Boolean getActive() {
        if (active != null) return active;
        return status == null ? null : "Active".equalsIgnoreCase(status);
    }
    public void setActive(Boolean active) { this.active = active; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public int getLiked() { return liked; }
    public void setLiked(int liked) { this.liked = liked; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getFatContent() { return fatContent; }
    public void setFatContent(String fatContent) { this.fatContent = fatContent; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }

    public String getFirstImage() {
        String firstImage = getImageUrl();
        return firstImage != null ? firstImage : "";
    }
}
