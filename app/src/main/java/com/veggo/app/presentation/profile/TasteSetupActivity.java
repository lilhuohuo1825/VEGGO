package com.veggo.app.presentation.profile;

import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class TasteSetupActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taste_setup);
        findViewById(R.id.tasteSetupBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteSetupCancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteSetupSaveButton).setOnClickListener(v -> finish());
    }
}
