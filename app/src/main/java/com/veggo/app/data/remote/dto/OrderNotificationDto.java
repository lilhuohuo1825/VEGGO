package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class OrderNotificationDto {
    @SerializedName(value = "_id", alternate = {"id"})
    private Object id;
    private String category;
    private String targetType;
    @SerializedName(value = "targetId", alternate = {"OrderID", "orderId"})
    private String targetId;
    private String sku;
    private String title;
    private String body;
    private String action;
    private String type;
    private Boolean isRead;
    private Object createdAt;

    public String getId() {
        return objectText(id);
    }

    public String getCategory() {
        return category;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public boolean isRead() {
        return Boolean.TRUE.equals(isRead);
    }

    public String getCreatedAtText() {
        return objectText(createdAt);
    }

    @SuppressWarnings("unchecked")
    private static String objectText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map) {
            Object date = ((Map<String, Object>) value).get("$date");
            if (date != null) {
                return String.valueOf(date);
            }
            Object oid = ((Map<String, Object>) value).get("$oid");
            if (oid != null) {
                return String.valueOf(oid);
            }
        }
        return String.valueOf(value);
    }
}
