package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.RecipeDetailDto;
import com.veggo.app.data.remote.dto.RelatedRecipeDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecipeApi {
    @GET("recipes/related")
    Call<List<RelatedRecipeDto>> getRelatedRecipes(@Query("productName") String productName);

    @GET("recipes/{instructionId}")
    Call<RecipeDetailDto> getRecipeDetail(@Path("instructionId") String instructionId);
}
