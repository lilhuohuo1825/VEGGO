package com.veggo.app.domain.repository;

import com.veggo.app.data.remote.dto.PromotionDto;
import java.util.List;
import retrofit2.Call;

public interface PromotionRepository {
    Call<List<PromotionDto>> getAllPromotions();
    Call<PromotionDto> getPromotionByCode(String code);
}
