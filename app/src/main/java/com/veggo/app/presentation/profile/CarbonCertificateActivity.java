package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.view.View;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class CarbonCertificateActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_certificate);
        findViewById(R.id.carbonCertificateBackButton).setOnClickListener(v -> finish());
        loadCertificate();
    }

    private void loadCertificate() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindCertificate(snapshot));
        }).start();
    }

    private void bindCertificate(AssetScreenData.Snapshot snapshot) {
        AssetModels.Certificate current = null;
        if (snapshot.user != null) {
            for (AssetModels.Certificate certificate : snapshot.certificates) {
                if (snapshot.user.carbonPoint >= certificate.requiredCarbonPoint) {
                    current = certificate;
                }
            }
        }
        if (current != null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName, current.certificateName);
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateDescription,
                    current.certificateDescription + "\n" + current.rewardDescription);
            View shareBtn = findViewById(R.id.carbonShareButton);
            if (shareBtn != null) {
                shareBtn.setVisibility(View.VISIBLE);
            }
        } else {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName, "Chưa có chứng nhận");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateDescription, "Bạn chưa đạt chứng nhận carbon tương ứng.");
            View shareBtn = findViewById(R.id.carbonShareButton);
            if (shareBtn != null) {
                shareBtn.setVisibility(View.GONE);
            }
        }
    }
}
