package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class WarehouseDto {
    @SerializedName("_id")
    private String id;

    @SerializedName(value = "code", alternate = {"ma_kho"})
    private String code;

    @SerializedName(value = "name", alternate = {"ten_kho"})
    private String name;

    private String address;

    @SerializedName(value = "location", alternate = {"toa_do"})
    private Location location;

    @SerializedName("isActive")
    private Boolean active;

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public static class Location {
        public double lat;
        public double lng;
    }
}
