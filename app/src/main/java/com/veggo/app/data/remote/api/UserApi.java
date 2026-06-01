package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UserApi {
    @GET("users/firebase/{firebaseUid}")
    Call<UserDto> getUserByFirebaseUid(@Path("firebaseUid") String firebaseUid);

    @POST("users/sync")
    Call<UserDto> syncUser(@Body UserDto user);
}
