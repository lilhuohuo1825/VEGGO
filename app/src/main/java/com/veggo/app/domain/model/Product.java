package com.veggo.app.domain.model;

public class Product {
    private final String id;
    private final String name;
    private final String sku;
    private final long price;
    private final long originalPrice;
    private final String imageUrl;
    private final String weight;
    private final float rating;
    private final int reviewCount;
    private final int soldCount;
    private final String description;
    private final String origin;
    private final String condition;
    private final String fatContent;

    public Product(String id, String name, long price, String imageUrl) {
        this(id, name, null, price, 0, imageUrl, null, 0, 0, 0, null, null, null, null);
    }

    public Product(String id, String name, String sku, long price, long originalPrice, String imageUrl, 
                   String weight, float rating, int reviewCount, int soldCount, 
                   String description, String origin, String condition, String fatContent) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
        this.weight = weight;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        this.description = description;
        this.origin = origin;
        this.condition = condition;
        this.fatContent = fatContent;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public long getPrice() { return price; }
    public long getOriginalPrice() { return originalPrice; }
    public String getImageUrl() { return imageUrl; }
    public String getWeight() { return weight; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public int getSoldCount() { return soldCount; }
    public String getDescription() { return description; }
    public String getOrigin() { return origin; }
    public String getCondition() { return condition; }
    public String getFatContent() { return fatContent; }
}
