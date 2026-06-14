package com.veggo.app.domain.model;

public class Recipe {
    private final String id;
    private final String name;
    private final String imageUrl;
    private final String cookingTime;
    private final String price;
    private final float rating;
    private final int reviewCount;
    private boolean isBookmarked;

    public Recipe(String id, String name, String imageUrl, String cookingTime, String price, float rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.cookingTime = cookingTime;
        this.price = price;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.isBookmarked = false;
    }

    public Recipe(String id, String name, String cookingTime, int imageResId, String imageUrl) {
        this(id, name, imageUrl, cookingTime, "", 0f, 0);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getCookingTime() { return cookingTime; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}
