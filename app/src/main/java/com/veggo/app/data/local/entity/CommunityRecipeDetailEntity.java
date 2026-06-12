package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_recipe_details")
public class CommunityRecipeDetailEntity {
    @PrimaryKey
    @NonNull
    private String recipeId;
    private int calories;
    private String saltLevel;
    private String sugarLevel;
    private float rating;
    private String instructions;
    private String videoUrl;

    public CommunityRecipeDetailEntity(@NonNull String recipeId, int calories, String saltLevel, String sugarLevel, float rating, String instructions, String videoUrl) {
        this.recipeId = recipeId;
        this.calories = calories;
        this.saltLevel = saltLevel;
        this.sugarLevel = sugarLevel;
        this.rating = rating;
        this.instructions = instructions;
        this.videoUrl = videoUrl;
    }

    @NonNull public String getRecipeId() { return recipeId; }
    public void setRecipeId(@NonNull String recipeId) { this.recipeId = recipeId; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public String getSaltLevel() { return saltLevel; }
    public void setSaltLevel(String saltLevel) { this.saltLevel = saltLevel; }
    public String getSugarLevel() { return sugarLevel; }
    public void setSugarLevel(String sugarLevel) { this.sugarLevel = sugarLevel; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}
