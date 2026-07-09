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
import com.veggo.app.core.notification.RecurringConfirmationHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecurringConfirmOrderActivity extends BaseActivity {
    public static final String EXTRA_RECURRING_ORDER_ID = "extra_recurring_order_id";
    public static final String EXTRA_OCCURRENCE_DATE = "extra_occurrence_date";

    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat titleFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
    private final NumberFormat vndFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private RecurringOrderStore store;
    private RecurringOrderStore.RecurringOrder order;
    private String occurrenceDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_confirm_order);
        store = new RecurringOrderStore(this);
        findViewById(R.id.recurringConfirmBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.recurringConfirmCancelButton).setOnClickListener(v -> confirmCancelOccurrence());
        findViewById(R.id.recurringConfirmOrderButton).setOnClickListener(v -> onPrimaryAction());
        loadOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (order != null && occurrenceDate != null) {
            bindOrder();
        }
    }

    private void loadOrder() {
        String orderId = getIntent().getStringExtra(EXTRA_RECURRING_ORDER_ID);
        occurrenceDate = getIntent().getStringExtra(EXTRA_OCCURRENCE_DATE);
        order = store.findById(orderId);
        if (order == null || occurrenceDate == null || occurrenceDate.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đơn định kỳ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!store.orderOccursOnDate(order, occurrenceDate)) {
            Toast.makeText(this, "Ngày giao không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bindOrder();
    }

    private void bindOrder() {
        RecurringOrderStore.OccurrenceState state = store.getOccurrence(order.id, occurrenceDate);
        String status = state == null ? null : state.status;
        boolean orderPlaced = RecurringConfirmationHelper.hasOrderPlaced(status);
        boolean canManage = RecurringConfirmationHelper.isConfirmWindowOpen(occurrenceDate);
        boolean canCheckout = RecurringConfirmationHelper.isActionable(status) && canManage;

        ((TextView) findViewById(R.id.recurringConfirmCode)).setText("#" + order.id);
        ((TextView) findViewById(R.id.recurringConfirmName)).setText(order.name);

        Date date = parseDate(occurrenceDate);
        String displayDate = date == null ? occurrenceDate : titleFormat.format(date);
        ((TextView) findViewById(R.id.recurringConfirmSchedule)).setText(
                "Ngày giao: " + displayDate + "\n"
                        + "Khung giờ: " + order.deliverySlot + "\n"
                        + "Tần suất: " + order.frequency
        );
        ((TextView) findViewById(R.id.recurringConfirmReceiver)).setText(
                joinNameAndPhone(order.receiverName, order.receiverPhone)
        );
        ((TextView) findViewById(R.id.recurringConfirmAddress)).setText(order.displayAddress());

        TextView noticeView = findViewById(R.id.recurringConfirmNotice);
        TextView statusView = findViewById(R.id.recurringConfirmStatus);
        TextView cancelButton = findViewById(R.id.recurringConfirmCancelButton);
        TextView primaryButton = findViewById(R.id.recurringConfirmOrderButton);

        if (orderPlaced) {
            noticeView.setText(R.string.recurring_confirm_notice_placed);
            statusView.setText(R.string.recurring_confirm_status_completed);
            cancelButton.setEnabled(RecurringConfirmationHelper.canCancelPlacedOrder(occurrenceDate));
            cancelButton.setAlpha(cancelButton.isEnabled() ? 1f : 0.5f);
            primaryButton.setText(R.string.recurring_confirm_view_order);
            primaryButton.setEnabled(state != null && state.placedOrderId != null && !state.placedOrderId.isEmpty());
            primaryButton.setAlpha(primaryButton.isEnabled() ? 1f : 0.5f);
        } else if (RecurringConfirmationHelper.STATUS_CANCELLED.equals(status)) {
            noticeView.setText(R.string.recurring_confirm_notice_cancelled);
            statusView.setText(R.string.recurring_confirm_status_cancelled);
            setButtonsDisabled(cancelButton, primaryButton);
        } else if (RecurringConfirmationHelper.STATUS_SKIPPED.equals(status)) {
            noticeView.setText(R.string.recurring_confirm_notice_skipped);
            statusView.setText(R.string.recurring_confirm_status_skipped);
            setButtonsDisabled(cancelButton, primaryButton);
        } else if (canCheckout) {
            noticeView.setText(R.string.recurring_confirm_notice);
            statusView.setText(R.string.recurring_confirm_status_pending);
            cancelButton.setEnabled(true);
            cancelButton.setAlpha(1f);
            primaryButton.setText(R.string.recurring_confirm_place_order);
            primaryButton.setEnabled(true);
            primaryButton.setAlpha(1f);
        } else {
            noticeView.setText(R.string.recurring_confirm_notice_expired);
            statusView.setText(R.string.recurring_confirm_status_skipped);
            setButtonsDisabled(cancelButton, primaryButton);
        }

        LinearLayout container = findViewById(R.id.recurringConfirmProductsContainer);
        container.removeAllViews();
        if (order.items != null) {
            for (RecurringOrderStore.RecurringProductItem item : order.items) {
                container.addView(createProductCard(item));
            }
        }
        container.addView(createTotalCard());
    }

    private void setButtonsDisabled(TextView cancelButton, TextView primaryButton) {
        cancelButton.setEnabled(false);
        cancelButton.setAlpha(0.5f);
        primaryButton.setText(R.string.recurring_confirm_place_order);
        primaryButton.setEnabled(false);
        primaryButton.setAlpha(0.5f);
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

    private void onPrimaryAction() {
        RecurringOrderStore.OccurrenceState state = store.getOccurrence(order.id, occurrenceDate);
        if (state != null && RecurringConfirmationHelper.hasOrderPlaced(state.status)) {
            openPlacedOrder(state.placedOrderId);
            return;
        }
        openCheckout();
    }

    private void confirmCancelOccurrence() {
        RecurringOrderStore.OccurrenceState state = store.getOccurrence(order.id, occurrenceDate);
        boolean orderPlaced = state != null && RecurringConfirmationHelper.hasOrderPlaced(state.status);
        int messageRes = orderPlaced
                ? R.string.recurring_confirm_cancel_placed_message
                : R.string.recurring_confirm_cancel_message;

        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                getString(R.string.recurring_confirm_cancel_title),
                getString(messageRes),
                getString(R.string.recurring_confirm_cancel_occurrence),
                "Quay lại",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        store.markOccurrenceCancelled(order.id, occurrenceDate);
                        Toast.makeText(
                                RecurringConfirmOrderActivity.this,
                                R.string.recurring_confirm_cancelled_toast,
                                Toast.LENGTH_SHORT
                        ).show();
                        if (orderPlaced && state.placedOrderId != null && !state.placedOrderId.isEmpty()) {
                            openPlacedOrder(state.placedOrderId);
                        } else {
                            finish();
                        }
                    }
                }
        );
    }

    private void openPlacedOrder(String placedOrderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, placedOrderId);
        startActivity(intent);
    }

    private void openCheckout() {
        if (!RecurringConfirmationHelper.isConfirmWindowOpen(occurrenceDate)) {
            Toast.makeText(this, R.string.recurring_confirm_expired_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        RecurringCheckoutStore checkoutStore = new RecurringCheckoutStore(this);
        checkoutStore.save(store.buildCheckoutCart(order), order.id, occurrenceDate, order);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.EXTRA_RECURRING_CHECKOUT, true);
        intent.putExtra(CheckoutActivity.EXTRA_RECURRING_ORDER_ID, order.id);
        intent.putExtra(CheckoutActivity.EXTRA_RECURRING_OCCURRENCE_DATE, occurrenceDate);
        startActivity(intent);
    }

    private Date parseDate(String value) {
        try {
            return storageFormat.parse(value);
        } catch (Exception exception) {
            return null;
        }
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
