package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_follows")
public class CommunityFollowEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String chefId;
    private String relationType;
    private String name;
    private String location;
    private String imageUrl;
    private boolean following;

    public CommunityFollowEntity(
            @NonNull String id,
            String chefId,
            String relationType,
            String name,
            String location,
            String imageUrl,
            boolean following
    ) {
        this.id = id;
        this.chefId = chefId;
        this.relationType = relationType;
        this.name = name;
        this.location = location;
        this.imageUrl = imageUrl;
        this.following = following;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getChefId() { return chefId; }
    public void setChefId(String chefId) { this.chefId = chefId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isFollowing() { return following; }
    public void setFollowing(boolean following) { this.following = following; }
}
