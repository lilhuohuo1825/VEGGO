package com.veggo.app.domain.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Product {
    @SerializedName("_id")
    private String id;

    @SerializedName("product_name")
    private String productName;

    private String brand;
    private double price;

    @SerializedName("base_price")
    private double basePrice;

    private List<String> image;
    private float rating;

    @SerializedName("purchase_count")
    private int purchaseCount;

    public Product() {
    }

    public Product(String id, String productName, double price, String imageUrl) {
        this.id = id;
        this.productName = productName;
        this.price = price;
        if (imageUrl != null) {
            this.image = java.util.Collections.singletonList(imageUrl);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getName() { return productName; }
    public void setName(String name) { this.productName = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public List<String> getImage() { return image; }
    public void setImage(List<String> image) { this.image = image; }

    public String getImageUrl() {
        return (image != null && !image.isEmpty()) ? image.get(0) : null;
    }
    public void setImageUrl(String imageUrl) {
        if (imageUrl != null) {
            this.image = java.util.Collections.singletonList(imageUrl);
        } else {
            this.image = null;
        }
    }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }
}