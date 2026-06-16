package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class CarbonPointsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_points);
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
            return;
        }
        int points = snapshot.user.carbonPoint;
        AssetModels.Certificate current = null;
        AssetModels.Certificate next = null;
        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (points >= certificate.requiredCarbonPoint) {
                current = certificate;
            } else {
                next = certificate;
                break;
            }
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentValue, points + " C");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonCurrentLevel,
                current == null ? "Chưa đạt chứng nhận" : current.certificateName);
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonNextLevel,
                next == null ? "Cao nhất" : next.certificateName);
        if (next == null) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue, points + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft, "Đã đạt cấp cao nhất");
        } else {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressValue,
                    points + " / " + next.requiredCarbonPoint + " C");
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.carbonProgressLeft,
                    "Còn " + (next.requiredCarbonPoint - points) + " C");
        }
    }
}
