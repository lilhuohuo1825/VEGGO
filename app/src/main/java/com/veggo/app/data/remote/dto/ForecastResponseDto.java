package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ForecastResponseDto {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ForecastData data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public ForecastData getData() {
        return data;
    }

    public static class ForecastData {
        @SerializedName("productId")
        private Object productId; // Can be a string ID or populated ProductDto object

        @SerializedName("sku")
        private String sku;

        @SerializedName("currentPrice")
        private double currentPrice;

        @SerializedName("predictedPrice7Days")
        private double predictedPrice7Days;

        @SerializedName("changePercent")
        private double changePercent;

        @SerializedName("trend")
        private String trend; // "up", "down", "stable"

        @SerializedName("reason")
        private String reason;

        public Object getProductId() {
            return productId;
        }

        public String getSku() {
            return sku;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public double getPredictedPrice7Days() {
            return predictedPrice7Days;
        }

        public double getChangePercent() {
            return changePercent;
        }

        public String getTrend() {
            return trend;
        }

        public String getReason() {
            return reason;
        }
    }
}
