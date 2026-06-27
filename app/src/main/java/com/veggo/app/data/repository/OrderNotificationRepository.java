package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.dto.OrderNotificationDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class OrderNotificationRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    private final OrderApi api;

    public OrderNotificationRepository() {
        api = ApiClient.createService(OrderApi.class);
    }

    public void getNotifications(String customerId, Callback<List<OrderNotificationDto>> callback) {
        if (isBlank(customerId)) {
            callback.onResult(new ArrayList<>());
            return;
        }
        api.getOrderNotifications(customerId).enqueue(new retrofit2.Callback<List<OrderNotificationDto>>() {
            @Override
            public void onResponse(
                    Call<List<OrderNotificationDto>> call,
                    Response<List<OrderNotificationDto>> response
            ) {
                callback.onResult(response.isSuccessful() && response.body() != null
                        ? response.body()
                        : new ArrayList<>());
            }

            @Override
            public void onFailure(Call<List<OrderNotificationDto>> call, Throwable t) {
                callback.onResult(new ArrayList<>());
            }
        });
    }

    public List<OrderNotificationDto> getNotificationsSync(String customerId) {
        if (isBlank(customerId)) {
            return new ArrayList<>();
        }
        try {
            Response<List<OrderNotificationDto>> response = api.getOrderNotifications(customerId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public void markAllReadSync(String customerId) {
        if (isBlank(customerId)) {
            return;
        }
        try {
            api.markAllOrderNotificationsRead(customerId).execute();
        } catch (Exception ignored) {
        }
    }

    public void markReadSync(String notificationId) {
        if (isBlank(notificationId)) {
            return;
        }
        try {
            api.markOrderNotificationRead(notificationId).execute();
        } catch (Exception ignored) {
        }
    }

    public static int countUnread(List<OrderNotificationDto> notifications) {
        int count = 0;
        if (notifications == null) {
            return count;
        }
        for (OrderNotificationDto notification : notifications) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    public static OrderNotificationDto latestUnread(List<OrderNotificationDto> notifications) {
        if (notifications == null) {
            return null;
        }
        for (OrderNotificationDto notification : notifications) {
            if (notification != null && !notification.isRead()) {
                return notification;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
