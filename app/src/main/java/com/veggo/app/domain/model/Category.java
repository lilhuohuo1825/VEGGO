package com.veggo.app.domain.model;

public class Category {
    private final String id;
    private final String name;
    private final int iconRes;

    public Category(String id, String name, int iconRes) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
}
