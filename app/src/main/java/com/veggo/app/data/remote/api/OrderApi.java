package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.OrderDto;
import com.veggo.app.data.remote.dto.OrderNotificationDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {
    @GET("orders/{userId}")
    Call<List<OrderDto>> getOrders(@Path("userId") String userId);

    @GET("orders/guest/search")
    Call<OrderDto> searchGuestOrder(
            @Query("orderId") String orderId,
            @Query("phone") String phone
    );

    @GET("orders/id/{orderId}")
    Call<OrderDto> getOrderById(@Path("orderId") String orderId);

    @POST("orders")
    Call<OrderDto> createOrder(@Body Map<String, Object> order);

    @PATCH("orders/{orderId}/status")
    Call<Map<String, Object>> updateOrderStatus(
            @Path("orderId") String orderId,
            @Body Map<String, String> body
    );

    @PATCH("orders/{orderId}/payment-status")
    Call<Map<String, Object>> updatePaymentStatus(
            @Path("orderId") String orderId,
            @Body Map<String, String> body
    );

    @GET("orders/notifications/{customerId}")
    Call<List<OrderNotificationDto>> getOrderNotifications(@Path("customerId") String customerId);

    @PATCH("orders/notifications/{customerId}/read-all")
    Call<Map<String, Object>> markAllOrderNotificationsRead(@Path("customerId") String customerId);

    @PATCH("orders/notifications/item/{notificationId}/read")
    Call<Map<String, Object>> markOrderNotificationRead(@Path("notificationId") String notificationId);
}
