package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class RecurringOrdersActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_orders);
        findViewById(R.id.recurringOrdersBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.recurringOrdersMenuButton).setOnClickListener(v ->
                com.veggo.app.presentation.common.AssetScreenData.showOrderOptions(this)
        );
        findViewById(R.id.recurringCreateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRecurringOrderActivity.class))
        );
        findViewById(R.id.recurringSelectedDay).setOnClickListener(v ->
                startActivity(new Intent(this, RecurringDayDetailActivity.class))
        );
        findViewById(R.id.recurringEmptyDay).setOnClickListener(v ->
                startActivity(new Intent(this, RecurringDayEmptyActivity.class))
        );

        if (!new com.veggo.app.core.preferences.AppPreferences(this).isLoggedIn()) {
            findViewById(R.id.recurringCalendarCard).setVisibility(android.view.View.GONE);
        }
    }
}
