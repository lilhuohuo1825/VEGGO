package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.HomeProductResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PromotionApi {
    @GET("promotions")
    Call<List<PromotionDto>> getPromotions();

    @GET("promotions/{id}")
    Call<PromotionDto> getPromotionById(@retrofit2.http.Path("id") String id);

    @GET("promotions/{id}/products")
    Call<HomeProductResponse> getPromotionProducts(
            @retrofit2.http.Path("id") String id,
            @retrofit2.http.Query("limit") Integer limit,
            @retrofit2.http.Query("skip") Integer skip
    );
}
