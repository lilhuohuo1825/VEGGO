package com.veggo.app.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.utils.Constants;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.domain.model.Banner;
import com.veggo.app.domain.repository.PromotionRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromotionRepositoryImpl implements PromotionRepository {
    private final PromotionApi promotionApi;
    private final MutableLiveData<List<Banner>> banners = new MutableLiveData<>(new ArrayList<>());

    public PromotionRepositoryImpl() {
        promotionApi = ApiClient.createService(PromotionApi.class);
    }

    @Override
    public LiveData<List<Banner>> getAppBanners() {
        return banners;
    }

    @Override
    public void refreshBanners() {
        promotionApi.getPromotions().enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PromotionDto>> call, @NonNull Response<List<PromotionDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    postDefaultBanner();
                    return;
                }

                banners.postValue(mapToBanners(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<List<PromotionDto>> call, @NonNull Throwable throwable) {
                postDefaultBanner();
            }
        });
    }

    private void postDefaultBanner() {
        List<Banner> defaultList = new ArrayList<>();
        defaultList.add(new Banner("default_1", com.veggo.app.R.drawable.banner_freeship, null));
        banners.postValue(defaultList);
    }

    private List<Banner> mapToBanners(List<PromotionDto> promotions) {
        List<Banner> result = new ArrayList<>();
        if (promotions == null) return result;

        for (PromotionDto promotion : promotions) {
            // Nới lỏng điều kiện: Chỉ cần có ảnh và không bị tắt (isActive != false)
            if (promotion == null || (promotion.getActive() != null && !promotion.getActive())) {
                continue;
            }

            String imageUrl = getPromotionImageUrl(promotion);
            String fullImageUrl = buildFullImageUrl(imageUrl);
            
            if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
                result.add(new Banner(getBannerId(promotion), 0, fullImageUrl));
            }
        }

        // Nếu API không trả về banner nào, thêm banner mặc định để tránh trống màn hình
        if (result.isEmpty()) {
            result.add(new Banner("default_1", com.veggo.app.R.drawable.banner_freeship, null));
        }

        return result;
    }

    private boolean isHomeBannerPromotion(String promotionId) {
        if (promotionId == null) {
            return false;
        }

        switch (promotionId) {
            case "PROMO010":
            case "PROMO011":
            case "PROMO012":
            case "PROMO013":
            case "PROMO014":
                return true;
            default:
                return false;
        }
    }

    private String getPromotionImageUrl(PromotionDto promotion) {
        PromotionDto.BannerDataDto bannerData = promotion.getBannerData();
        if (bannerData != null) {
            if (bannerData.getSrc() != null && !bannerData.getSrc().isEmpty()) {
                return bannerData.getSrc(); // Ưu tiên dùng trường "src" nếu có
            }
            if (bannerData.getImageUrl() != null && !bannerData.getImageUrl().isEmpty()) {
                return bannerData.getImageUrl();
            }
        }
        return promotion.getImageUrl();
    }

    private String getBannerId(PromotionDto promotion) {
        if (promotion.getPromotionId() != null && !promotion.getPromotionId().isEmpty()) {
            return promotion.getPromotionId();
        }
        String id = promotion.getId();
        if (id != null && !id.isEmpty()) {
            return id;
        }
        return String.valueOf(System.nanoTime());
    }

    private String buildFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        String trimmedUrl = imageUrl.trim();
        String baseUrl = Constants.API_BASE_URL; // Ví dụ: http://10.158.43.138:5001/api/
        
        // Trích xuất IP:Port từ Constants để thay thế localhost
        String serverRootWithIp = "";
        try {
            String[] parts = baseUrl.split("//");
            if (parts.length > 1) {
                serverRootWithIp = parts[0] + "//" + parts[1].split("/")[0];
            }
        } catch (Exception e) {
            serverRootWithIp = removeTrailingSlash(baseUrl);
        }

        // Nếu link đã là URL đầy đủ
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            // Thay thế localhost bằng IP thật của server
            if (trimmedUrl.contains("localhost") || trimmedUrl.contains("127.0.0.1")) {
                return trimmedUrl.replace("localhost:5001", serverRootWithIp.replace("http://", "").replace("https://", ""))
                                 .replace("127.0.0.1:5001", serverRootWithIp.replace("http://", "").replace("https://", ""))
                                 .replace("localhost", serverRootWithIp.replace("http://", "").replace("https://", ""))
                                 .replace("127.0.0.1", serverRootWithIp.replace("http://", "").replace("https://", ""));
            }
            return trimmedUrl;
        }

        // Nếu là path tương đối (/api/...)
        String apiPrefix = "/api/";
        int apiIndex = baseUrl.indexOf(apiPrefix);
        String serverRoot = apiIndex >= 0 ? baseUrl.substring(0, apiIndex) : removeTrailingSlash(baseUrl);

        if (trimmedUrl.startsWith("/")) {
            return serverRoot + trimmedUrl;
        }

        return removeTrailingSlash(baseUrl) + "/" + trimmedUrl;
    }

    private String removeTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
