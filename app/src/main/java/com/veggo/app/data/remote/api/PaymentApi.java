package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.PaymentUrlDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentApi {
    @POST("payment/create-payment-url")
    Call<PaymentUrlDto> createVnpayUrl(@Body Map<String, Object> body);
}
