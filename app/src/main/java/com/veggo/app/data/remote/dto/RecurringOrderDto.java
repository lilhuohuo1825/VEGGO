package com.veggo.app.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

public class RecurringOrderDto {
    private String id;
    private String customerId;
    private String name;
    private String frequency;
    private String deliveryDate;
    private String deliverySlot;
    private String receiverName;
    private String receiverPhone;
    private String city;
    private String district;
    private String ward;
    private String detailAddress;
    private String itemSummary;
    private long estimatedTotal;
    private double carbonPoints;
    private List<RecurringProductItemDto> items = new ArrayList<>();
    private String status;
    private String createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getDeliverySlot() { return deliverySlot; }
    public void setDeliverySlot(String deliverySlot) { this.deliverySlot = deliverySlot; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public String getItemSummary() { return itemSummary; }
    public void setItemSummary(String itemSummary) { this.itemSummary = itemSummary; }
    public long getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(long estimatedTotal) { this.estimatedTotal = estimatedTotal; }
    public double getCarbonPoints() { return carbonPoints; }
    public void setCarbonPoints(double carbonPoints) { this.carbonPoints = carbonPoints; }
    public List<RecurringProductItemDto> getItems() { return items; }
    public void setItems(List<RecurringProductItemDto> items) { this.items = items; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static class RecurringProductItemDto {
        private String productId;
        private String sku;
        private String name;
        private String imageUrl;
        private String unit;
        private int quantity;
        private double selectedWeight;
        private long unitPrice;
        private long baseUnitPrice;
        private double baseCarbonSavingPoint;
        private double emissionFactor;
        private boolean hasWeightOptions;
        private double carbonPoints;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getSelectedWeight() { return selectedWeight; }
        public void setSelectedWeight(double selectedWeight) { this.selectedWeight = selectedWeight; }
        public long getUnitPrice() { return unitPrice; }
        public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
        public long getBaseUnitPrice() { return baseUnitPrice; }
        public void setBaseUnitPrice(long baseUnitPrice) { this.baseUnitPrice = baseUnitPrice; }
        public double getBaseCarbonSavingPoint() { return baseCarbonSavingPoint; }
        public void setBaseCarbonSavingPoint(double baseCarbonSavingPoint) {
            this.baseCarbonSavingPoint = baseCarbonSavingPoint;
        }
        public double getEmissionFactor() { return emissionFactor; }
        public void setEmissionFactor(double emissionFactor) { this.emissionFactor = emissionFactor; }
        public boolean isHasWeightOptions() { return hasWeightOptions; }
        public void setHasWeightOptions(boolean hasWeightOptions) { this.hasWeightOptions = hasWeightOptions; }
        public double getCarbonPoints() { return carbonPoints; }
        public void setCarbonPoints(double carbonPoints) { this.carbonPoints = carbonPoints; }
    }
}
