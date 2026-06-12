package com.veggo.app.presentation.order;

import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class RecurringDayDetailActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_day_detail);
        findViewById(R.id.recurringDayBackButton).setOnClickListener(v -> finish());
    }
}
