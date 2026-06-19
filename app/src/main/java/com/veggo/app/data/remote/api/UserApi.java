package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.presentation.profile.TastePreferenceStore;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;

public interface UserApi {
    @GET("users/firebase/{firebaseUid}")
    Call<UserDto> getUserByFirebaseUid(@Path("firebaseUid") String firebaseUid);

    @GET("users/phone/{phone}")
    Call<UserDto> getUserByPhone(@Path("phone") String phone);

    @POST("users/sync")
    Call<UserDto> syncUser(@Body UserDto user);

    @GET("users/{customerId}/taste-preferences")
    Call<TastePreferenceStore.RemoteTastePreferences> getTastePreferences(@Path("customerId") String customerId);

    @PUT("users/{customerId}/taste-preferences")
    Call<TastePreferenceStore.RemoteTastePreferences> saveTastePreferences(
            @Path("customerId") String customerId,
            @Body TastePreferenceStore.RemoteTastePreferences preferences
    );
}
