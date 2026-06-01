package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ProductDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ProductApi {
    @GET("products")
    Call<List<ProductDto>> getProducts();

    @GET("products/{id}")
    Call<ProductDto> getProductById(@Path("id") String productId);

    @POST("products")
    Call<ProductDto> createProduct(@Body ProductDto product);
}
