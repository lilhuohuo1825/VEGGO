package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_recipes")
public class CommunityRecipeEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String categoryId;
    private String chefId;
    private int timeMinutes;
    private int ingredientCount;
    private String imageUrl;

    public CommunityRecipeEntity(@NonNull String id, String title, String categoryId, String chefId, int timeMinutes, int ingredientCount, String imageUrl) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.chefId = chefId;
        this.timeMinutes = timeMinutes;
        this.ingredientCount = ingredientCount;
        this.imageUrl = imageUrl;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getChefId() { return chefId; }
    public void setChefId(String chefId) { this.chefId = chefId; }
    public int getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }
    public int getIngredientCount() { return ingredientCount; }
    public void setIngredientCount(int ingredientCount) { this.ingredientCount = ingredientCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
