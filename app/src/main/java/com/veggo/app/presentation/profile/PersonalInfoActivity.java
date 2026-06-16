package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class PersonalInfoActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        findViewById(R.id.personalInfoBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.personalInfoLogoutButton).setOnClickListener(v -> finish());
        findViewById(R.id.personalInfoSaveButton).setOnClickListener(v -> finish());
        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindProfile(snapshot.user));
        }).start();
    }

    private void bindProfile(AssetModels.User user) {
        if (user == null) {
            return;
        }
        
        // Ưu tiên FullName từ session/DB. Chỉ fallback khi cả fullName và customerId đều không có (không xảy ra).
        String name = user.fullName;
        if (!AssetScreenData.hasText(name)) {
            name = "Khách hàng " + user.customerId;
        }

        setText(R.id.personalInfoNameInput, name);
        setText(R.id.personalInfoPhoneInput, user.phone);
        setText(R.id.personalInfoEmailInput, user.email);
        setText(R.id.personalInfoBirthdayInput, user.birthDay);
        setText(R.id.personalInfoGenderInput, user.gender);
        ((TextView) findViewById(R.id.personalInfoCarbonBadge)).setText(user.carbonPoint + " điểm carbon");
    }

    private void setText(int viewId, String value) {
        if (!AssetScreenData.hasText(value)) {
            return;
        }
        ((EditText) findViewById(viewId)).setText(value);
    }
}
