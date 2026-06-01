package com.veggo.app.domain.model;

public class Product {
    private final String id;
    private final String name;
    private final long price;
    private final String imageUrl;

    public Product(String id, String name, long price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}
