package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.RecurringNotificationDto;
import com.veggo.app.data.remote.dto.RecurringOrderDto;
import com.veggo.app.data.remote.dto.RecurringOrderOccurrenceDto;
import com.veggo.app.data.remote.dto.RecurringOrderSyncRequestDto;
import com.veggo.app.data.remote.dto.RecurringOrderSyncResponseDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RecurringOrderApi {
    @GET("recurring-orders/customer/{customerId}")
    Call<RecurringOrderSyncResponseDto> getCustomerData(@Path("customerId") String customerId);

    @POST("recurring-orders")
    Call<RecurringOrderDto> createOrder(@Body RecurringOrderDto order);

    @PUT("recurring-orders/{recurringId}")
    Call<RecurringOrderDto> updateOrder(
            @Path("recurringId") String recurringId,
            @Body RecurringOrderDto order
    );

    @DELETE("recurring-orders/{recurringId}")
    Call<Map<String, Object>> deleteOrder(@Path("recurringId") String recurringId);

    @PUT("recurring-orders/{recurringId}/occurrences/{date}")
    Call<RecurringOrderOccurrenceDto> upsertOccurrence(
            @Path("recurringId") String recurringId,
            @Path("date") String date,
            @Body RecurringOrderOccurrenceDto occurrence
    );

    @POST("recurring-orders/sync")
    Call<RecurringOrderSyncResponseDto> syncOrders(@Body RecurringOrderSyncRequestDto request);

    @POST("recurring-orders/notifications")
    Call<RecurringNotificationDto> createNotification(@Body RecurringNotificationDto notification);

    @PATCH("recurring-orders/notifications/{notificationId}/read")
    Call<RecurringNotificationDto> markNotificationRead(@Path("notificationId") String notificationId);

    @PATCH("recurring-orders/notifications/customer/{customerId}/read-all")
    Call<Map<String, Object>> markAllNotificationsRead(@Path("customerId") String customerId);
}
