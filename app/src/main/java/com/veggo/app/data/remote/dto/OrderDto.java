package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDto {
    @SerializedName("_id")
    private String id;

    /** Mã đơn hàng dạng ORD... */
    @SerializedName(value = "orderId", alternate = {"OrderID"})
    private String orderId;

    /** CustomerID (CUS000XXX) – backend trả về field này trong userId */
    @SerializedName(value = "userId", alternate = {"CustomerID"})
    private String userId;

    private String paymentMethod;
    private String paymentStatus;
    private List<OrderItemDto> items = new ArrayList<>();
    private long subtotal;
    private long shippingFee;
    private long shippingDiscount;
    private long discount;
    private long total;
    private long totalAmount;
    private String status;
    private String rejectReason;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> shippingInfo;
    private String warehouseId;
    private String createdAt;
    @SerializedName("CarbonPointEarned")
    private int carbonPointEarned;
    @SerializedName("TotalCarbonEmission")
    private double totalCarbonEmission;

    public String getId() { return orderId != null && !orderId.isEmpty() ? orderId : id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }

    public long getShippingFee() { return shippingFee; }
    public void setShippingFee(long shippingFee) { this.shippingFee = shippingFee; }

    public long getShippingDiscount() { return shippingDiscount; }
    public void setShippingDiscount(long shippingDiscount) { this.shippingDiscount = shippingDiscount; }

    public long getDiscount() { return discount; }
    public void setDiscount(long discount) { this.discount = discount; }

    public long getTotal() { return totalAmount > 0 ? totalAmount : total; }
    public void setTotal(long total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Map<String, Object> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }
    public Map<String, Object> getShippingInfo() { return shippingInfo; }
    public void setShippingInfo(Map<String, Object> shippingInfo) { this.shippingInfo = shippingInfo; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public int getCarbonPointEarned() { return carbonPointEarned; }
    public double getTotalCarbonEmission() { return totalCarbonEmission; }

    public static class OrderItemDto {
        @SerializedName("sku")
        private String sku;
        private String productId;
        @SerializedName(value = "name", alternate = {"productName"})
        private String name;
        private long price;
        private long originalPrice;
        private int quantity;
        @SerializedName(value = "imageUrl", alternate = {"image"})
        private String imageUrl;
        private String unit;
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
        public long getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public void setSku(String sku) { this.sku = sku; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public int getCarbonPointEarned() { return carbonPointEarned; }
        public double getTotalCarbonEmission() { return totalCarbonEmission; }
    }
}
