package com.veggo.app.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

public class RecurringOrderSyncResponseDto {
    private List<RecurringOrderDto> orders = new ArrayList<>();
    private List<RecurringOrderOccurrenceDto> occurrences = new ArrayList<>();
    private List<RecurringNotificationDto> notifications = new ArrayList<>();

    public List<RecurringOrderDto> getOrders() { return orders; }
    public void setOrders(List<RecurringOrderDto> orders) { this.orders = orders; }
    public List<RecurringOrderOccurrenceDto> getOccurrences() { return occurrences; }
    public void setOccurrences(List<RecurringOrderOccurrenceDto> occurrences) {
        this.occurrences = occurrences;
    }
    public List<RecurringNotificationDto> getNotifications() { return notifications; }
    public void setNotifications(List<RecurringNotificationDto> notifications) {
        this.notifications = notifications;
    }
}
