package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.AccessTokenResponseDto;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.request.FirebaseLoginRequest;
import com.veggo.app.data.remote.request.ForgotPasswordRequest;
import com.veggo.app.data.remote.request.LoginRequest;
import com.veggo.app.data.remote.request.RefreshAccessTokenRequest;
import com.veggo.app.data.remote.request.RegisterRequest;
import com.veggo.app.data.remote.request.ResetPasswordRequest;
import com.veggo.app.data.remote.request.VerifyForgotPasswordOtpRequest;
import com.veggo.app.data.remote.request.VerifyGuestOrderOtpRequest;

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

    @POST("users/verify-forgot-password-otp")
    Call<Map<String, String>> verifyForgotPasswordOtp(@Body VerifyForgotPasswordOtpRequest request);

    @POST("users/guest-order-otp")
    Call<Map<String, String>> sendGuestOrderOtp(@Body ForgotPasswordRequest request);

    @POST("users/guest-order-otp/verify")
    Call<Map<String, String>> verifyGuestOrderOtp(@Body VerifyGuestOrderOtpRequest request);

    @POST("users/sync")
    Call<UserDto> syncFirebaseUser(@Body UserDto user);

    @POST("users/firebase-login")
    Call<UserDto> firebaseLogin(@Body FirebaseLoginRequest request);

    @POST("users/refresh-access-token")
    Call<AccessTokenResponseDto> refreshAccessToken(@Body RefreshAccessTokenRequest request);
}
