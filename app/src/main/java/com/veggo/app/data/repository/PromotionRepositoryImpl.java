package com.veggo.app.data.repository;

import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.domain.repository.PromotionRepository;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class PromotionRepositoryImpl implements PromotionRepository {
    private final PromotionApi promotionApi;

    @Inject
    public PromotionRepositoryImpl(PromotionApi promotionApi) {
        this.promotionApi = promotionApi;
    }

    @Override
    public Call<List<PromotionDto>> getAllPromotions() {
        return promotionApi.getAllPromotions();
    }

    @Override
    public Call<PromotionDto> getPromotionByCode(String code) {
        return promotionApi.getPromotionByCode(code);
    }
}
