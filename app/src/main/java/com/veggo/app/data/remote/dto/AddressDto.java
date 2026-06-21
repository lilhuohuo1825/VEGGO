package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AddressDto {
    @SerializedName("id")
    private String id;

    @SerializedName("_id")
    private String mongoId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    @SerializedName("province")
    private String province;

    @SerializedName("district")
    private String district;

    @SerializedName("ward")
    private String ward;

    @SerializedName("detail")
    private String detail;

    @SerializedName("isDefault")
    private boolean isDefault;

    public String getId() {
        return id != null ? id : mongoId;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email == null ? "" : email; }
    public String getProvince() { return province; }
    public String getDistrict() { return district; }
    public String getWard() { return ward; }
    public String getDetail() { return detail; }
    public boolean isDefault() { return isDefault; }
}
