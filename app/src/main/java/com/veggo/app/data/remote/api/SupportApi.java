package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.SupportConversationsResponseDto;
import com.veggo.app.data.remote.dto.SupportMessagesResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SupportApi {
    @GET("support/conversations")
    Call<SupportConversationsResponseDto> getConversations(@Query("customerId") String customerId);

    @GET("support/conversations/{id}/messages")
    Call<SupportMessagesResponseDto> getMessages(
            @Path("id") String conversationId,
            @Query("before") String before,
            @Query("limit") int limit
    );

    @POST("support/conversations/{id}/mark-read")
    Call<SupportConversationsResponseDto> markConversationRead(@Path("id") String conversationId);
}

