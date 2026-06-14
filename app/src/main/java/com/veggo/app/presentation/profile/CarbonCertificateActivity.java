package com.veggo.app.presentation.profile;

import android.os.Bundle;

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
        if (snapshot.user == null) {
            return;
        }
        AssetModels.Certificate current = null;
        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (snapshot.user.carbonPoint >= certificate.requiredCarbonPoint) {
                current = certificate;
            }
        }
        if (current == null && !snapshot.certificates.isEmpty()) {
            current = snapshot.certificates.get(0);
        }
        if (current == null) {
            return;
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateName, current.certificateName);
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCertificateDescription,
                current.certificateDescription + "\n" + current.rewardDescription);
    }
}
