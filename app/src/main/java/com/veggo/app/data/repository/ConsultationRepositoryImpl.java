package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.mapper.ConsultationMapper;
import com.veggo.app.data.remote.api.ConsultationApi;
import com.veggo.app.data.remote.dto.ConsultationAskRequest;
import com.veggo.app.data.remote.dto.ConsultationDto;
import com.veggo.app.data.remote.dto.ConsultationLikeRequest;
import com.veggo.app.data.remote.dto.ConsultationReplyRequest;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.repository.ConsultationRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsultationRepositoryImpl implements ConsultationRepository {
    private final ConsultationApi consultationApi;

    public ConsultationRepositoryImpl(ConsultationApi consultationApi) {
        this.consultationApi = consultationApi;
    }

    @Override
    public void getConsultationsBySku(String sku, Callback<List<Consultation>> callback) {
        consultationApi.getConsultationBySku(sku).enqueue(handleConsultationResponse(callback));
    }

    @Override
    public void submitQuestion(String sku, String question, String customerId, String customerName,
                               String productName, String customerAvatarUrl,
                               Callback<List<Consultation>> callback) {
        ConsultationAskRequest request = new ConsultationAskRequest(
                question, customerId, customerName, productName, customerAvatarUrl);
        consultationApi.askQuestion(sku, request).enqueue(handleConsultationResponse(callback, true));
    }

    @Override
    public void toggleQuestionLike(String sku, String questionId, String customerId, String customerName,
                                   Callback<List<Consultation>> callback) {
        ConsultationLikeRequest request = new ConsultationLikeRequest(customerId, customerName);
        consultationApi.toggleQuestionLike(sku, questionId, request)
                .enqueue(handleConsultationResponse(callback, true));
    }

    @Override
    public void submitReply(String sku, String questionId, String content, String customerId,
                          String customerName, String customerAvatarUrl,
                          Callback<List<Consultation>> callback) {
        ConsultationReplyRequest request = new ConsultationReplyRequest(
                content, customerId, customerName, customerAvatarUrl);
        consultationApi.submitReply(sku, questionId, request)
                .enqueue(handleConsultationResponse(callback, true));
    }

    private retrofit2.Callback<ConsultationDto> handleConsultationResponse(
            Callback<List<Consultation>> callback) {
        return handleConsultationResponse(callback, false);
    }

    private retrofit2.Callback<ConsultationDto> handleConsultationResponse(
            Callback<List<Consultation>> callback, boolean parseErrorBody) {
        return new retrofit2.Callback<ConsultationDto>() {
            @Override
            public void onResponse(Call<ConsultationDto> call, Response<ConsultationDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(ConsultationMapper.fromDto(response.body()));
                    return;
                }
                if (response.code() == 404 && !parseErrorBody) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), readErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<ConsultationDto> call, Throwable t) {
                callback.onError(t);
            }
        };
    }

    private String readErrorMessage(Response<ConsultationDto> response) {
        if (response.errorBody() != null) {
            try {
                return response.errorBody().string();
            } catch (Exception ignored) {
            }
        }
        return "Consultation request failed";
    }
}
