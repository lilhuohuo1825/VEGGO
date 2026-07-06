package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("CustomerID")
    private String customerId;

    @SerializedName("Phone")
    private String phone;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("Email")
    private String email;

    @SerializedName("Address")
    private String address;

    @SerializedName("CustomerType")
    private String customerType;

    @SerializedName("TotalSpent")
    private double totalSpent;

    @SerializedName("CarbonPoint")
    private int carbonPoint;

    @SerializedName("CertificateID")
    private String certificateId;

    @SerializedName("PasswordVersion")
    private int passwordVersion;

    @SerializedName("LastPasswordReset")
    private String lastPasswordReset;

    @SerializedName("firebaseUid")
    private String firebaseUid;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("BirthDay")
    private String birthDay;

    @SerializedName("Gender")
    private String gender;

    @SerializedName("addresses")
    private List<AddressDto> addresses;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public int getCarbonPoint() { return carbonPoint; }
    public void setCarbonPoint(int carbonPoint) { this.carbonPoint = carbonPoint; }

    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }

    public int getPasswordVersion() { return passwordVersion; }
    public void setPasswordVersion(int passwordVersion) { this.passwordVersion = passwordVersion; }

    public String getLastPasswordReset() { return lastPasswordReset; }
    public void setLastPasswordReset(String lastPasswordReset) { this.lastPasswordReset = lastPasswordReset; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBirthDay() { return birthDay; }
    public void setBirthDay(String birthDay) { this.birthDay = birthDay; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public List<AddressDto> getAddresses() { return addresses; }
    public void setAddresses(List<AddressDto> addresses) { this.addresses = addresses; }

    public static class AddressDto {
        @SerializedName("receiverName")
        private String receiverName;
        @SerializedName("phone")
        private String phone;
        @SerializedName("line1")
        private String line1;
        @SerializedName("ward")
        private String ward;
        @SerializedName("district")
        private String district;
        @SerializedName("city")
        private String city;
        @SerializedName("isDefault")
        private boolean isDefault;

        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }
}
