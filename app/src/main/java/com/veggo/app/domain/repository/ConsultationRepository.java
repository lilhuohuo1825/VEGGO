package com.veggo.app.domain.repository;

import com.veggo.app.domain.model.Consultation;

import java.util.List;

public interface ConsultationRepository {
    interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    void getConsultationsBySku(String sku, Callback<List<Consultation>> callback);

    void submitQuestion(String sku, String question, String customerName, String productName,
                        Callback<List<Consultation>> callback);
}
