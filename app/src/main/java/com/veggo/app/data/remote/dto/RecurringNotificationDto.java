package com.veggo.app.data.remote.dto;

public class RecurringNotificationDto {
    private String id;
    private String customerId;
    private String recurringOrderId;
    private String occurrenceDate;
    private String type;
    private String title;
    private String body;
    private String action;
    private long createdAt;
    private boolean read;
    private boolean outsideApp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRecurringOrderId() { return recurringOrderId; }
    public void setRecurringOrderId(String recurringOrderId) { this.recurringOrderId = recurringOrderId; }
    public String getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(String occurrenceDate) { this.occurrenceDate = occurrenceDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public boolean isOutsideApp() { return outsideApp; }
    public void setOutsideApp(boolean outsideApp) { this.outsideApp = outsideApp; }
}
