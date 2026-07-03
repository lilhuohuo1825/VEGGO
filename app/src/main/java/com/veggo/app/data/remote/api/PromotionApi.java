package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.FlashSaleResponseDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.HomeProductResponse;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PromotionApi {
    @GET("promotions")
    Call<List<PromotionDto>> getPromotions(
            @retrofit2.http.Query("customerId") String customerId,
            @retrofit2.http.Query("code") String code,
            @retrofit2.http.Query("surface") String surface
    );

    @GET("promotions/{id}")
    Call<PromotionDto> getPromotionById(@retrofit2.http.Path("id") String id);

    @GET("promotions/{id}/products")
    Call<HomeProductResponse> getPromotionProducts(
            @retrofit2.http.Path("id") String id,
            @retrofit2.http.Query("limit") Integer limit,
            @retrofit2.http.Query("skip") Integer skip
    );

    @GET("promotions/flash-sales")
    Call<FlashSaleResponseDto> getFlashSales();

    @GET("promotion-targets")
    Call<ApiListResponseDto<PromotionTargetDto>> getPromotionTargets();

    @GET("promotion-usages")
    Call<ApiListResponseDto<PromotionUsageDto>> getPromotionUsages();
}
