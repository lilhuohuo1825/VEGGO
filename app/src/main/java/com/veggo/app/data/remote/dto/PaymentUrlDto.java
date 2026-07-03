package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PaymentUrlDto {
    @SerializedName("paymentUrl")
    private String paymentUrl;

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
}
