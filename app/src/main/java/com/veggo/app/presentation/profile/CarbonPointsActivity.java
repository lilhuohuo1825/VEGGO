package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.veggo.app.R;
import com.veggo.app.adapter.BannerAdapter;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.utils.Constants;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.domain.model.Banner;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.promotion.PromotionDetailActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

public class CarbonPointsActivity extends BaseActivity {
    private BannerAdapter carbonPromotionAdapter;
    private View[] carbonPromotionIndicators;
    private View carbonPromotionBannerCard;
    private LinearLayout carbonPromotionIndicatorRow;
    private List<Banner> boundCertificateBanners = new ArrayList<>();
    private final Handler carbonPromotionHandler = new Handler(Looper.getMainLooper());
    private final Runnable carbonPromotionRunnable = () -> {
        ViewPager2 pager = findViewById(R.id.carbonPromotionBanners);
        if (pager != null && carbonPromotionAdapter != null && carbonPromotionAdapter.getRealCount() > 0) {
            pager.setCurrentItem(pager.getCurrentItem() + 1, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "điểm carbon")) {
            return;
        }
        setContentView(R.layout.activity_carbon_points);
        updateProgressFill(0f);
        findViewById(R.id.carbonBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.carbonHistoryButton).setOnClickListener(v ->
                startActivity(new Intent(this, CarbonHistoryActivity.class))
        );
        findViewById(R.id.carbonCertificateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CarbonCertificateActivity.class))
        );
        setupPromotionBannerCarousel();
        loadCarbonData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCarbonData();
        if (carbonPromotionAdapter != null && carbonPromotionAdapter.getRealCount() > 0) {
            carbonPromotionHandler.postDelayed(carbonPromotionRunnable, 4000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        carbonPromotionHandler.removeCallbacks(carbonPromotionRunnable);
    }

    private void loadCarbonData() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindCarbonData(snapshot));
        }).start();
    }

    private void bindCarbonData(AssetScreenData.Snapshot snapshot) {
        if (snapshot.user == null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentValue, "0 C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel, "Chưa đạt chứng nhận");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel, "");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue, "0 / 0 C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft, "");
            setStatusColors(R.color.neutral_70, R.color.neutral_70);
            updateProgressFill(0f);
            bindCertificateBanners(snapshot.certificates);
            return;
        }
        int points = snapshot.user.carbonPoint;
        AssetModels.Certificate current = findCertificateById(snapshot, snapshot.user.certificateId);
        AssetModels.Certificate eligible = findHighestEligibleCertificate(snapshot, points);
        AssetModels.Certificate next = findNextCertificate(snapshot, points);
        com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request = snapshot.latestCertificateRequest;

        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentValue, points + " C");

        if (current != null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel, current.certificateName);
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel,
                    next == null ? "Cao nhất" : next.certificateName);
            bindProgress(points, next);
            setStatusColors(R.color.primary_hover, next == null ? R.color.primary_hover : R.color.neutral_70);
        } else if (eligible != null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel,
                    eligibilityLabel(request, eligible));
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel, eligible.certificateName);
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue,
                    points + " / " + eligible.requiredCarbonPoint + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft,
                    eligibilityProgressText(request, eligible));
            setStatusColors(eligibilityColor(request, eligible), eligibilityColor(request, eligible));
            updateProgressFill(1f);
        } else {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel, "Chưa đạt chứng nhận");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel,
                    next == null ? "" : next.certificateName);
            bindProgress(points, next);
            setStatusColors(R.color.primary_hover, R.color.neutral_70);
        }
        bindCertificateBanners(snapshot.certificates);
    }

    private void setupPromotionBannerCarousel() {
        carbonPromotionBannerCard = findViewById(R.id.carbonPromotionBannerCard);
        carbonPromotionIndicatorRow = findViewById(R.id.carbonPromotionIndicators);
        carbonPromotionAdapter = new BannerAdapter();
        ViewPager2 pager = findViewById(R.id.carbonPromotionBanners);
        if (pager == null) return;

        carbonPromotionAdapter.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        pager.setAdapter(carbonPromotionAdapter);
        carbonPromotionAdapter.setOnBannerClickListener(banner -> {
            Intent intent = new Intent(this, PromotionDetailActivity.class);
            intent.putExtra(PromotionDetailActivity.EXTRA_PROMOTION_ID, banner.getId());
            startActivity(intent);
        });
        carbonPromotionIndicators = new View[]{
                findViewById(R.id.carbonPromotionIndicator1),
                findViewById(R.id.carbonPromotionIndicator2),
                findViewById(R.id.carbonPromotionIndicator3),
                findViewById(R.id.carbonPromotionIndicator4),
                findViewById(R.id.carbonPromotionIndicator5)
        };
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (carbonPromotionAdapter.getRealCount() > 0) {
                    updatePromotionIndicators(position % carbonPromotionAdapter.getRealCount());
                }
                carbonPromotionHandler.removeCallbacks(carbonPromotionRunnable);
                carbonPromotionHandler.postDelayed(carbonPromotionRunnable, 4000);
            }
        });
    }

    private void bindCertificateBanners(List<AssetModels.Certificate> certificates) {
        List<Banner> banners = new ArrayList<>();
        if (certificates != null) {
            for (AssetModels.Certificate certificate : certificates) {
                if (certificate == null || certificate.certificateId == null) continue;
                String promotionId = getPromotionIdForCertificate(certificate.certificateId);
                if (promotionId == null || promotionId.isEmpty()) continue;
                String imageUrl = buildPromotionBannerProxyUrl(promotionId);
                if (imageUrl == null || imageUrl.isEmpty()) continue;
                banners.add(new Banner(promotionId, 0, imageUrl));
            }
        }
        if (sameBanners(boundCertificateBanners, banners)) {
            return;
        }
        boundCertificateBanners = banners;
        preloadAndShowCertificateBanners(banners);
    }

    private boolean sameBanners(List<Banner> current, List<Banner> next) {
        if (current.size() != next.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            Banner left = current.get(i);
            Banner right = next.get(i);
            if (!left.getId().equals(right.getId())) {
                return false;
            }
            String leftUrl = left.getImageUrl() == null ? "" : left.getImageUrl();
            String rightUrl = right.getImageUrl() == null ? "" : right.getImageUrl();
            if (!leftUrl.equals(rightUrl)) {
                return false;
            }
        }
        return true;
    }

    private void preloadAndShowCertificateBanners(List<Banner> banners) {
        ViewPager2 pager = findViewById(R.id.carbonPromotionBanners);
        if (carbonPromotionAdapter == null || pager == null) {
            return;
        }

        carbonPromotionHandler.removeCallbacks(carbonPromotionRunnable);
        if (banners.isEmpty()) {
            carbonPromotionAdapter.submitList(new ArrayList<>());
            setCertificateBannerSectionVisible(false);
            return;
        }

        setCertificateBannerSectionVisible(false);

        Glide.with(this)
                .load(banners.get(0).getImageUrl())
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource
                    ) {
                        runOnUiThread(() -> showCertificateBanners(banners));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            android.graphics.drawable.Drawable resource,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        runOnUiThread(() -> showCertificateBanners(banners));
                        return false;
                    }
                })
                .preload();
    }

    private void showCertificateBanners(List<Banner> banners) {
        ViewPager2 pager = findViewById(R.id.carbonPromotionBanners);
        if (carbonPromotionAdapter == null || pager == null) {
            return;
        }

        carbonPromotionAdapter.submitList(banners);
        setCertificateBannerSectionVisible(true);
        pager.post(() -> {
            int realCount = carbonPromotionAdapter.getRealCount();
            if (realCount == 0) return;
            int startPos = (Integer.MAX_VALUE / 2) - ((Integer.MAX_VALUE / 2) % realCount);
            pager.setCurrentItem(startPos, false);
            updatePromotionIndicators(0);
            carbonPromotionHandler.removeCallbacks(carbonPromotionRunnable);
            carbonPromotionHandler.postDelayed(carbonPromotionRunnable, 4000);
        });
    }

    private void setCertificateBannerSectionVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (carbonPromotionBannerCard != null) {
            carbonPromotionBannerCard.setVisibility(visibility);
        }
        if (carbonPromotionIndicatorRow != null) {
            carbonPromotionIndicatorRow.setVisibility(visibility);
        }
    }

    private String getPromotionIdForCertificate(String certificateId) {
        if (certificateId == null) return null;
        switch (certificateId) {
            case "CER001": return "PROMO017";
            case "CER002": return "PROMO018";
            case "CER003": return "PROMO019";
            case "CER004": return "PROMO020";
            case "CER005": return "PROMO021";
            default: return null;
        }
    }

    private void updatePromotionIndicators(int position) {
        if (carbonPromotionIndicators == null || carbonPromotionAdapter == null) return;
        int realCount = carbonPromotionAdapter.getRealCount();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < carbonPromotionIndicators.length; i++) {
            View indicator = carbonPromotionIndicators[i];
            if (indicator == null) continue;
            indicator.setVisibility(i < realCount ? View.VISIBLE : View.GONE);
            if (i >= realCount) continue;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) indicator.getLayoutParams();
            if (i == position) {
                indicator.setBackgroundResource(R.drawable.bg_indicator_selected);
                params.width = (int) (14 * density);
                params.height = (int) (4 * density);
            } else {
                indicator.setBackgroundResource(R.drawable.bg_indicator_unselected);
                params.width = (int) (4 * density);
                params.height = (int) (4 * density);
            }
            indicator.setLayoutParams(params);
        }
    }

    private String buildPromotionBannerProxyUrl(String promotionId) {
        if (promotionId == null || promotionId.trim().isEmpty()) return null;
        return removeTrailingSlash(Constants.API_BASE_URL) + "/promotions/" + promotionId.trim() + "/banner-image";
    }

    private String removeTrailingSlash(String value) {
        if (value == null) return "";
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private void bindProgress(int points, AssetModels.Certificate target) {
        if (target == null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue, points + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft, "Đã đạt cấp cao nhất");
            updateProgressFill(1f);
        } else {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue,
                    points + " / " + target.requiredCarbonPoint + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft,
                    "Còn " + Math.max(0, target.requiredCarbonPoint - points) + " C");
            updateProgressFill(progressRatio(points, target.requiredCarbonPoint));
        }
    }

    private String eligibilityLabel(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        if (isRequestFor(request, certificate)) {
            if ("pending".equalsIgnoreCase(request.status)) {
                return "Chờ duyệt " + certificate.certificateName;
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return "Đã từ chối duyệt";
            }
        }
        return "Đủ điều kiện " + certificate.certificateName;
    }

    private String eligibilityProgressText(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        if (isRequestFor(request, certificate)) {
            if ("pending".equalsIgnoreCase(request.status)) {
                return "Đang chờ duyệt";
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return "Đã từ chối duyệt";
            }
        }
        return "Đủ điểm, chờ gửi duyệt";
    }

    private int eligibilityColor(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        if (isRequestFor(request, certificate)) {
            if ("pending".equalsIgnoreCase(request.status)) {
                return R.color.secondary_hover;
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return R.color.danger_main;
            }
        }
        return R.color.info_main;
    }

    private void setStatusColors(int currentLevelColorResId, int progressStatusColorResId) {
        TextView currentLevel = findViewById(R.id.carbonCurrentLevel);
        TextView progressLeft = findViewById(R.id.carbonProgressLeft);
        if (currentLevel != null) {
            currentLevel.setTextColor(getColor(currentLevelColorResId));
        }
        if (progressLeft != null) {
            progressLeft.setTextColor(getColor(progressStatusColorResId));
        }
    }

    private float progressRatio(int points, int targetPoints) {
        if (targetPoints <= 0) return 0f;
        return Math.max(0f, Math.min(1f, points / (float) targetPoints));
    }

    private void updateProgressFill(float ratio) {
        View track = findViewById(R.id.carbonProgressTrack);
        View fill = findViewById(R.id.carbonProgressFill);
        if (track == null || fill == null) return;

        float clampedRatio = Math.max(0f, Math.min(1f, ratio));
        track.post(() -> {
            ViewGroup.LayoutParams params = fill.getLayoutParams();
            params.width = Math.round(track.getWidth() * clampedRatio);
            fill.setLayoutParams(params);
        });
    }

    private AssetModels.Certificate findCertificateById(AssetScreenData.Snapshot snapshot, String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) return null;
        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (certificateId.equals(certificate.certificateId)) {
                return certificate;
            }
        }
        return null;
    }

    private AssetModels.Certificate findHighestEligibleCertificate(AssetScreenData.Snapshot snapshot, int points) {
        AssetModels.Certificate eligible = null;
        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (points >= certificate.requiredCarbonPoint) {
                eligible = certificate;
            }
        }
        return eligible;
    }

    private AssetModels.Certificate findNextCertificate(AssetScreenData.Snapshot snapshot, int points) {
        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (points < certificate.requiredCarbonPoint) {
                return certificate;
            }
        }
        return null;
    }

    private boolean isRequestFor(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        return request != null
                && certificate != null
                && request.getRequestedCertificateId() != null
                && request.getRequestedCertificateId().equals(certificate.certificateId);
    }

}
