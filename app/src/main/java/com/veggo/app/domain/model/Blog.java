package com.veggo.app.domain.model;

public class Blog {
    private String id;
    private String img;
    private String title;
    private String excerpt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
}