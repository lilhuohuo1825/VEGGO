package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "blogs")
public class BlogEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String imageUrl;
    private String title;
    private String excerpt;
    private String publishedAt;
    private long publishedAtMillis;
    private String author;
    private String categoryTag;
    private String content;
    @Ignore
    private int likeCount;
    @Ignore
    private boolean likedByCurrentUser;

    public BlogEntity(
            @NonNull String id,
            String imageUrl,
            String title,
            String excerpt,
            String publishedAt,
            long publishedAtMillis,
            String author,
            String categoryTag,
            String content
    ) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.excerpt = excerpt;
        this.publishedAt = publishedAt;
        this.publishedAtMillis = publishedAtMillis;
        this.author = author;
        this.categoryTag = categoryTag;
        this.content = content;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public long getPublishedAtMillis() { return publishedAtMillis; }
    public void setPublishedAtMillis(long publishedAtMillis) { this.publishedAtMillis = publishedAtMillis; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCategoryTag() { return categoryTag; }
    public void setCategoryTag(String categoryTag) { this.categoryTag = categoryTag; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
}
