package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.mapper.ConsultationMapper;
import com.veggo.app.data.remote.api.ConsultationApi;
import com.veggo.app.data.remote.dto.ConsultationAskRequest;
import com.veggo.app.data.remote.dto.ConsultationDto;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.repository.ConsultationRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ConsultationRepositoryImpl implements ConsultationRepository {
    private final ConsultationApi consultationApi;

    public ConsultationRepositoryImpl(ConsultationApi consultationApi) {
        this.consultationApi = consultationApi;
    }

    @Override
    public void getConsultationsBySku(String sku, Callback<List<Consultation>> callback) {
        consultationApi.getConsultationBySku(sku).enqueue(new retrofit2.Callback<ConsultationDto>() {
            @Override
            public void onResponse(Call<ConsultationDto> call, Response<ConsultationDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(ConsultationMapper.fromDto(response.body()));
                } else if (response.code() == 404) {
                    callback.onSuccess(new ArrayList<>());
                } else {
                    callback.onError(new ApiHttpException(
                            response.code(), "Failed to fetch consultations"));
                }
            }

            @Override
            public void onFailure(Call<ConsultationDto> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    @Override
    public void submitQuestion(String sku, String question, String customerId, String customerName, String productName,
                               Callback<List<Consultation>> callback) {
        ConsultationAskRequest request = new ConsultationAskRequest(question, customerId, customerName, productName);

        consultationApi.askQuestion(sku, request).enqueue(new retrofit2.Callback<ConsultationDto>() {
            @Override
            public void onResponse(Call<ConsultationDto> call, Response<ConsultationDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(ConsultationMapper.fromDto(response.body()));
                    return;
                }

                String message = "Failed to submit question";
                if (response.errorBody() != null) {
                    try {
                        message = response.errorBody().string();
                    } catch (Exception ignored) {
                    }
                }
                callback.onError(new ApiHttpException(response.code(), message));
            }

            @Override
            public void onFailure(Call<ConsultationDto> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
}
