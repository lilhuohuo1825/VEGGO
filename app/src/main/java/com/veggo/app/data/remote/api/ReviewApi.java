package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ProductReviewsDto;
import com.veggo.app.data.remote.dto.ReviewMediaUploadResponseDto;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ReviewApi {
    @GET("reviews/sku/{sku}")
    Call<ProductReviewsDto> getReviewsBySku(@Path("sku") String sku);
    
    @GET("reviews")
    Call<List<ProductReviewsDto>> getAllReviews();

    @POST("reviews")
    Call<Map<String, Object>> submitReview(@Body Map<String, Object> body);

    @Multipart
    @POST("reviews/uploads/media")
    Call<ReviewMediaUploadResponseDto> uploadReviewMedia(@Part List<MultipartBody.Part> media);
}
