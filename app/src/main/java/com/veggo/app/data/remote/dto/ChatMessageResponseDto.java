package com.veggo.app.data.remote.dto;

public class ChatMessageResponseDto {
    private boolean success;
    private ChatMessageDataDto data;

    public boolean isSuccess() {
        return success;
    }

    public ChatMessageDataDto getData() {
        return data;
    }
}
