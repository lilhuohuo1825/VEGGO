package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("users/sync")
    Call<UserDto> syncFirebaseUser(@Body UserDto user);
}
