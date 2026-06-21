package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.FlashSaleResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PromotionApi {
    @GET("promotions/flash-sales")
    Call<FlashSaleResponseDto> getFlashSales();
}
