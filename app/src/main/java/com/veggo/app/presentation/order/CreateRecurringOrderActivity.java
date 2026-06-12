package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.profile.AddressBookActivity;

public class CreateRecurringOrderActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recurring_order);
        findViewById(R.id.createRecurringBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.createRecurringAddressBookButton).setOnClickListener(v ->
                startActivity(new Intent(this, AddressBookActivity.class))
        );
        findViewById(R.id.createRecurringScheduleButton).setOnClickListener(v ->
                startActivity(new Intent(this, RecurringDayDetailActivity.class))
        );
    }
}
