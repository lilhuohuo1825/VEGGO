package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ProductReviewsDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import java.util.List;

public interface ReviewApi {
    @GET("reviews/sku/{sku}")
    Call<ProductReviewsDto> getReviewsBySku(@Path("sku") String sku);
    
    @GET("reviews")
    Call<List<ProductReviewsDto>> getAllReviews();
}
