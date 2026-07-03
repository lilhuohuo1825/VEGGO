package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.AuthApi;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.request.FirebaseLoginRequest;
import com.veggo.app.data.remote.request.ForgotPasswordRequest;
import com.veggo.app.data.remote.request.LoginRequest;
import com.veggo.app.data.remote.request.RegisterRequest;
import com.veggo.app.data.remote.request.ResetPasswordRequest;
import com.veggo.app.domain.repository.AuthRepository;

import java.util.Map;

import retrofit2.Callback;

public class AuthRepositoryImpl implements AuthRepository {
    private final AuthApi authApi;

    public AuthRepositoryImpl() {
        this.authApi = ApiClient.createService(AuthApi.class);
    }

    @Override
    public void login(String phone, String password, Callback<UserDto> callback) {
        authApi.login(new LoginRequest(phone, password)).enqueue(callback);
    }

    @Override
    public void register(String phone, String password, String fullName, String email, Callback<UserDto> callback) {
        authApi.register(new RegisterRequest(phone, password, fullName, email)).enqueue(callback);
    }

    @Override
    public void forgotPassword(String phone, Callback<Map<String, String>> callback) {
        authApi.forgotPassword(new ForgotPasswordRequest(phone)).enqueue(callback);
    }

    @Override
    public void resetPassword(String phone, String otp, String newPassword, Callback<Map<String, String>> callback) {
        authApi.resetPassword(new ResetPasswordRequest(phone, otp, newPassword)).enqueue(callback);
    }

    @Override
    public void firebaseLogin(String idToken, Callback<UserDto> callback) {
        authApi.firebaseLogin(new FirebaseLoginRequest(idToken)).enqueue(callback);
    }
}
