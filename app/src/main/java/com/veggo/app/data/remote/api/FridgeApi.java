package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.AiRecognitionRequestDto;
import com.veggo.app.data.remote.dto.AiRecognitionResponseDto;
import com.veggo.app.data.remote.dto.FridgeBatchRequestDto;
import com.veggo.app.data.remote.dto.FridgeLocationDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FridgeApi {
    @GET("fridge/{userId}")
    Call<List<com.veggo.app.data.remote.dto.FridgeItemDto>> getFridgeItems(@Path("userId") String userId);

    @POST("fridge/{userId}/batch")
    Call<List<Map<String, Object>>> addBatchItems(
            @Path("userId") String userId,
            @Body FridgeBatchRequestDto body
    );

    @POST("fridge/{userId}")
    Call<Map<String, Object>> addSingleItem(
            @Path("userId") String userId,
            @Body FridgeBatchRequestDto.FridgeItemRequestDto body
    );

    @POST("fridge/ai/recognize")
    Call<List<com.veggo.app.data.remote.dto.AiRecognitionItemDto>> recognizeIngredient(
            @Body AiRecognitionRequestDto request
    );

    @GET("fridge/{userId}/locations")
    Call<List<FridgeLocationDto>> getLocations(
            @Path("userId") String userId
    );

    @POST("fridge/{userId}/locations")
    Call<FridgeLocationDto> addLocation(
            @Path("userId") String userId,
            @Body FridgeLocationDto body
    );

    @retrofit2.http.PUT("fridge/{userId}/{itemId}")
    Call<com.veggo.app.data.remote.dto.FridgeItemDto> updateFridgeItem(
            @Path("userId") String userId,
            @Path("itemId") String itemId,
            @Body com.veggo.app.data.remote.dto.FridgeItemDto body
    );

    @retrofit2.http.DELETE("fridge/{userId}/{itemId}")
    Call<Void> deleteFridgeItem(
            @Path("userId") String userId,
            @Path("itemId") String itemId
    );
}
