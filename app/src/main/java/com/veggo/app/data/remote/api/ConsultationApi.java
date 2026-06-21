package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ConsultationAskRequest;
import com.veggo.app.data.remote.dto.ConsultationDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ConsultationApi {
    @GET("consultations/{sku}")
    Call<ConsultationDto> getConsultationBySku(@Path("sku") String sku);

    @POST("consultations/{sku}/questions")
    Call<ConsultationDto> askQuestion(
        @Path("sku") String sku,
        @Body ConsultationAskRequest request
    );
}
