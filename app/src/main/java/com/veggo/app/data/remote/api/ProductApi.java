package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.HomeProductResponse;
import com.veggo.app.data.remote.dto.ProductDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    /**
     * Lấy sản phẩm cho trang chủ, hỗ trợ 4 tab:
     *   popular | newest | top_rated | best_price
     * Trả về wrapper { success, count, tab, data[] }
     */
    @GET("products/home")
    Call<HomeProductResponse> getHomeProducts(
            @Query("tab") String tab,
            @Query("limit") int limit,
            @Query("skip") int skip
    );

    /** Catalog sync — bắt buộc lite=true để backend trả kèm image/imageUrl. */
    @GET("products")
    Call<List<ProductDto>> getProducts(@Query("lite") String lite);

    @GET("products/search")
    Call<List<ProductDto>> searchProducts(
            @Query("q") String query,
            @Query("limit") int limit
    );

    @GET("products/{id}")
    Call<ProductDto> getProductById(@Path("id") String productId);

    @POST("products")
    Call<ProductDto> createProduct(@Body ProductDto product);
}
