package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "community_chefs")
public class CommunityChefEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private int recipeCount;
    private int likes;
    @SerializedName(value = "avatarUrl", alternate = {"imageUrl"})
    private String imageUrl;

    public CommunityChefEntity(@NonNull String id, String name, int recipeCount, int likes, String imageUrl) {
        this.id = id;
        this.name = name;
        this.recipeCount = recipeCount;
        this.likes = likes;
        this.imageUrl = imageUrl;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRecipeCount() { return recipeCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
