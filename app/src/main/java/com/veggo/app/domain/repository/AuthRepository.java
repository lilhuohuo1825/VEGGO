package com.veggo.app.domain.repository;

import com.veggo.app.data.remote.dto.UserDto;
import java.util.Map;
import retrofit2.Callback;

public interface AuthRepository {
    void login(String phone, String password, Callback<UserDto> callback);
    void register(String phone, String password, String fullName, String email, Callback<UserDto> callback);
    void forgotPassword(String phone, Callback<Map<String, String>> callback);
    void verifyForgotPasswordOtp(String phone, String otp, Callback<Map<String, String>> callback);
    void resetPassword(String phone, String otp, String newPassword, Callback<Map<String, String>> callback);
    void firebaseLogin(String idToken, Callback<UserDto> callback);
    void firebaseLogin(String idToken, String avatarUrl, Callback<UserDto> callback);
}
