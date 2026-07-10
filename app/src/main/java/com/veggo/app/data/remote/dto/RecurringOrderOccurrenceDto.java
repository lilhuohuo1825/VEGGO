package com.veggo.app.data.remote.dto;

public class RecurringOrderOccurrenceDto {
    private String orderId;
    private String customerId;
    private String date;
    private String status;
    private String placedOrderId;
    private long confirmNotifiedAt;
    private long deliveryReminderNotifiedAt;
    private long notifiedAt;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlacedOrderId() { return placedOrderId; }
    public void setPlacedOrderId(String placedOrderId) { this.placedOrderId = placedOrderId; }
    public long getConfirmNotifiedAt() { return confirmNotifiedAt; }
    public void setConfirmNotifiedAt(long confirmNotifiedAt) { this.confirmNotifiedAt = confirmNotifiedAt; }
    public long getDeliveryReminderNotifiedAt() { return deliveryReminderNotifiedAt; }
    public void setDeliveryReminderNotifiedAt(long deliveryReminderNotifiedAt) {
        this.deliveryReminderNotifiedAt = deliveryReminderNotifiedAt;
    }
    public long getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(long notifiedAt) { this.notifiedAt = notifiedAt; }
}
