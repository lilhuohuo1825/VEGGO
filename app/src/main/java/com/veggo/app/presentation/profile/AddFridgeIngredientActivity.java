package com.veggo.app.presentation.profile;

import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class AddFridgeIngredientActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_fridge_ingredient);
        findViewById(R.id.addFridgeBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addFridgeCancelButton).setOnClickListener(v -> finish());
    }
}
