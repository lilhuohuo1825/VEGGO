package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.notification.RecurringConfirmationScheduler;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.text.NumberFormat;
import java.util.Locale;

public class RecurringOrderDetailActivity extends BaseActivity {
    public static final String EXTRA_RECURRING_ORDER_ID = "extra_recurring_order_id";

    private final NumberFormat vndFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private RecurringOrderStore store;
    private RecurringOrderStore.RecurringOrder order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_order_detail);
        store = new RecurringOrderStore(this);
        findViewById(R.id.recurringOrderDetailBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.recurringOrderEditButton).setOnClickListener(v -> openEditScreen());
        findViewById(R.id.recurringOrderDeleteButton).setOnClickListener(v -> confirmDelete());
        loadOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store != null) {
            loadOrder();
        }
    }

    private void loadOrder() {
        String orderId = getIntent().getStringExtra(EXTRA_RECURRING_ORDER_ID);
        order = store.findById(orderId);
        if (order == null) {
            Toast.makeText(this, "Không tìm thấy đơn định kỳ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bindOrder();
    }

    private void bindOrder() {
        ((TextView) findViewById(R.id.recurringOrderDetailCode)).setText("#" + order.id);
        ((TextView) findViewById(R.id.recurringOrderDetailName)).setText(order.name);
        ((TextView) findViewById(R.id.recurringOrderDetailSchedule)).setText(
                "Tần suất: " + order.frequency + "\n"
                        + "Bắt đầu từ: " + order.deliveryDate + "\n"
                        + "Khung giờ: " + order.deliverySlot
        );
        ((TextView) findViewById(R.id.recurringOrderDetailReceiver)).setText(
                joinNameAndPhone(order.receiverName, order.receiverPhone)
        );
        ((TextView) findViewById(R.id.recurringOrderDetailAddress)).setText(order.displayAddress());

        LinearLayout container = findViewById(R.id.recurringOrderDetailProductsContainer);
        container.removeAllViews();
        if (order.items != null) {
            for (RecurringOrderStore.RecurringProductItem item : order.items) {
                container.addView(createProductCard(item));
            }
        }
        container.addView(createTotalCard());
    }

    private LinearLayout createProductCard(RecurringOrderStore.RecurringProductItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackgroundResource(R.drawable.bg_order_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        card.setLayoutParams(params);

        ImageView image = new ImageView(this);
        image.setPadding(dp(6), dp(6), dp(6), dp(6));
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_leaf).error(R.drawable.ic_leaf).into(image);
        card.addView(image, new LinearLayout.LayoutParams(dp(70), dp(70)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        contentParams.setMarginStart(dp(10));
        card.addView(content, contentParams);

        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextColor(getColor(R.color.neutral_100));
        name.setTextSize(14);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(name);

        TextView meta = new TextView(this);
        meta.setText(item.unit + "  x" + item.quantity);
        meta.setTextColor(getColor(R.color.neutral_70));
        meta.setTextSize(12);
        meta.setPadding(0, dp(6), 0, 0);
        content.addView(meta);

        TextView price = new TextView(this);
        price.setText(vndFormat.format(item.totalPrice()) + "đ");
        price.setTextColor(getColor(R.color.primary_main));
        price.setTextSize(14);
        price.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(price);
        return card;
    }

    private TextView createTotalCard() {
        TextView total = new TextView(this);
        total.setText("Tổng tiền: " + vndFormat.format(order.estimatedTotal) + "đ/lần"
                + " • +" + formatCarbon(order.carbonPoints) + " điểm carbon");
        total.setGravity(Gravity.END);
        total.setTextColor(getColor(R.color.primary_hover));
        total.setTextSize(16);
        total.setTypeface(null, android.graphics.Typeface.BOLD);
        total.setPadding(0, dp(16), 0, 0);
        return total;
    }

    private void openEditScreen() {
        Intent intent = new Intent(this, CreateRecurringOrderActivity.class);
        intent.putExtra(CreateRecurringOrderActivity.EXTRA_EDIT_RECURRING_ORDER_ID, order.id);
        startActivity(intent);
    }

    private void confirmDelete() {
        VeggoDialog.show(
                this,
                R.drawable.ic_order_cancel_dialog,
                "Xoá đơn định kỳ",
                "Bạn có chắc chắn muốn xoá đơn định kỳ này không?",
                "Xoá",
                "Huỷ",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                    store.delete(order.id);
                    RecurringConfirmationScheduler.runCheckNow(RecurringOrderDetailActivity.this);
                    Toast.makeText(RecurringOrderDetailActivity.this, "Đã xoá đơn định kỳ", Toast.LENGTH_SHORT).show();
                    finish();
                    }
                }
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String joinNameAndPhone(String name, String phone) {
        String safeName = name == null ? "" : name.trim();
        String safePhone = phone == null ? "" : phone.trim();
        if (safeName.isEmpty()) return safePhone;
        if (safePhone.isEmpty()) return safeName;
        return safeName + " - " + safePhone;
    }

    private String formatCarbon(double points) {
        if (Math.abs(points - Math.round(points)) < 0.0001) {
            return String.format(Locale.US, "%.0f", points);
        }
        return String.format(Locale.US, "%.1f", points);
    }
}
