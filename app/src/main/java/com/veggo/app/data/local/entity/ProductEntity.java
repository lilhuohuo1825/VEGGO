package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private long price;
    private long originalPrice;
    private String sku;
    private String imageUrl;
    private String weightOptionsJson;
    private String weight;
    private float rating;
    private int reviewCount;
    private int soldCount;
    private String description;
    private String ingredients;
    private String usage;
    private String storage;
    private String producer;
    private String responsibleOrg;
    private String safetyWarning;
    private String manufactureDate;
    private String expiryDate;
    private String origin;
    private String condition;
    private String fatContent;
    private String categoryId;
    private String subcategoryId;
    private String brand;
    private double carbonSavingPoint;

    @Ignore
    public ProductEntity(@NonNull String id, String name, long price, long originalPrice,
                         String sku, String imageUrl, String weight, float rating, int reviewCount, 
                         int soldCount, String description, String origin, 
                         String condition, String fatContent, String categoryId, String subcategoryId) {
        this(id, name, price, originalPrice, sku, imageUrl, null, weight, rating, reviewCount, soldCount,
                description, null, null, null, null, null, null, null, null,
                origin, condition, fatContent, categoryId, subcategoryId, null, 0.0);
    }

    @Ignore
    public ProductEntity(@NonNull String id, String name, long price, long originalPrice,
                         String sku, String imageUrl, String weightOptionsJson, String weight, float rating, int reviewCount,
                         int soldCount, String description, String origin,
                         String condition, String fatContent, String categoryId, String subcategoryId, double carbonSavingPoint) {
        this(id, name, price, originalPrice, sku, imageUrl, weightOptionsJson, weight, rating, reviewCount, soldCount,
                description, null, null, null, null, null, null, null, null,
                origin, condition, fatContent, categoryId, subcategoryId, null, carbonSavingPoint);
    }

    public ProductEntity(@NonNull String id, String name, long price, long originalPrice,
                         String sku, String imageUrl, String weightOptionsJson, String weight, float rating, int reviewCount,
                         int soldCount, String description, String ingredients, String usage, String storage,
                         String producer, String responsibleOrg, String safetyWarning,
                         String manufactureDate, String expiryDate, String origin,
                         String condition, String fatContent, String categoryId, String subcategoryId,
                         String brand, double carbonSavingPoint) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.sku = sku;
        this.imageUrl = imageUrl;
        this.weightOptionsJson = weightOptionsJson;
        this.weight = weight;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        this.description = description;
        this.ingredients = ingredients;
        this.usage = usage;
        this.storage = storage;
        this.producer = producer;
        this.responsibleOrg = responsibleOrg;
        this.safetyWarning = safetyWarning;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.origin = origin;
        this.condition = condition;
        this.fatContent = fatContent;
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
        this.brand = brand;
        this.carbonSavingPoint = carbonSavingPoint;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getWeightOptionsJson() { return weightOptionsJson; }
    public void setWeightOptionsJson(String weightOptionsJson) { this.weightOptionsJson = weightOptionsJson; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
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
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public double getCarbonSavingPoint() { return carbonSavingPoint; }
    public void setCarbonSavingPoint(double carbonSavingPoint) { this.carbonSavingPoint = carbonSavingPoint; }
}
