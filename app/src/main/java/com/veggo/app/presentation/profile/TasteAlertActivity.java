package com.veggo.app.presentation.profile;

import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class TasteAlertActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taste_alert);
        findViewById(R.id.tasteAlertBackButton).setOnClickListener(v -> finish());
    }
}
