package com.veggo.app.data.remote.dto;

import java.util.List;

public class FridgeBatchRequestDto {
    private final List<FridgeItemRequestDto> items;

    public FridgeBatchRequestDto(List<FridgeItemRequestDto> items) {
        this.items = items;
    }

    public List<FridgeItemRequestDto> getItems() {
        return items;
    }

    public static class FridgeItemRequestDto {
        private final String name;
        private final double quantity;
        private final String purchaseDate;
        private final String expiryDate;
        private final String sku;
        private final String orderId;
        private final String image;
        private final List<String> images;
        private final String unit;
        private final String source;
        private String locationCode;
        private final boolean remindBeforeExpiry;

        public FridgeItemRequestDto(
                String name,
                double quantity,
                String purchaseDate,
                String expiryDate,
                String sku,
                String orderId,
                String image,
                List<String> images,
                String unit,
                String source,
                String locationCode,
                boolean remindBeforeExpiry
        ) {
            this.name = name;
            this.quantity = quantity;
            this.purchaseDate = purchaseDate;
            this.expiryDate = expiryDate;
            this.sku = sku;
            this.orderId = orderId;
            this.image = image;
            this.images = images;
            this.unit = unit;
            this.source = source;
            this.locationCode = locationCode;
            this.remindBeforeExpiry = remindBeforeExpiry;
        }

        public String getName() { return name; }
        public double getQuantity() { return quantity; }
        public String getPurchaseDate() { return purchaseDate; }
        public String getExpiryDate() { return expiryDate; }
        public String getSku() { return sku; }
        public String getOrderId() { return orderId; }
        public String getImage() { return image; }
        public List<String> getImages() { return images; }
        public String getUnit() { return unit; }
        public String getSource() { return source; }
        public String getLocationCode() { return locationCode; }
        public boolean isRemindBeforeExpiry() { return remindBeforeExpiry; }
        public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    }
}
