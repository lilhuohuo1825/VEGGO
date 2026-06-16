package com.veggo.app.presentation.auth.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String objectId;

    @SerializedName("CustomerID")
    private String customerId;

    @SerializedName("Phone")
    private String phone;

    @SerializedName("Password")
    private String password;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("Email")
    private String email;

    @SerializedName("Address")
    private String address;

    @SerializedName("CarbonPoint")
    private int carbonPoint;

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCarbonPoint(int carbonPoint) {
        this.carbonPoint = carbonPoint;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public int getCarbonPoint() {
        return carbonPoint;
    }
}
