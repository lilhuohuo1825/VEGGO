package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DishDto {
    @SerializedName("_id")
    private Id id;
    
    @SerializedName("ID")
    private String dishID;
    
    @SerializedName("Description")
    private String description;

    @SerializedName("Ingredients")
    private String ingredients;

    public String getDishID() { return dishID; }
    public String getDescription() { return description; }
    public String getIngredients() { return ingredients; }

    public static class Id {
        @SerializedName("$oid")
        private String oid;
        public String getOid() { return oid; }
    }
}
