package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ForecastResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ForecastApi {

    /**
     * Lấy dữ liệu dự báo biến động giá ngắn hạn của sản phẩm
     * Trả về ForecastResponseDto chứa thông tin tỉ lệ biến động, xu hướng và lý do thời tiết
     */
    @GET("forecast/{productId}")
    Call<ForecastResponseDto> getProductForecast(@Path("productId") String productId);
}
