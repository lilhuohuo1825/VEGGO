package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RelatedRecipeDto {
    @SerializedName("instructionId")
    private String instructionId;

    @SerializedName("title")
    private String title;

    @SerializedName("image")
    private String image;

    @SerializedName("description")
    private String description;

    @SerializedName("cookingTime")
    private String cookingTime;

    @SerializedName("matchScore")
    private double matchScore;

    public String getInstructionId() {
        return instructionId;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public String getCookingTime() {
        return cookingTime;
    }

    public double getMatchScore() {
        return matchScore;
    }
}
