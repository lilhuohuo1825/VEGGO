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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.LocationOptionAdapter;
import com.veggo.app.adapter.PaymentItemAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.presentation.profile.AddressFormActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends BaseActivity {
    private static final int COLLAPSED_PRODUCT_COUNT = 4;

    private TextView tvCheckoutNote;
    private TextView tvScheduleDeliveryTime;
    private TextView tvToggleProducts;
    private TextView tvAddressName;
    private TextView tvAddressPhone;
    private TextView tvAddressDetail;
    private TextView tvAddressDefaultBadge;
    private ImageView imgToggleProducts;
    private LinearLayout layoutToggleProducts;
    private View layoutFastDelivery;
    private View layoutScheduleDelivery;
    private PaymentItemAdapter paymentItemAdapter;
    private TextView tvCheckoutSubtotal, tvCheckoutVoucherDiscount, tvCheckoutProductDiscount, tvCheckoutShippingFee, tvCheckoutShippingDiscount, tvCheckoutTotal, tvCheckoutBottomTotal;
    private final List<PaymentItemAdapter.PaymentItemUiModel> previewItems = new ArrayList<>();
    private final List<LocationOptionAdapter.LocationItemUiModel> locationItems = new ArrayList<>();
    private boolean isProductsExpanded;
    private int selectedLocationIndex;
    private PromotionDto selectedPromotion;
    private CartDto cartData;
    private final ActivityResultLauncher<Intent> addAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                addLocationFromResult(result.getData());
                showLocationDialog();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        View layoutAddress = findViewById(R.id.layoutAddress);
        View layoutDeliveryOrigin = findViewById(R.id.layoutDeliveryOrigin);
        View layoutNote = findViewById(R.id.layoutNote);
        View layoutVoucher = findViewById(R.id.layoutVoucher);
        View btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        RecyclerView rvCheckoutProducts = findViewById(R.id.rvCheckoutProducts);
        View btnBack = findViewById(R.id.checkoutBackButton);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvCheckoutNote = findViewById(R.id.tvCheckoutNote);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        layoutToggleProducts = findViewById(R.id.layoutToggleProducts);
        tvToggleProducts = findViewById(R.id.tvToggleProducts);
        imgToggleProducts = findViewById(R.id.imgToggleProducts);
        tvAddressName = findViewById(R.id.tvAddressName);
        tvAddressPhone = findViewById(R.id.tvAddressPhone);
        tvAddressDetail = findViewById(R.id.tvAddressDetail);
        tvAddressDefaultBadge = findViewById(R.id.tvAddressDefaultBadge);
        layoutFastDelivery = findViewById(R.id.layoutFastDelivery);
        layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);

        tvCheckoutSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvCheckoutVoucherDiscount = findViewById(R.id.tvCheckoutVoucherDiscount);
        tvCheckoutProductDiscount = findViewById(R.id.tvCheckoutProductDiscount);
        tvCheckoutShippingFee = findViewById(R.id.tvCheckoutShippingFee);
        tvCheckoutShippingDiscount = findViewById(R.id.tvCheckoutShippingDiscount);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        tvCheckoutBottomTotal = findViewById(R.id.tvCheckoutBottomTotal);

        if (getIntent().hasExtra("SELECTED_PROMOTION")) {
            selectedPromotion = (PromotionDto) getIntent().getSerializableExtra("SELECTED_PROMOTION");
        }
        if (getIntent().hasExtra("CART_DATA")) {
            cartData = (CartDto) getIntent().getSerializableExtra("CART_DATA");
        }

        tvCheckoutNote.setText("");
        tvCheckoutNote.setHint("Nhap ghi chu");

        setupProducts(rvCheckoutProducts);
        ensureLocationItems();
        applySelectedLocationToAddressCard();
        selectDeliveryMode(true);
        updateSummary();
        layoutToggleProducts.setOnClickListener(v -> toggleProducts());

        layoutAddress.setOnClickListener(v -> showPopup(R.layout.dialog_location));
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
        if (cartData != null && cartData.getItems() != null) {
            for (CartDto.CartItemDto item : cartData.getItems()) {
                if (item.getProduct() != null) {
                    items.add(new PaymentItemAdapter.PaymentItemUiModel(
                            item.getProduct().getProductName(),
                            String.valueOf(item.getSelectedWeight()) + item.getProduct().getUnit(),
                            (int) item.getProduct().getPrice(),
                            item.getQuantity(),
                            R.drawable.ic_vegetable // Placeholder or map from DTO
                    ));
                }
            }
        } else {
            // Fallback for legacy/testing
            items.add(new PaymentItemAdapter.PaymentItemUiModel(
                    getString(R.string.orders_sample_product_name),
                    "200gr",
                    45000,
                    1,
                    R.drawable.ic_vegetable
            ));
        }
        return items;
    }

    private void updateSummary() {
        long subtotal = 0;
        if (cartData != null && cartData.getItems() != null) {
            for (CartDto.CartItemDto item : cartData.getItems()) {
                if (item.getProduct() != null) {
                    subtotal += item.getProduct().getPrice() * item.getQuantity();
                }
            }
        }

        long shippingFee = 18000; // Default or from logic
        long voucherDiscount = 0;
        if (selectedPromotion != null && subtotal >= selectedPromotion.getMinOrderValue()) {
            if ("percentage".equalsIgnoreCase(selectedPromotion.getDiscountType())) {
                voucherDiscount = (subtotal * selectedPromotion.getDiscountValue()) / 100;
                if (selectedPromotion.getMaxDiscountValue() > 0) {
                    voucherDiscount = Math.min(voucherDiscount, selectedPromotion.getMaxDiscountValue());
                }
            } else {
                voucherDiscount = selectedPromotion.getDiscountValue();
            }
        }

        long productDiscount = 0; // Can be calculated from DTO if needed
        long shippingDiscount = 0; // Can be calculated from DTO if needed
        long total = Math.max(0, subtotal + shippingFee - voucherDiscount - productDiscount - shippingDiscount);

        tvCheckoutSubtotal.setText(formatCurrency(subtotal));
        tvCheckoutVoucherDiscount.setText("-" + formatCurrency(voucherDiscount));
        tvCheckoutProductDiscount.setText("-" + formatCurrency(productDiscount));
        tvCheckoutShippingFee.setText(formatCurrency(shippingFee));
        tvCheckoutShippingDiscount.setText("-" + formatCurrency(shippingDiscount));
        tvCheckoutTotal.setText(formatCurrency(total));
        tvCheckoutBottomTotal.setText(formatCurrency(total));
    }

    private String formatCurrency(long value) {
        return String.format(Locale.getDefault(), "%,dđ", value).replace(",", ".");
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
        if (layoutResId == R.layout.dialog_location) {
            showLocationDialog();
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

    private void showLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvLocationOptions);
        TextView btnCancelLocation = dialogView.findViewById(R.id.btnCancelLocation);
        TextView btnConfirmLocation = dialogView.findViewById(R.id.btnConfirmLocation);
        TextView btnAddLocation = dialogView.findViewById(R.id.btnAddLocation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ensureLocationItems();
        recyclerView.setAdapter(new LocationOptionAdapter(locationItems, selectedLocationIndex, item ->
                selectedLocationIndex = locationItems.indexOf(item)
        ));

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnCancelLocation.setOnClickListener(v -> dialog.dismiss());
        btnConfirmLocation.setOnClickListener(v -> {
            applySelectedLocationToAddressCard();
            dialog.dismiss();
        });
        btnAddLocation.setOnClickListener(v -> {
            dialog.dismiss();
            openAddAddressForm();
        });
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

    private void ensureLocationItems() {
        if (!locationItems.isEmpty()) {
            return;
        }
        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                "Nguyen Minh Khoi - 0987654321",
                "123 Nguyen Trai, Phuong 2, Quan 5, Thanh pho Ho Chi Minh",
                true
        ));
        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                "Nguyen Minh Khoi - 0987654321",
                "456 Tran Hung Dao, Phuong Cau Ong Lanh, Quan 1, Thanh pho Ho Chi Minh",
                false
        ));
        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                "Nguyen Minh Khoi - 0987654321",
                "88 Nguyen Van Linh, Phuong Tan Phong, Quan 7, Thanh pho Ho Chi Minh",
                false
        ));
        selectedLocationIndex = 0;
    }

    private void openAddAddressForm() {
        addAddressLauncher.launch(new Intent(this, AddressFormActivity.class));
    }

    private void addLocationFromResult(Intent data) {
        String name = data.getStringExtra(AddressFormActivity.EXTRA_NAME);
        String phone = data.getStringExtra(AddressFormActivity.EXTRA_PHONE);
        String detail = data.getStringExtra(AddressFormActivity.EXTRA_DETAIL);
        String ward = data.getStringExtra(AddressFormActivity.EXTRA_WARD);
        String district = data.getStringExtra(AddressFormActivity.EXTRA_DISTRICT);
        String city = data.getStringExtra(AddressFormActivity.EXTRA_CITY);

        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                joinNameAndPhone(name, phone),
                joinAddress(detail, ward, district, city),
                false
        ));
        selectedLocationIndex = locationItems.size() - 1;
    }

    private String joinNameAndPhone(String name, String phone) {
        String safeName = name == null ? "" : name.trim();
        String safePhone = phone == null ? "" : phone.trim();
        if (safeName.isEmpty()) {
            return safePhone;
        }
        if (safePhone.isEmpty()) {
            return safeName;
        }
        return safeName + " - " + safePhone;
    }

    private String joinAddress(String detail, String ward, String district, String city) {
        List<String> parts = new ArrayList<>();
        addIfHasText(parts, detail);
        addIfHasText(parts, ward);
        addIfHasText(parts, district);
        addIfHasText(parts, city);
        return String.join(", ", parts);
    }

    private void addIfHasText(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value.trim());
        }
    }

    private void applySelectedLocationToAddressCard() {
        if (selectedLocationIndex < 0 || selectedLocationIndex >= locationItems.size()) {
            return;
        }
        LocationOptionAdapter.LocationItemUiModel selectedItem = locationItems.get(selectedLocationIndex);
        String[] nameAndPhone = splitNameAndPhone(selectedItem.name);
        tvAddressName.setText(nameAndPhone[0]);
        tvAddressPhone.setText(nameAndPhone[1]);
        tvAddressDetail.setText(selectedItem.address);
        tvAddressDefaultBadge.setVisibility(selectedItem.isDefault ? View.VISIBLE : View.GONE);
    }

    private String[] splitNameAndPhone(String value) {
        String safeValue = value == null ? "" : value;
        String[] parts = safeValue.split(" - ", 2);
        if (parts.length == 2) {
            return parts;
        }
        return new String[]{safeValue, ""};
    }
}
