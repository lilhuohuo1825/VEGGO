package com.veggo.app.data.remote.dto;

public class FridgeLocationDto {
    private String _id;
    private String locationCode;
    private String name;

    public FridgeLocationDto(String locationCode, String name) {
        this.locationCode = locationCode;
        this.name = name;
    }

    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
