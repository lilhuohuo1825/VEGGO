package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CartApi {
    @GET("cart/{userId}")
    Call<CartDto> getCart(@Path("userId") String userId);

    @POST("cart/{userId}/items")
    Call<CartDto> addItem(@Path("userId") String userId, @Body CartItemRequestDto item);

    @DELETE("cart/{userId}/items/{productId}")
    Call<CartDto> removeItem(@Path("userId") String userId, @Path("productId") String productId);

    @DELETE("cart/{userId}")
    Call<CartDto> clearCart(@Path("userId") String userId);
}
