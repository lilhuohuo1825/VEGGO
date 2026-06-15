package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_recipe_ingredients")
public class CommunityRecipeIngredientEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String recipeId;
    private String productId;
    private String productSku;
    private String displayName;
    private String iconUrl;
    private String iconEmoji;
    private String quantity;
    private int sortOrder;

    public CommunityRecipeIngredientEntity(@NonNull String id, String recipeId, String productId, String productSku, String displayName, String iconUrl, String iconEmoji, String quantity, int sortOrder) {
        this.id = id;
        this.recipeId = recipeId;
        this.productId = productId;
        this.productSku = productSku;
        this.displayName = displayName;
        this.iconUrl = iconUrl;
        this.iconEmoji = iconEmoji;
        this.quantity = quantity;
        this.sortOrder = sortOrder;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
