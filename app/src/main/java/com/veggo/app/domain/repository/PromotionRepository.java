package com.veggo.app.domain.repository;

import androidx.lifecycle.LiveData;

import com.veggo.app.domain.model.Banner;

import java.util.List;

public interface PromotionRepository {
    LiveData<List<Banner>> getAppBanners();

    void refreshBanners();
}
