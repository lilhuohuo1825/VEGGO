package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "community_cookbook_recipes", primaryKeys = {"cookbookId", "recipeId"})
public class CommunityCookbookRecipeEntity {
    @NonNull
    private String cookbookId;
    @NonNull
    private String recipeId;
    private int sortOrder;

    public CommunityCookbookRecipeEntity(@NonNull String cookbookId, @NonNull String recipeId, int sortOrder) {
        this.cookbookId = cookbookId;
        this.recipeId = recipeId;
        this.sortOrder = sortOrder;
    }

    @NonNull public String getCookbookId() { return cookbookId; }
    public void setCookbookId(@NonNull String cookbookId) { this.cookbookId = cookbookId; }
    @NonNull public String getRecipeId() { return recipeId; }
    public void setRecipeId(@NonNull String recipeId) { this.recipeId = recipeId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
