package com.veggo.app.presentation.checkout;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.PaymentItemAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.ui.BaseActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CheckoutGuestActivity extends BaseActivity {
    private static final int COLLAPSED_PRODUCT_COUNT = 4;

    private TextView tvCheckoutNote;
    private TextView tvScheduleDeliveryTime;
    private TextView tvToggleProducts;
    private ImageView imgToggleProducts;
    private LinearLayout layoutToggleProducts;
    private View layoutFastDelivery;
    private View layoutScheduleDelivery;
    private PaymentItemAdapter paymentItemAdapter;
    private final List<PaymentItemAdapter.PaymentItemUiModel> previewItems = new ArrayList<>();
    private boolean isProductsExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_guest);

        View layoutNote = findViewById(R.id.layoutNote);
        View layoutDeliveryOrigin = findViewById(R.id.layoutDeliveryOrigin);
        View layoutVoucher = findViewById(R.id.layoutVoucher);
        View btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        RecyclerView rvCheckoutProducts = findViewById(R.id.rvCheckoutProducts);

        tvCheckoutNote = findViewById(R.id.tvCheckoutNote);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        layoutToggleProducts = findViewById(R.id.layoutToggleProducts);
        tvToggleProducts = findViewById(R.id.tvToggleProducts);
        imgToggleProducts = findViewById(R.id.imgToggleProducts);
        layoutFastDelivery = findViewById(R.id.layoutFastDelivery);
        layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);
        tvCheckoutNote.setText("");
        tvCheckoutNote.setHint("Nhap ghi chu");

        setupProducts(rvCheckoutProducts);
        selectDeliveryMode(true);
        layoutToggleProducts.setOnClickListener(v -> toggleProducts());

        layoutDeliveryOrigin.setOnClickListener(v -> showPopup(R.layout.dialog_delivery_origin));
        layoutVoucher.setOnClickListener(v -> showPopup(R.layout.dialog_voucher));
        layoutNote.setOnClickListener(v -> showNoteDialog());
        layoutFastDelivery.setOnClickListener(v -> selectDeliveryMode(true));
        layoutScheduleDelivery.setOnClickListener(v -> {
            selectDeliveryMode(false);
            showScheduleTimePicker();
        });
        btnPlaceOrder.setOnClickListener(v -> startActivity(new Intent(this, QrPaymentActivity.class)));
    }

    private void setupProducts(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        previewItems.clear();
        previewItems.addAll(buildPreviewItems());
        paymentItemAdapter = new PaymentItemAdapter(getVisiblePreviewItems());
        recyclerView.setAdapter(paymentItemAdapter);
        updateToggleProductsUi();
    }

    private List<PaymentItemAdapter.PaymentItemUiModel> buildPreviewItems() {
        List<PaymentItemAdapter.PaymentItemUiModel> items = new ArrayList<>();
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                getString(R.string.orders_sample_product_name),
                "200gr",
                45000,
                1,
                R.drawable.ic_vegetable
        ));
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                "Ca chua bi huu co",
                "500gr",
                32000,
                2,
                R.drawable.ic_fruit
        ));
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                "Ca chua bi huu co",
                "500gr",
                32000,
                2,
                R.drawable.ic_fruit
        ));
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                getString(R.string.orders_sample_product_name),
                "200gr",
                45000,
                1,
                R.drawable.ic_vegetable
        ));
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                "Ca chua bi huu co",
                "500gr",
                32000,
                2,
                R.drawable.ic_fruit
        ));
        items.add(new PaymentItemAdapter.PaymentItemUiModel(
                "Rau chan vit baby",
                "250gr",
                28000,
                1,
                R.drawable.ic_leaf
        ));
        return items;
    }

    private void toggleProducts() {
        isProductsExpanded = !isProductsExpanded;
        paymentItemAdapter.setItems(getVisiblePreviewItems());
        updateToggleProductsUi();
    }

    private List<PaymentItemAdapter.PaymentItemUiModel> getVisiblePreviewItems() {
        if (isProductsExpanded || previewItems.size() <= COLLAPSED_PRODUCT_COUNT) {
            return new ArrayList<>(previewItems);
        }
        return new ArrayList<>(previewItems.subList(0, COLLAPSED_PRODUCT_COUNT));
    }

    private void updateToggleProductsUi() {
        if (layoutToggleProducts == null) {
            return;
        }
        boolean shouldShowToggle = previewItems.size() > COLLAPSED_PRODUCT_COUNT;
        layoutToggleProducts.setVisibility(shouldShowToggle ? View.VISIBLE : View.GONE);
        if (!shouldShowToggle) {
            return;
        }
        tvToggleProducts.setText(isProductsExpanded ? "Thu gon" : "Xem them");
        imgToggleProducts.setImageResource(isProductsExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
    }

    private void showPopup(int layoutResId) {
        if (layoutResId == R.layout.dialog_voucher) {
            showVoucherDialog();
            return;
        }
        if (layoutResId == R.layout.dialog_delivery_origin) {
            showDeliveryOriginDialog();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(layoutResId, null, false);
        createBottomSheetDialog(dialogView).show();
    }

    private void showVoucherDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvVoucherOptions);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvVoucherSelectedCount);
        TextView tvSelectedTitle = dialogView.findViewById(R.id.tvVoucherSelectedTitle);
        TextView btnApplyVoucher = dialogView.findViewById(R.id.btnApplyVoucher);

        List<VoucherOptionAdapter.VoucherItemUiModel> vouchers = buildVoucherItems();
        VoucherOptionAdapter.VoucherItemUiModel initialVoucher = vouchers.get(0);
        tvSelectedCount.setText("1 ma da duoc chon");
        tvSelectedTitle.setText(initialVoucher.title);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new VoucherOptionAdapter(vouchers, 0, item -> {
            tvSelectedCount.setText("1 ma da duoc chon");
            tvSelectedTitle.setText(item.title);
        }));

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnApplyVoucher.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeliveryOriginDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_origin, null, false);
        TextView tvAddress = dialogView.findViewById(R.id.tvDeliveryOriginAddress);
        TextView tvDistance = dialogView.findViewById(R.id.tvDeliveryOriginDistance);
        TextView btnClose = dialogView.findViewById(R.id.btnCloseDeliveryOrigin);

        tvAddress.setText("123 Nguyen Van Linh, P. Tan Phong, Quan 7, TP. Ho Chi Minh");
        tvDistance.setText("20 km ~ 45 phut");

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note, null, false);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        TextView btnSave = dialogView.findViewById(R.id.btnSave);

        CharSequence currentNote = tvCheckoutNote.getText();
        if (currentNote != null && currentNote.length() > 0) {
            edtNote.setText(currentNote.toString());
            edtNote.setSelection(edtNote.getText().length());
        }

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnSave.setOnClickListener(v -> {
            tvCheckoutNote.setText(edtNote.getText().toString().trim());
            dialog.dismiss();
        });
        dialog.show();
    }

    private BottomSheetDialog createBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                bottomSheet.setPadding(0, 0, 0, 0);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    private void showScheduleTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int initialHour = calendar.get(Calendar.HOUR_OF_DAY);
        int initialMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> tvScheduleDeliveryTime.setText(buildScheduleText(hourOfDay, minute)),
                initialHour,
                initialMinute,
                true
        );
        timePickerDialog.show();
    }

    private void selectDeliveryMode(boolean isFastDelivery) {
        if (layoutFastDelivery != null) {
            layoutFastDelivery.setBackgroundResource(isFastDelivery ? R.drawable.bg_selected : R.drawable.bg_normal);
        }
        if (layoutScheduleDelivery != null) {
            layoutScheduleDelivery.setBackgroundResource(isFastDelivery ? R.drawable.bg_normal : R.drawable.bg_selected);
        }
    }

    private String buildScheduleText(int startHour, int startMinute) {
        int endHour = (startHour + 2) % 24;
        return String.format(
                Locale.getDefault(),
                "Du kien nhan hang hom nay, %02d:%02d - %02d:%02d",
                startHour,
                startMinute,
                endHour,
                startMinute
        );
    }

    private List<VoucherOptionAdapter.VoucherItemUiModel> buildVoucherItems() {
        List<VoucherOptionAdapter.VoucherItemUiModel> items = new ArrayList<>();
        items.add(new VoucherOptionAdapter.VoucherItemUiModel(
                "Giam 50k cho lan dau tien mua hang",
                "Don toi thieu: 2.000.000 VND",
                "Het han trong: 2 ngay",
                R.drawable.ic_voucher
        ));
        items.add(new VoucherOptionAdapter.VoucherItemUiModel(
                "Giam 20k cho don rau cu",
                "Don toi thieu: 300.000 VND",
                "Het han trong: 5 ngay",
                R.drawable.ic_voucher
        ));
        items.add(new VoucherOptionAdapter.VoucherItemUiModel(
                "Mien phi van chuyen",
                "Ap dung cho don tu 199.000 VND",
                "Het han trong: 1 ngay",
                R.drawable.ic_voucher
        ));
        return items;
    }
}
