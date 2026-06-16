package com.veggo.app.data.remote.request;

public class ResetPasswordRequest {
    private String phone;
    private String otp;
    private String newPassword;

    public ResetPasswordRequest(String phone, String otp, String newPassword) {
        this.phone = phone;
        this.otp = otp;
        this.newPassword = newPassword;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
