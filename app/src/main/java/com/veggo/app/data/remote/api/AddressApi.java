package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.AddressDto;
import com.veggo.app.data.remote.dto.AddressRequestDto;
import com.google.gson.JsonArray;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AddressApi {
    @GET("addresses/user/{userId}")
    Call<List<AddressDto>> getAddresses(@Path("userId") String userId);

    @POST("addresses")
    Call<AddressDto> createAddress(@Body AddressRequestDto request);

    @PUT("addresses/{id}")
    Call<AddressDto> updateAddress(@Path("id") String id, @Body AddressRequestDto request);

    @DELETE("addresses/{id}")
    Call<Void> deleteAddress(@Path("id") String id);

    @PATCH("addresses/{id}/default")
    Call<AddressDto> setDefaultAddress(@Path("id") String id);

    @GET("tree_complete")
    Call<JsonArray> getTreeComplete();
}
