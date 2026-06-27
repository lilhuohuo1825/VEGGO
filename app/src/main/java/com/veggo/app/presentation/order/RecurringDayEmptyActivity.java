package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecurringDayEmptyActivity extends BaseActivity {
    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat titleFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_day_empty);
        findViewById(R.id.recurringDayEmptyBackButton).setOnClickListener(v -> finish());
        bindTitle();
        findViewById(R.id.recurringDayEmptyCreateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRecurringOrderActivity.class))
        );
    }

    private void bindTitle() {
        String deliveryDate = getIntent().getStringExtra(RecurringDayDetailActivity.EXTRA_DELIVERY_DATE);
        try {
            Date date = storageFormat.parse(deliveryDate);
            ((TextView) findViewById(R.id.recurringDayEmptyTitle)).setText(titleFormat.format(date));
        } catch (Exception ignored) {
            ((TextView) findViewById(R.id.recurringDayEmptyTitle)).setText("");
        }
    }
}
