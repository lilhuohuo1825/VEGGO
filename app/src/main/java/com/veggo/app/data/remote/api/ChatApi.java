package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ChatMessageRequestDto;
import com.veggo.app.data.remote.dto.ChatMessageResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatApi {
    @POST("chat/message")
    Call<ChatMessageResponseDto> sendMessage(@Body ChatMessageRequestDto request);
}
