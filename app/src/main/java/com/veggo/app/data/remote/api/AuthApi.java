package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.request.FirebaseLoginRequest;
import com.veggo.app.data.remote.request.ForgotPasswordRequest;
import com.veggo.app.data.remote.request.LoginRequest;
import com.veggo.app.data.remote.request.RegisterRequest;
import com.veggo.app.data.remote.request.ResetPasswordRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("users/login")
    Call<UserDto> login(@Body LoginRequest request);

    @POST("users/register")
    Call<UserDto> register(@Body RegisterRequest request);

    @POST("users/forgot-password")
    Call<Map<String, String>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("users/reset-password")
    Call<Map<String, String>> resetPassword(@Body ResetPasswordRequest request);

    @POST("users/sync")
    Call<UserDto> syncFirebaseUser(@Body UserDto user);

    @POST("users/firebase-login")
    Call<UserDto> firebaseLogin(@Body FirebaseLoginRequest request);
}
