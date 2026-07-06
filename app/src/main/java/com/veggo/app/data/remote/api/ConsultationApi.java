package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.ConsultationAskRequest;
import com.veggo.app.data.remote.dto.ConsultationDto;
import com.veggo.app.data.remote.dto.ConsultationLikeRequest;
import com.veggo.app.data.remote.dto.ConsultationReplyRequest;

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

    @POST("consultations/{sku}/questions/{questionId}/like")
    Call<ConsultationDto> toggleQuestionLike(
            @Path("sku") String sku,
            @Path("questionId") String questionId,
            @Body ConsultationLikeRequest request
    );

    @POST("consultations/{sku}/questions/{questionId}/replies")
    Call<ConsultationDto> submitReply(
            @Path("sku") String sku,
            @Path("questionId") String questionId,
            @Body ConsultationReplyRequest request
    );
}
