package com.veggo.app.presentation.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.auth.LoginActivity;

public class LoginRequiredActivity extends BaseActivity {
    private static final String EXTRA_FEATURE_NAME = "extra_feature_name";

    public static void open(Context context, String featureName) {
        Intent intent = new Intent(context, LoginRequiredActivity.class);
        intent.putExtra(EXTRA_FEATURE_NAME, featureName);
        context.startActivity(intent);
    }

    public static boolean redirectIfGuest(Activity activity, String featureName) {
        if (new AppPreferences(activity).isLoggedIn()) {
            return false;
        }
        open(activity, featureName);
        activity.finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_required);

        String featureName = getIntent().getStringExtra(EXTRA_FEATURE_NAME);
        if (featureName == null || featureName.trim().isEmpty()) {
            featureName = "tính năng này";
        }

        findViewById(R.id.loginRequiredBackButton).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.loginRequiredTitle)).setText("Đăng nhập để sử dụng " + featureName);
        ((TextView) findViewById(R.id.loginRequiredMessage)).setText(
                "Mascot VEGGO đang giữ khu vực cá nhân của bạn. Vui lòng đăng nhập để dùng "
                        + featureName
                        + " và đồng bộ dữ liệu của bạn."
        );
        findViewById(R.id.loginRequiredButton).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }
}
