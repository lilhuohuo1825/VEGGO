package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_recipe_galleries")
public class CommunityRecipeGalleryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String recipeId;
    private String imageUrl;
    private int sortOrder;

    public CommunityRecipeGalleryEntity(@NonNull String id, String recipeId, String imageUrl, int sortOrder) {
        this.id = id;
        this.recipeId = recipeId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
