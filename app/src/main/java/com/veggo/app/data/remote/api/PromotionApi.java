package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.PromotionDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PromotionApi {
    @GET("promotions")
    Call<List<PromotionDto>> getAllPromotions();

    @GET("promotions/{code}")
    Call<PromotionDto> getPromotionByCode(@Path("code") String code);
}
