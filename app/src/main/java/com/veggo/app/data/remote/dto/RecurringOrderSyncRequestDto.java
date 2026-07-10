package com.veggo.app.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

public class RecurringOrderSyncRequestDto {
    private String customerId;
    private List<RecurringOrderDto> orders = new ArrayList<>();
    private List<RecurringOrderOccurrenceDto> occurrences = new ArrayList<>();

    public RecurringOrderSyncRequestDto(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<RecurringOrderDto> getOrders() { return orders; }
    public void setOrders(List<RecurringOrderDto> orders) { this.orders = orders; }
    public List<RecurringOrderOccurrenceDto> getOccurrences() { return occurrences; }
    public void setOccurrences(List<RecurringOrderOccurrenceDto> occurrences) {
        this.occurrences = occurrences;
    }
}
