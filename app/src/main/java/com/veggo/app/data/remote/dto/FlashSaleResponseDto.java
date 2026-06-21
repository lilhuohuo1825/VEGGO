package com.veggo.app.data.remote.dto;

import java.util.List;

public class FlashSaleResponseDto {
    private boolean success;
    private List<FlashSaleItemDto> data;

    public boolean isSuccess() {
        return success;
    }

    public List<FlashSaleItemDto> getData() {
        return data;
    }

    public static class FlashSaleItemDto {
        private String id;
        private String sku;
        private String name;
        private long price;
        private String unit;
        private String discount;
        private String imageUrl;
        private float rating;

        public String getId() { return id; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public long getPrice() { return price; }
        public String getUnit() { return unit; }
        public String getDiscount() { return discount; }
        public String getImageUrl() { return imageUrl; }
        public float getRating() { return rating; }
    }
}
