package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecipeDetailDto {
    @SerializedName("instruction")
    private InstructionSummaryDto instruction;

    @SerializedName("dishes")
    private List<DishDetailDto> dishes;

    public InstructionSummaryDto getInstruction() {
        return instruction;
    }

    public List<DishDetailDto> getDishes() {
        return dishes;
    }

    public static class InstructionSummaryDto {
        @SerializedName("title")
        private String title;

        @SerializedName("image")
        private String image;

        @SerializedName("description")
        private String description;

        @SerializedName("cookingTime")
        private String cookingTime;

        @SerializedName("difficulty")
        private String difficulty;

        @SerializedName("servings")
        private String servings;

        @SerializedName("video")
        private String video;

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

        public String getDifficulty() {
            return difficulty;
        }

        public String getServings() {
            return servings;
        }

        public String getVideo() {
            return video;
        }
    }

    public static class DishDetailDto {
        @SerializedName("dishName")
        private String dishName;

        @SerializedName("description")
        private String description;

        @SerializedName("ingredients")
        private List<String> ingredients;

        @SerializedName("steps")
        private String steps;

        @SerializedName("usage")
        private String usage;

        public String getDishName() {
            return dishName;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getIngredients() {
            return ingredients;
        }

        public String getSteps() {
            return steps;
        }

        public String getUsage() {
            return usage;
        }
    }
}
