package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CategoryDto {
    @SerializedName("_id")
    private Id id;
    
    @SerializedName("CategoryID")
    private String categoryID;
    
    @SerializedName("CategoryName")
    private String categoryName;
    
    @SerializedName("Subcategories")
    private List<Subcategory> subcategories;

    public String getCategoryName() { return categoryName; }
    public String getCategoryID() { return categoryID; }
    public List<Subcategory> getSubcategories() { return subcategories; }

    public static class Id {
        @SerializedName("$oid")
        private String oid;
        public String getOid() { return oid; }
    }

    public static class Subcategory {
        @SerializedName("SubcategoryID")
        private String subcategoryID;
        @SerializedName("SubcategoryName")
        private String subcategoryName;
        @SerializedName("img")
        private String img;

        public String getSubcategoryID() { return subcategoryID; }
        public String getSubcategoryName() { return subcategoryName; }
        public String getImg() { return img; }
    }
}
