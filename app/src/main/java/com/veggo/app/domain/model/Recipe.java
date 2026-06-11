package com.veggo.app.domain.model;

public class Recipe {
    private final String id;
    private final String name;
    private final String time;
    private final int imageRes;
    private final String imageUrl;

    public Recipe(String id, String name, String time, int imageRes, String imageUrl) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTime() { return time; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
}
