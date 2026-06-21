package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;

public class BlogCommentEntity {
    @NonNull
    private String id;
    private String blogId;
    private String customerId;
    private String userName;
    private String userImageUrl;
    private String content;
    private int likeCount;
    private String createdAt;
    private boolean likedByCurrentUser;

    public BlogCommentEntity() {
        this.id = "";
    }

    public BlogCommentEntity(@NonNull String id, String blogId, String customerId, String userName, String userImageUrl, String content, int likeCount, String createdAt) {
        this.id = id;
        this.blogId = blogId;
        this.customerId = customerId;
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getBlogId() { return blogId; }
    public void setBlogId(String blogId) { this.blogId = blogId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserImageUrl() { return userImageUrl; }
    public void setUserImageUrl(String userImageUrl) { this.userImageUrl = userImageUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
}
