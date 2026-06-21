package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class CarbonPointsActivity extends BaseActivity {
    private static final int PROGRESS_SEGMENT_COUNT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_points);
        setupProgressSegments();
        findViewById(R.id.carbonBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.carbonHistoryButton).setOnClickListener(v ->
                startActivity(new Intent(this, CarbonHistoryActivity.class))
        );
        findViewById(R.id.carbonCertificateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CarbonCertificateActivity.class))
        );
        loadCarbonData();
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
            updateProgressFill(0f);
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
        } else if (eligible != null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel,
                    eligibilityLabel(request, eligible));
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel, eligible.certificateName);
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue,
                    points + " / " + eligible.requiredCarbonPoint + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft,
                    eligibilityProgressText(request, eligible));
            updateProgressFill(1f);
        } else {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel, "Chưa đạt chứng nhận");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel,
                    next == null ? "" : next.certificateName);
            bindProgress(points, next);
        }
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
                return "Bị từ chối " + certificate.certificateName;
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
                return "Đang chờ quản trị viên duyệt";
            }
            if ("rejected".equalsIgnoreCase(request.status)) {
                return AssetScreenData.hasText(request.rejectReason)
                        ? "Đã từ chối: " + request.rejectReason
                        : "Yêu cầu đã bị từ chối";
            }
        }
        return "Đủ điểm, chờ gửi duyệt";
    }

    private void setupProgressSegments() {
        LinearLayout container = findViewById(R.id.carbonProgressSegments);
        if (container == null) return;

        container.removeAllViews();
        int gap = dp(3);
        for (int i = 0; i < PROGRESS_SEGMENT_COUNT; i++) {
            View segment = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
            );
            if (i > 0) {
                params.setMarginStart(gap);
            }
            segment.setLayoutParams(params);
            container.addView(segment);
        }
    }

    private float progressRatio(int points, int targetPoints) {
        if (targetPoints <= 0) return 0f;
        return Math.max(0f, Math.min(1f, points / (float) targetPoints));
    }

    private void updateProgressFill(float ratio) {
        LinearLayout container = findViewById(R.id.carbonProgressSegments);
        if (container == null) return;

        int filledSegments = ratio >= 1f
                ? PROGRESS_SEGMENT_COUNT
                : (int) Math.floor(ratio * PROGRESS_SEGMENT_COUNT);
        for (int i = 0; i < container.getChildCount(); i++) {
            View segment = container.getChildAt(i);
            segment.setBackgroundResource(i < filledSegments
                    ? R.drawable.bg_carbon_progress_fill
                    : 0);
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
