package com.veggo.app.data.remote.request;

public class RegisterRequest {
    private String phone;
    private String password;
    private String fullName;
    private String email;

    public RegisterRequest(String phone, String password, String fullName, String email) {
        this.phone = phone;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
