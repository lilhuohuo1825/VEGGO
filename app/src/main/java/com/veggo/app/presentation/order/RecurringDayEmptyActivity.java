package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class RecurringDayEmptyActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_day_empty);
        findViewById(R.id.recurringDayEmptyBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.recurringDayEmptyCreateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRecurringOrderActivity.class))
        );
    }
}
