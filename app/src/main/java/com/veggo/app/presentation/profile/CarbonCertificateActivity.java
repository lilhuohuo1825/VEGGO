package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;
import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CarbonCertificateActivity extends BaseActivity {
    private static final NumberFormat POINT_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "điểm carbon")) {
            return;
        }
        setContentView(R.layout.activity_carbon_certificate);
        findViewById(R.id.carbonCertificateBackButton).setOnClickListener(v -> finish());
        loadCertificate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCertificate();
    }

    private void loadCertificate() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindCertificate(snapshot));
        }).start();
    }

    private void bindCertificate(AssetScreenData.Snapshot snapshot) {
        int points = snapshot.user == null ? 0 : snapshot.user.carbonPoint;
        AssetModels.Certificate current = findCertificateById(snapshot.certificates,
                snapshot.user == null ? null : snapshot.user.certificateId);
        AssetModels.Certificate eligible = findHighestEligibleCertificate(snapshot.certificates, points);
        AssetModels.Certificate next = findNextCertificate(snapshot.certificates, points);
        com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request = snapshot.latestCertificateRequest;

        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificatePoints,
                formatPoints(points) + " C");

        View placeholder = findViewById(R.id.myCertificatePlaceholder);
        View content = findViewById(R.id.myCertificateContent);
        ImageView banner = findViewById(R.id.myCertificateBanner);
        TextView rewardDetails = findViewById(R.id.myCertificateRewardDetails);
        TextView statusView = findViewById(R.id.carbonCertificateStatus);
        TextView descView = findViewById(R.id.carbonCertificateDescription);

        if (current != null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName, current.certificateName);
            if (statusView != null) statusView.setVisibility(View.GONE);
            if (descView != null) descView.setVisibility(View.GONE);
            View shareBtn = findViewById(R.id.carbonShareButton);
            if (shareBtn != null) {
                shareBtn.setVisibility(View.VISIBLE);
            }

            if (placeholder != null) placeholder.setVisibility(View.GONE);
            if (content != null) content.setVisibility(View.VISIBLE);
            if (banner != null) {
                String promoId = getPromotionIdForCertificate(current.certificateId);
                String bannerUrl = buildPromotionBannerProxyUrl(promoId);
                if (bannerUrl != null) {
                    Glide.with(this)
                            .load(bannerUrl)
                            .fitCenter()
                            .placeholder(R.drawable.banner_nam_rom)
                            .error(R.drawable.banner_nam_rom)
                            .into(banner);
                }
            }
            if (rewardDetails != null) {
                rewardDetails.setText("• Chi tiết ưu đãi: " + current.certificateDescription + "\n• Quà tặng đi kèm: " + current.rewardDescription);
            }
        } else {
            if (placeholder != null) placeholder.setVisibility(View.VISIBLE);
            if (content != null) content.setVisibility(View.GONE);
            if (statusView != null) statusView.setVisibility(View.VISIBLE);
            if (descView != null) descView.setVisibility(View.VISIBLE);

            if (eligible != null) {
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName,
                        eligibilityTitle(request, eligible));
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateStatus,
                        eligibilityStatus(request, eligible));
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateDescription,
                        eligibilityDescription(request, eligible, points));
                View shareBtn = findViewById(R.id.carbonShareButton);
                if (shareBtn != null) {
                    shareBtn.setVisibility(View.GONE);
                }
            } else {
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName, "Chưa có chứng nhận");
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateStatus,
                        next == null ? "Chưa có dữ liệu mốc chứng nhận" : "Cần thêm " + formatPoints(next.requiredCarbonPoint - points) + " C");
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateDescription,
                        next == null
                                ? "Danh sách chứng nhận chưa sẵn sàng. Vui lòng thử lại sau."
                                : "Tích lũy điểm carbon từ các đơn hàng xanh để mở mốc " + next.certificateName + ".");
                View shareBtn = findViewById(R.id.carbonShareButton);
                if (shareBtn != null) {
                    shareBtn.setVisibility(View.GONE);
                }
            }
        }

        bindCertificatePath(snapshot.certificates, current, eligible, request, points);
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

    private String buildPromotionBannerProxyUrl(String promotionId) {
        if (promotionId == null || promotionId.trim().isEmpty()) return null;
        String baseUrl = com.veggo.app.core.utils.Constants.API_BASE_URL;
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/promotions/" + promotionId.trim() + "/banner-image";
    }

    private void bindCertificatePath(List<AssetModels.Certificate> certificates,
                                     AssetModels.Certificate current,
                                     AssetModels.Certificate eligible,
                                     com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
                                     int points) {
        LinearLayout container = findViewById(R.id.carbonCertificatePathList);
        if (container == null) return;

        container.removeAllViews();
        for (int i = 0; i < certificates.size(); i++) {
            AssetModels.Certificate certificate = certificates.get(i);
            container.addView(createCertificateRow(certificate, current, eligible, request, points));
            if (i < certificates.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
                dividerParams.setMargins(dp(56), 0, 0, 0);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.neutral_30));
                container.addView(divider);
            }
        }
    }

    private View createCertificateRow(AssetModels.Certificate certificate,
                                      AssetModels.Certificate current,
                                      AssetModels.Certificate eligible,
                                      com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
                                      int points) {
        boolean approved = sameCertificate(current, certificate);
        boolean waiting = current == null
                && sameCertificate(eligible, certificate)
                && isRequestFor(request, certificate)
                && "pending".equalsIgnoreCase(request.status);
        boolean rejected = current == null
                && sameCertificate(eligible, certificate)
                && isRequestFor(request, certificate)
                && "rejected".equalsIgnoreCase(request.status);
        boolean reached = points >= certificate.requiredCarbonPoint;
        int activeColor = ContextCompat.getColor(this, R.color.primary_hover);
        int pendingColor = ContextCompat.getColor(this, R.color.secondary_hover);
        int rejectedColor = ContextCompat.getColor(this, R.color.danger_main);
        int normalColor = ContextCompat.getColor(this, R.color.neutral_80);
        int mutedColor = ContextCompat.getColor(this, R.color.neutral_60);
        int textColor = rejected ? rejectedColor : waiting ? pendingColor : (approved || reached) ? activeColor : normalColor;

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(76));
        row.setPadding(0, dp(10), 0, dp(10));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout iconFrame = new FrameLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconFrame.setLayoutParams(iconParams);
        iconFrame.setBackgroundResource(R.drawable.bg_carbon_icon_circle);

        ImageView icon = new ImageView(this);
        FrameLayout.LayoutParams leafParams = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        icon.setLayoutParams(leafParams);
        icon.setImageResource(R.drawable.ic_profile_leaf);
        icon.setColorFilter(textColor);
        iconFrame.addView(icon);
        row.addView(iconFrame);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        contentParams.setMargins(dp(16), 0, dp(12), 0);
        content.setLayoutParams(contentParams);

        TextView name = new TextView(this);
        name.setText(certificate.certificateName);
        name.setTextColor(textColor);
        name.setTextSize(16);
        name.setTypeface(Typeface.DEFAULT, (approved || waiting) ? Typeface.BOLD : Typeface.NORMAL);
        content.addView(name);

        TextView reward = new TextView(this);
        reward.setText(certificate.rewardDescription);
        reward.setTextColor(mutedColor);
        reward.setTextSize(12);
        reward.setMaxLines(2);
        LinearLayout.LayoutParams rewardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rewardParams.setMargins(0, dp(3), 0, 0);
        reward.setLayoutParams(rewardParams);
        content.addView(reward);
        row.addView(content);

        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.END);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setLayoutParams(new LinearLayout.LayoutParams(dp(108), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView point = new TextView(this);
        point.setGravity(Gravity.END);
        point.setText(formatPoints(certificate.requiredCarbonPoint) + " C");
        point.setTextColor(textColor);
        point.setTextSize(15);
        point.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        meta.addView(point);

        TextView status = new TextView(this);
        status.setGravity(Gravity.END);
        status.setText(statusLabel(approved, waiting, rejected, reached));
        status.setTextColor(rejected ? rejectedColor : waiting ? pendingColor : approved ? activeColor : mutedColor);
        status.setTextSize(12);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, dp(3), 0, 0);
        status.setLayoutParams(statusParams);
        meta.addView(status);
        row.addView(meta);

        return row;
    }

    private String statusLabel(boolean approved, boolean waiting, boolean rejected, boolean reached) {
        if (approved) return "Đã duyệt";
        if (waiting) return "Chờ duyệt";
        if (rejected) return "Đã từ chối duyệt";
        if (reached) return "Đủ điểm";
        return "Chưa đạt";
    }

    private String eligibilityTitle(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        if (isRequestFor(request, certificate) && "rejected".equalsIgnoreCase(request.status)) {
            return "Bị từ chối " + certificate.certificateName;
        }
        return "Đủ điều kiện " + certificate.certificateName;
    }

    private String eligibilityStatus(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate
    ) {
        if (isRequestFor(request, certificate)) {
            if ("pending".equalsIgnoreCase(request.status)) {
                return "Chờ quản trị viên duyệt";
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return "Yêu cầu đã bị từ chối";
            }
        }
        return "Đủ điểm, chờ gửi duyệt";
    }

    private String eligibilityDescription(
            com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto request,
            AssetModels.Certificate certificate,
            int points
    ) {
        if (isRequestFor(request, certificate)) {
            if ("pending".equalsIgnoreCase(request.status)) {
                return "Bạn đã đạt " + formatPoints(points) + " C, đủ mốc "
                        + formatPoints(certificate.requiredCarbonPoint)
                        + " C. Chứng nhận sẽ hiển thị sau khi quản trị viên duyệt.";
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return AssetScreenData.hasText(request.rejectReason)
                        ? "Lý do từ chối: " + request.rejectReason
                        : "Yêu cầu chứng nhận đã bị từ chối. Khi điểm carbon tăng thêm, hệ thống có thể tạo yêu cầu mới.";
            }
        }
        return "Bạn đã đạt " + formatPoints(points) + " C, đủ mốc "
                + formatPoints(certificate.requiredCarbonPoint)
                + " C. Vui lòng chờ hệ thống/admin quét đơn đã giao để gửi duyệt.";
    }

    private AssetModels.Certificate findCertificateById(List<AssetModels.Certificate> certificates, String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) return null;
        for (AssetModels.Certificate certificate : certificates) {
            if (certificateId.equals(certificate.certificateId)) {
                return certificate;
            }
        }
        return null;
    }

    private AssetModels.Certificate findHighestEligibleCertificate(List<AssetModels.Certificate> certificates, int points) {
        AssetModels.Certificate eligible = null;
        for (AssetModels.Certificate certificate : certificates) {
            if (points >= certificate.requiredCarbonPoint) {
                eligible = certificate;
            }
        }
        return eligible;
    }

    private AssetModels.Certificate findNextCertificate(List<AssetModels.Certificate> certificates, int points) {
        for (AssetModels.Certificate certificate : certificates) {
            if (points < certificate.requiredCarbonPoint) {
                return certificate;
            }
        }
        return null;
    }

    private boolean sameCertificate(AssetModels.Certificate left, AssetModels.Certificate right) {
        return left != null
                && right != null
                && left.certificateId != null
                && left.certificateId.equals(right.certificateId);
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

    private String formatPoints(int points) {
        return POINT_FORMAT.format(points);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
