package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.dto.UserProfileDto;
import com.veggo.app.presentation.profile.TastePreferenceStore;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface UserApi {
    @GET("users/firebase/{firebaseUid}")
    Call<UserDto> getUserByFirebaseUid(@Path("firebaseUid") String firebaseUid);

    @GET("users/phone/{phone}")
    Call<UserDto> getUserByPhone(@Path("phone") String phone);

    @POST("users/sync")
    Call<UserDto> syncUser(@Body UserDto user);

    @Multipart
    @PUT("users/profile")
    Call<UserProfileDto> updateProfile(
            @Header("X-Current-Phone") String currentPhone,
            @Part("name") RequestBody name,
            @Part("phone") RequestBody phone,
            @Part("email") RequestBody email,
            @Part MultipartBody.Part avatar
    );

    @GET("users/{customerId}/taste-preferences")
    Call<TastePreferenceStore.RemoteTastePreferences> getTastePreferences(@Path("customerId") String customerId);

    @PUT("users/{customerId}/taste-preferences")
    Call<TastePreferenceStore.RemoteTastePreferences> saveTastePreferences(
            @Path("customerId") String customerId,
            @Body TastePreferenceStore.RemoteTastePreferences preferences
    );
}
