package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_recipe_comments")
public class CommunityRecipeCommentEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String recipeId;
    private String userName;
    private String userImageUrl;
    private String content;
    private int likeCount;
    private int sortOrder;
    @Ignore
    private boolean likedByCurrentUser;

    public CommunityRecipeCommentEntity(@NonNull String id, String recipeId, String userName, String userImageUrl, String content, int likeCount, int sortOrder) {
        this.id = id;
        this.recipeId = recipeId;
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.content = content;
        this.likeCount = likeCount;
        this.sortOrder = sortOrder;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserImageUrl() { return userImageUrl; }
    public void setUserImageUrl(String userImageUrl) { this.userImageUrl = userImageUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
}
