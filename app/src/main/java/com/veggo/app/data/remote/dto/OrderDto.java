package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDto {
    @SerializedName("_id")
    private String id;
    @SerializedName("OrderID")
    private String orderId;
    private String userId;
    @SerializedName("CustomerID")
    private String customerId;
    private List<OrderItemDto> items = new ArrayList<>();
    private long subtotal;
    private long shippingFee;
    private long total;
    private long totalAmount;
    private String status;
    private Map<String, Object> shippingAddress;
    private String createdAt;
    @SerializedName("CarbonPointEarned")
    private int carbonPointEarned;
    @SerializedName("TotalCarbonEmission")
    private double totalCarbonEmission;

    public String getId() { return orderId != null && !orderId.isEmpty() ? orderId : id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return customerId != null && !customerId.isEmpty() ? customerId : userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }
    public long getShippingFee() { return shippingFee; }
    public void setShippingFee(long shippingFee) { this.shippingFee = shippingFee; }
    public long getTotal() { return totalAmount > 0 ? totalAmount : total; }
    public void setTotal(long total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Object> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public int getCarbonPointEarned() { return carbonPointEarned; }
    public double getTotalCarbonEmission() { return totalCarbonEmission; }

    public static class OrderItemDto {
        @SerializedName("sku")
        private String sku;
        private String productId;
        private String name;
        private long price;
        private int quantity;
        private String imageUrl;
        @SerializedName("CarbonPointEarned")
        private int carbonPointEarned;
        @SerializedName("TotalCarbonEmission")
        private double totalCarbonEmission;

        public String getSku() { return sku; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getPrice() { return price; }
        public void setPrice(long price) { this.price = price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public int getCarbonPointEarned() { return carbonPointEarned; }
        public double getTotalCarbonEmission() { return totalCarbonEmission; }
    }
}
