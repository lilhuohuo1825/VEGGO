package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.remote.api.ChatApi;
import com.veggo.app.data.remote.dto.ChatMessageDataDto;
import com.veggo.app.data.remote.dto.ChatMessageRequestDto;
import com.veggo.app.data.remote.dto.ChatMessageResponseDto;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {
    public interface ResultCallback<T> {
        void onSuccess(T result);

        void onError(Throwable error);
    }

    private final ChatApi chatApi;

    public ChatRepository(ChatApi chatApi) {
        this.chatApi = chatApi;
    }

    public void sendMessage(
            String customerId,
            String message,
            String conversationId,
            ResultCallback<ChatMessageDataDto> callback
    ) {
        ChatMessageRequestDto request = new ChatMessageRequestDto(customerId, message, conversationId);

        chatApi.sendMessage(request).enqueue(new Callback<ChatMessageResponseDto>() {
            @Override
            public void onResponse(Call<ChatMessageResponseDto> call, Response<ChatMessageResponseDto> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }

                callback.onError(new ApiHttpException(
                        response.code(),
                        parseErrorMessage(response)
                ));
            }

            @Override
            public void onFailure(Call<ChatMessageResponseDto> call, Throwable t) {
                callback.onError(new IOException(mapNetworkError(t), t));
            }
        });
    }

    private static String parseErrorMessage(Response<ChatMessageResponseDto> response) {
        String fallback = "Không thể gửi tin nhắn tới chatbot";
        if (response.code() == 503) {
            fallback = "Dịch vụ AI chưa sẵn sàng. Vui lòng thử lại sau.";
        }

        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            String raw = response.errorBody().string();
            JSONObject json = new JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            return raw;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static String mapNetworkError(Throwable error) {
        if (error instanceof SocketTimeoutException) {
            return "Kết nối quá thời gian. Kiểm tra backend đang chạy và IP trong local.properties.";
        }
        if (error instanceof ConnectException || error instanceof UnknownHostException) {
            return "Không kết nối được máy chủ tại:\n" + com.veggo.app.core.utils.Constants.API_BASE_URL + "\n\n"
                    + "Hãy kiểm tra:\n"
                    + "• Backend đang chạy: cd backend && npm run start\n"
                    + "• Điện thoại và máy tính cùng Wi-Fi\n"
                    + "• Mở trình duyệt điện thoại: http://<IP-máy-tính>:5001/api/health\n"
                    + "• Cập nhật api.base.url trong local.properties rồi Rebuild app";
        }
        if (error != null && error.getMessage() != null && !error.getMessage().trim().isEmpty()) {
            return error.getMessage();
        }
        return "Không thể kết nối tới máy chủ. Vui lòng thử lại.";
    }
}
