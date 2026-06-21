package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CartApi {
    @GET("cart/{customerId}")
    Call<CartDto> getCart(@Path("customerId") String customerId);

    @POST("cart/{customerId}/items")
    Call<CartDto> addItem(@Path("customerId") String customerId, @Body CartItemRequestDto item);

    @PATCH("cart/{customerId}/items/{sku}")
    Call<CartDto> updateItemQuantity(
            @Path("customerId") String customerId,
            @Path("sku") String sku,
            @Body Map<String, Integer> body
    );

    @DELETE("cart/{customerId}/items/{sku}")
    Call<CartDto> removeItem(@Path("customerId") String customerId, @Path("sku") String sku);

    @DELETE("cart/{customerId}")
    Call<CartDto> clearCart(@Path("customerId") String customerId);
}
