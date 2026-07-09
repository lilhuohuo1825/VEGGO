package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_cookbooks")
public class CommunityCookbookEntity {
    @PrimaryKey
    @NonNull
    private String id;
    @Ignore
    private String customerId;
    private String accountId;
    private String title;
    private int recipeCount;
    private String imageUrl;

    @Ignore
    public CommunityCookbookEntity() {
        this.id = "";
    }

    public CommunityCookbookEntity(@NonNull String id, String accountId, String title, int recipeCount, String imageUrl) {
        this.id = id;
        this.customerId = accountId;
        this.accountId = accountId;
        this.title = title;
        this.recipeCount = recipeCount;
        this.imageUrl = imageUrl;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getCustomerId() { return customerId == null ? accountId : customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getRecipeCount() { return recipeCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
