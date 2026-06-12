package com.veggo.app.domain.model;

public class FlashSale {
    private final String id;
    private final String name;
    private final long price;
    private final String unit;
    private final String discount;
    private final int imageRes;
    private final String imageUrl;
    private final float rating;

    public FlashSale(String id, String name, long price, String unit, String discount, int imageRes, String imageUrl, float rating) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.discount = discount;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
        this.rating = rating;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getPrice() { return price; }
    public String getUnit() { return unit; }
    public String getDiscount() { return discount; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
    public float getRating() { return rating; }
}
