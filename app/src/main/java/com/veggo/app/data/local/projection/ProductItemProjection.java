package com.veggo.app.data.local.projection;

import androidx.room.ColumnInfo;

public class ProductItemProjection {
    @ColumnInfo(name = "id")
    private String id;
    
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "price")
    private long price;
    
    @ColumnInfo(name = "imageUrl")
    private String imageUrl;

    public ProductItemProjection(String id, String name, long price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
