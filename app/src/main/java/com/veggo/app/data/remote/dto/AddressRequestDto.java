package com.veggo.app.data.remote.dto;

public class AddressRequestDto {
    private final String userId;
    private final String name;
    private final String phone;
    private final String email;
    private final String province;
    private final String district;
    private final String ward;
    private final String detail;
    private final boolean isDefault;

    public AddressRequestDto(
            String userId,
            String name,
            String phone,
            String email,
            String province,
            String district,
            String ward,
            String detail,
            boolean isDefault
    ) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.detail = detail;
        this.isDefault = isDefault;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getProvince() { return province; }
    public String getDistrict() { return district; }
    public String getWard() { return ward; }
    public String getDetail() { return detail; }
    public boolean isDefault() { return isDefault; }
}
