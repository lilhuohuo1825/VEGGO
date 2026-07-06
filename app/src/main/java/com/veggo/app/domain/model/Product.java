package com.veggo.app.domain.model;

import com.google.gson.annotations.SerializedName;
import com.veggo.app.core.utils.ProductImageUtils;
import java.util.List;

public class Product {
    @SerializedName(value = "_id", alternate = {"id"})
    private final String id;

    @SerializedName(value = "product_name", alternate = {"name"})
    private final String name;

    private final String sku;
    private final long price;

    @SerializedName("originalPrice")
    private final long originalPrice;

    @SerializedName(value = "imageUrl")
    private final String imageUrl;

    @SerializedName(value = "image")
    private final List<String> image;

    @SerializedName(value = "WeightOptions", alternate = {"weightOptions"})
    private final List<Double> weightOptions;

    private final String weight;
    private final float rating;
    private final int reviewCount;

    @SerializedName(value = "purchase_count", alternate = {"soldCount"})
    private final int soldCount;

    private final String description;
    private final String ingredients;
    private final String usage;
    private final String storage;
    private final String producer;
    private final String responsibleOrg;
    private final String safetyWarning;
    private final String manufactureDate;
    private final String expiryDate;
    private final String origin;
    private final String condition;
    private final String fatContent;
    private final String categoryId;
    private final String subcategoryId;
    private final String brand;
    private final double carbonSavingPoint;

    public Product(String id, String name, long price, String imageUrl) {
        this(id, name, null, price, 0, imageUrl, null, null, 0, 0, 0,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, 0.0);
    }

    public Product(String id, String name, String sku, long price, long originalPrice, String imageUrl, 
                   String weight, float rating, int reviewCount, int soldCount,
                   String description, String origin, String condition, String fatContent) {
        this(id, name, sku, price, originalPrice, imageUrl, null, weight, rating, reviewCount, soldCount,
                description, null, null, null, null, null, null, null, null,
                origin, condition, fatContent, null, null, null, 0.0);
    }

    public Product(String id, String name, String sku, long price, long originalPrice, String imageUrl, 
                   List<Double> weightOptions,
                   String weight, float rating, int reviewCount, int soldCount,
                   String description, String origin, String condition, String fatContent,
                   String categoryId, String subcategoryId) {
        this(id, name, sku, price, originalPrice, imageUrl, weightOptions, weight, rating, reviewCount, soldCount,
                description, null, null, null, null, null, null, null, null,
                origin, condition, fatContent, categoryId, subcategoryId, null, 0.0);
    }

    public Product(String id, String name, String sku, long price, long originalPrice, String imageUrl,
                   List<Double> weightOptions,
                   String weight, float rating, int reviewCount, int soldCount,
                   String description, String origin, String condition, String fatContent,
                   String categoryId, String subcategoryId, double carbonSavingPoint) {
        this(id, name, sku, price, originalPrice, imageUrl, weightOptions, weight, rating, reviewCount, soldCount,
                description, null, null, null, null, null, null, null, null,
                origin, condition, fatContent, categoryId, subcategoryId, null, carbonSavingPoint);
    }

    public Product(String id, String name, String sku, long price, long originalPrice, String imageUrl,
                   List<Double> weightOptions,
                   String weight, float rating, int reviewCount, int soldCount,
                   String description, String ingredients, String usage, String storage,
                   String producer, String responsibleOrg, String safetyWarning,
                   String manufactureDate, String expiryDate,
                   String origin, String condition, String fatContent,
                   String categoryId, String subcategoryId, String brand, double carbonSavingPoint) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
        this.image = null;
        this.weightOptions = weightOptions;
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

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public long getPrice() { return price; }
    public long getOriginalPrice() { return originalPrice; }

    public boolean hasActiveDiscount() {
        return originalPrice > price && originalPrice > 0;
    }

    public Product withPricing(long price, long originalPrice) {
        return new Product(
                id, name, sku, price, originalPrice, imageUrl, weightOptions, weight, rating, reviewCount, soldCount,
                description, ingredients, usage, storage, producer, responsibleOrg, safetyWarning,
                manufactureDate, expiryDate, origin, condition, fatContent, categoryId, subcategoryId, brand, carbonSavingPoint
        );
    }

    public String getImageUrl() {
        String resolved = ProductImageUtils.normalizeUrl(imageUrl);
        if (resolved != null) {
            return resolved;
        }
        if (image != null && !image.isEmpty()) {
            return ProductImageUtils.normalizeUrl(image.get(0));
        }
        return null;
    }

    public List<Double> getWeightOptions() { return weightOptions; }
    public String getWeight() { return weight; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public int getSoldCount() { return soldCount; }
    public String getDescription() { return description; }
    public String getIngredients() { return ingredients; }
    public String getUsage() { return usage; }
    public String getStorage() { return storage; }
    public String getProducer() { return producer; }
    public String getResponsibleOrg() { return responsibleOrg; }
    public String getSafetyWarning() { return safetyWarning; }
    public String getManufactureDate() { return manufactureDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getOrigin() { return origin; }
    public String getCondition() { return condition; }
    public String getFatContent() { return fatContent; }
    public String getCategoryId() { return categoryId; }
    public String getSubcategoryId() { return subcategoryId; }
    public String getBrand() { return brand; }
    public double getCarbonSavingPoint() { return carbonSavingPoint; }
}
