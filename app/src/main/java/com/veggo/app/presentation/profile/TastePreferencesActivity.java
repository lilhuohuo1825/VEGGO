package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class TastePreferencesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taste_preferences);
        findViewById(R.id.tasteBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteAddButton).setOnClickListener(v -> openSetup());
        findViewById(R.id.tasteAddTagChip).setOnClickListener(v -> openSetup());
        findViewById(R.id.tastePeanutCard).setOnClickListener(v -> openAlert());
        findViewById(R.id.tasteTomatoCard).setOnClickListener(v -> openAlert());
        findViewById(R.id.tasteAlertBehaviorCard).setOnClickListener(v -> openAlert());
        findViewById(R.id.tasteEditButton).setOnClickListener(v -> openSetup());
    }

    private void openSetup() {
        startActivity(new Intent(this, TasteSetupActivity.class));
    }

    private void openAlert() {
        startActivity(new Intent(this, TasteAlertActivity.class));
    }
}
