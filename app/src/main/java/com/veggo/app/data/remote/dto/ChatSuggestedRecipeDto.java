package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ChatSuggestedRecipeDto {
    @SerializedName(value = "instructionId", alternate = {"_id", "id"})
    private String instructionId;

    private String title;
    private String image;
    private String description;

    @SerializedName("cookingTime")
    private String cookingTime;

    @SerializedName("matchScore")
    private double matchScore;

    public String getInstructionId() {
        return instructionId;
    }

    public String resolveInstructionId() {
        if (instructionId != null && !instructionId.trim().isEmpty()) {
            return instructionId.trim();
        }
        return "";
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
