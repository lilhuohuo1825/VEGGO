package com.veggo.app.presentation.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.notification.EmulatorSmsSender;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.order.OrderDetailActivity;
import com.veggo.app.presentation.order.RecurringOrderStore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QrPaymentActivity extends BaseActivity {
    public static final String EXTRA_PAYMENT_METHOD = "extra_payment_method";
    public static final String EXTRA_PAYMENT_AMOUNT = "extra_payment_amount";
    public static final String EXTRA_ITEM_COUNT = "extra_item_count";
    public static final String EXTRA_PAYMENT_CODE = "extra_payment_code";
    public static final String EXTRA_SHOW_SUCCESS_IMMEDIATELY = "extra_show_success_immediately";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_CLEAR_CART_ON_SUCCESS = "extra_clear_cart_on_success";
    public static final String EXTRA_CUSTOMER_ID = "extra_customer_id";
    public static final String EXTRA_CART_CLEANUP_SKUS = "extra_cart_cleanup_skus";
    public static final String EXTRA_CART_CLEANUP_WEIGHTS = "extra_cart_cleanup_weights";
    public static final String EXTRA_IS_GUEST_ORDER = "extra_is_guest_order";
    public static final String EXTRA_GUEST_PHONE = "extra_guest_phone";
    public static final String EXTRA_RECURRING_ORDER_ID = "extra_recurring_order_id";
    public static final String EXTRA_RECURRING_OCCURRENCE_DATE = "extra_recurring_occurrence_date";

    private View layoutQrPayment;
    private View layoutOrderSuccess;
    private boolean cartCleared;
    private boolean guestOrderCodeNotified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_payment);

        layoutQrPayment = findViewById(R.id.layoutQrPayment);
        layoutOrderSuccess = findViewById(R.id.layoutOrderSuccess);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnConfirmPaid = findViewById(R.id.btnConfirmPaid);
        TextView btnContinueShopping = findViewById(R.id.btnContinueShopping);
        TextView btnTrackOrder = findViewById(R.id.btnTrackOrder);
        bindPaymentInfo();
        boolean showSuccessImmediately = getIntent().getBooleanExtra(EXTRA_SHOW_SUCCESS_IMMEDIATELY, false);
        if (showSuccessImmediately) {
            showOrderSuccess();
        }

        btnBack.setOnClickListener(v -> {
            if (layoutOrderSuccess.getVisibility() == View.VISIBLE) {
                openMainTab(R.id.nav_home, true);
            } else {
                finish();
            }
        });
        btnConfirmPaid.setOnClickListener(v -> confirmBankTransferPaid());
        btnContinueShopping.setOnClickListener(v -> openMainTab(R.id.nav_home, true));
        btnTrackOrder.setOnClickListener(v -> openOrderDetail());
    }

    private void confirmBankTransferPaid() {
        if (layoutOrderSuccess.getVisibility() == View.VISIBLE) {
            return;
        }
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        TextView btnConfirmPaid = findViewById(R.id.btnConfirmPaid);
        btnConfirmPaid.setEnabled(false);
        btnConfirmPaid.setText("Đang xác nhận...");
        new Thread(() -> {
            if (orderId != null && !orderId.trim().isEmpty()) {
                try {
                    OrderApi orderApi = ApiClient.createService(OrderApi.class);
                    Map<String, String> body = new HashMap<>();
                    body.put("paymentStatus", "paid");
                    orderApi.updatePaymentStatus(orderId, body).execute();
                } catch (Exception ignored) {
                    // Payment screen is simulated; still let the user continue after local confirmation.
                }
            }
            runOnUiThread(this::showOrderSuccess);
        }).start();
    }

    private void showOrderSuccess() {
        layoutQrPayment.setVisibility(View.GONE);
        layoutOrderSuccess.setVisibility(View.VISIBLE);
        TextView btnConfirmPaid = findViewById(R.id.btnConfirmPaid);
        if (btnConfirmPaid != null) {
            btnConfirmPaid.setEnabled(true);
            btnConfirmPaid.setText("Xác nhận thanh toán");
        }
        clearCartAfterOrderSuccess();
        markRecurringOccurrenceCompletedIfNeeded();
        notifyGuestOrderCodeIfNeeded();
    }

    @Override
    public void onBackPressed() {
        if (layoutOrderSuccess.getVisibility() == View.VISIBLE) {
            openMainTab(R.id.nav_home, true);
            return;
        }
        super.onBackPressed();
    }

    private void markRecurringOccurrenceCompletedIfNeeded() {
        String recurringOrderId = getIntent().getStringExtra(EXTRA_RECURRING_ORDER_ID);
        String occurrenceDate = getIntent().getStringExtra(EXTRA_RECURRING_OCCURRENCE_DATE);
        if (recurringOrderId == null || recurringOrderId.trim().isEmpty()
                || occurrenceDate == null || occurrenceDate.trim().isEmpty()) {
            return;
        }
        String placedOrderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        new RecurringOrderStore(this).markOccurrenceCompleted(
                recurringOrderId,
                occurrenceDate,
                placedOrderId == null ? "" : placedOrderId
        );
    }

    private void notifyGuestOrderCodeIfNeeded() {
        if (guestOrderCodeNotified || !getIntent().getBooleanExtra(EXTRA_IS_GUEST_ORDER, false)) {
            return;
        }
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null || orderId.trim().isEmpty()) {
            return;
        }
        guestOrderCodeNotified = true;
        EmulatorSmsSender.send(
                this,
                "VEGGO: Don hang cua ban da duoc tao thanh cong. Ma don: " + orderId
                        + ". De xem trang thai, vao Don hang cua toi, nhap ma don vao o tim kiem, sau do nhap so dien thoai da dat hang."
        );
    }

    private void clearCartAfterOrderSuccess() {
        if (cartCleared || !getIntent().getBooleanExtra(EXTRA_CLEAR_CART_ON_SUCCESS, false)) {
            return;
        }
        String customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
        if (customerId == null || customerId.trim().isEmpty()) {
            return;
        }
        cartCleared = true;
        new Thread(() -> {
            try {
                CartApi cartApi = ApiClient.createService(CartApi.class);
                java.util.ArrayList<String> skus = getIntent().getStringArrayListExtra(EXTRA_CART_CLEANUP_SKUS);
                double[] weights = getIntent().getDoubleArrayExtra(EXTRA_CART_CLEANUP_WEIGHTS);
                if (skus == null || weights == null || skus.isEmpty()) {
                    return;
                }
                int count = Math.min(skus.size(), weights.length);
                for (int i = 0; i < count; i++) {
                    String sku = skus.get(i);
                    if (sku != null && !sku.trim().isEmpty()) {
                        cartApi.removeItem(customerId, sku, weights[i] > 0 ? weights[i] : 1.0).execute();
                    }
                }
            } catch (Exception ignored) {
                // The order is already created; cart cleanup can be retried from backend sync later.
            }
        }).start();
    }

    private void openOrderDetail() {
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null || orderId.trim().isEmpty()) {
            openMainTab(R.id.nav_orders, false);
            return;
        }
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
        finish();
    }

    private void openMainTab(int navItemId, boolean scrollHomeProducts) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, navItemId);
        intent.putExtra(MainActivity.EXTRA_SCROLL_HOME_PRODUCTS, scrollHomeProducts);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void bindPaymentInfo() {
        long amount = getIntent().getLongExtra(EXTRA_PAYMENT_AMOUNT, 0);
        int itemCount = getIntent().getIntExtra(EXTRA_ITEM_COUNT, 0);
        String paymentCode = getIntent().getStringExtra(EXTRA_PAYMENT_CODE);

        TextView tvPaymentTitle = findViewById(R.id.tvPaymentTitle);
        TextView tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        TextView tvPaymentTimer = findViewById(R.id.tvPaymentTimer);
        TextView tvPaymentItemCount = findViewById(R.id.tvPaymentItemCount);
        TextView tvPaymentCode = findViewById(R.id.tvPaymentCode);
        TextView tvTransferContent = findViewById(R.id.tvTransferContent);

        tvPaymentTitle.setText("THÔNG TIN CHUYỂN KHOẢN");
        tvPaymentTimer.setText("Thời gian thanh toán còn lại: 09:59");
        tvPaymentAmount.setText(formatCurrency(amount));
        tvPaymentItemCount.setText("Tổng cộng: " + itemCount + " sản phẩm");
        tvPaymentCode.setText(paymentCode != null ? paymentCode : "VG000000");
        tvTransferContent.setText(paymentCode != null ? paymentCode : "VG000000");
    }

    private String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }
}
