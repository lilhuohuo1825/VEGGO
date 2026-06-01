package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.OrderDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OrderApi {
    @GET("orders/{userId}")
    Call<List<OrderDto>> getOrders(@Path("userId") String userId);

    @POST("orders")
    Call<OrderDto> createOrder(@Body OrderDto order);
}
