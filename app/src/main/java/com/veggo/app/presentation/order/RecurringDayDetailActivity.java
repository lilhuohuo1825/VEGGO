package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurringDayDetailActivity extends BaseActivity {
    public static final String EXTRA_DELIVERY_DATE = "extra_delivery_date";

    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat titleFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
    private final NumberFormat vndFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_day_detail);
        findViewById(R.id.recurringDayBackButton).setOnClickListener(v -> finish());
        bindDayOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindDayOrders();
    }

    private void bindDayOrders() {
        String deliveryDate = getIntent().getStringExtra(EXTRA_DELIVERY_DATE);
        Date date = parseDate(deliveryDate);
        String customerId = new AppPreferences(this).getCustomerId();
        List<RecurringOrderStore.RecurringOrder> orders =
                new RecurringOrderStore(this).occurrencesForCustomerOnDate(customerId, deliveryDate);

        ((TextView) findViewById(R.id.recurringDayTitle)).setText(date == null ? "" : titleFormat.format(date));

        LinearLayout container = findViewById(R.id.recurringDayOrdersContainer);
        container.removeAllViews();
        for (RecurringOrderStore.RecurringOrder order : orders) {
            container.addView(createOrderCard(order));
        }
    }

    private LinearLayout createOrderCard(RecurringOrderStore.RecurringOrder order) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));
        card.setBackgroundResource(R.drawable.bg_order_card);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecurringOrderDetailActivity.class);
            intent.putExtra(RecurringOrderDetailActivity.EXTRA_RECURRING_ORDER_ID, order.id);
            startActivity(intent);
        });
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(16);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(32)
        ));

        TextView id = new TextView(this);
        id.setText("#" + order.id);
        id.setTextColor(getColor(R.color.neutral_100));
        id.setTextSize(14);
        id.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(id, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView status = new TextView(this);
        status.setText("Định kỳ");
        status.setGravity(Gravity.CENTER);
        status.setMinWidth(dp(90));
        status.setTextColor(getColor(R.color.order_status_pending));
        status.setTextSize(12);
        status.setBackgroundResource(R.drawable.bg_order_pending_chip);
        header.addView(status, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(24)));

        TextView title = new TextView(this);
        title.setText(order.name);
        title.setTextColor(getColor(R.color.neutral_60));
        title.setTextSize(12);
        title.setPadding(0, dp(2), 0, 0);
        card.addView(title);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.neutral_30));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        dividerParams.topMargin = dp(8);
        card.addView(divider, dividerParams);

        card.addView(createProductPreview(order));

        TextView total = new TextView(this);
        total.setText("Tổng tiền: " + vndFormat.format(order.estimatedTotal) + "đ/lần"
                + " • +" + formatCarbon(order.carbonPoints) + " điểm carbon");
        total.setGravity(Gravity.END);
        total.setTextColor(getColor(R.color.primary_hover));
        total.setTextSize(14);
        total.setTypeface(null, android.graphics.Typeface.BOLD);
        total.setPadding(0, dp(10), 0, 0);
        card.addView(total);
        return card;
    }

    private LinearLayout createProductPreview(RecurringOrderStore.RecurringOrder order) {
        RecurringOrderStore.RecurringProductItem firstItem =
                order.items != null && !order.items.isEmpty() ? order.items.get(0) : null;
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        ImageView image = new ImageView(this);
        image.setPadding(dp(4), dp(4), dp(4), dp(4));
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        Glide.with(this)
                .load(firstItem == null ? null : firstItem.imageUrl)
                .placeholder(R.drawable.ic_leaf)
                .error(R.drawable.ic_leaf)
                .into(image);
        row.addView(image, new LinearLayout.LayoutParams(dp(70), dp(70)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        infoParams.setMarginStart(dp(8));
        row.addView(info, infoParams);

        TextView name = new TextView(this);
        name.setText(firstItem == null ? order.itemSummary : firstItem.name);
        name.setTextColor(getColor(R.color.neutral_100));
        name.setTextSize(14);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setSingleLine(true);
        info.addView(name);

        TextView meta = new TextView(this);
        meta.setText(firstItem == null
                ? order.frequency + " • " + order.deliverySlot
                : firstItem.unit + "  x" + firstItem.quantity);
        meta.setTextColor(getColor(R.color.neutral_70));
        meta.setTextSize(12);
        meta.setPadding(0, dp(6), 0, 0);
        info.addView(meta);

        TextView price = new TextView(this);
        price.setText(firstItem == null ? "" : vndFormat.format(firstItem.totalPrice()) + "đ");
        price.setTextColor(getColor(R.color.neutral_100));
        price.setTextSize(14);
        price.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(price);
        return row;
    }

    private Date parseDate(String value) {
        try {
            return storageFormat.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatCarbon(double points) {
        if (Math.abs(points - Math.round(points)) < 0.0001) {
            return String.format(Locale.US, "%.0f", points);
        }
        return String.format(Locale.US, "%.1f", points);
    }
}
