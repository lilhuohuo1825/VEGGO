package com.veggo.app.domain.model;

public class Banner {
    private final String id;
    private final int imageRes;
    private final String imageUrl;

    public Banner(String id, int imageRes, String imageUrl) {
        this.id = id;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
}
