package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.CategoryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CategoryApi {
    @GET("categories")
    Call<List<CategoryDto>> getCategories();
}
