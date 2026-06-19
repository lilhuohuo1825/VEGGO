package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_categories")
public class CommunityCategoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private int recipeCount;
    private String imageUrl;
    @Ignore
    private String iconEmoji;

    public CommunityCategoryEntity(@NonNull String id, String name, int recipeCount, String imageUrl) {
        this.id = id;
        this.name = name;
        this.recipeCount = recipeCount;
        this.imageUrl = imageUrl;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRecipeCount() { return recipeCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
}
