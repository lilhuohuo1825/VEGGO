package com.veggo.app.presentation.order;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.LocationOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.profile.AddressFormActivity;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateRecurringOrderActivity extends BaseActivity {
    public static final String EXTRA_EDIT_RECURRING_ORDER_ID = "extra_edit_recurring_order_id";

    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    private final TextView[] frequencyButtons = new TextView[5];
    private final List<LocationOptionAdapter.LocationItemUiModel> locationItems = new ArrayList<>();
    private final List<RecurringOrderStore.RecurringProductItem> selectedProductItems = new ArrayList<>();
    private final Gson gson = new Gson();
    private EditText nameInput;
    private TextView addressNameText;
    private TextView addressLineText;
    private TextView deliveryDayText;
    private TextView deliverySlotText;
    private LinearLayout productContainer;
    private TextView estimateAmountText;
    private TextView carbonText;
    private AddressRepository addressRepository;
    private AppPreferences preferences;
    private RecurringOrderStore store;
    private RecurringOrderStore.RecurringOrder editingOrder;
    private int selectedLocationIndex;
    private String selectedReceiverName = "";
    private String selectedReceiverPhone = "";
    private String selectedAddressLine = "";
    private String selectedCity = "";
    private String selectedDistrict = "";
    private String selectedWard = "";
    private String selectedDetailAddress = "";
    private long estimatedTotal;
    private double estimatedCarbonPoints;
    private String selectedFrequency = "Tuần";
    private String selectedDeliveryDate;
    private String selectedDeliverySlot = "07:30 - 09:30";

    private final ActivityResultLauncher<Intent> productPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                bindSelectedProduct(result.getData());
            });

    private final ActivityResultLauncher<Intent> addAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAddresses();
                    showLocationDialog();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recurring_order);
        preferences = new AppPreferences(this);
        store = new RecurringOrderStore(this);
        addressRepository = AppModule.provideAddressRepository(this);
        bindViews();
        setupDefaults();
        setupFrequencyButtons();
        loadEditingOrderIfNeeded();
        loadAddresses();

        findViewById(R.id.createRecurringBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.createRecurringAddressBookButton).setOnClickListener(v -> showLocationDialog());
        findViewById(R.id.createRecurringSelectedAddressCard).setOnClickListener(v -> showLocationDialog());
        findViewById(R.id.createRecurringChooseProductButton).setOnClickListener(v -> openProductPicker());
        deliveryDayText.setOnClickListener(v -> showDeliveryDatePicker());
        deliverySlotText.setOnClickListener(v -> showDeliverySlotPicker());
        findViewById(R.id.createRecurringScheduleButton).setOnClickListener(v -> createRecurringOrder());
    }

    private void bindViews() {
        nameInput = findViewById(R.id.createRecurringName);
        addressNameText = findViewById(R.id.createRecurringAddressName);
        addressLineText = findViewById(R.id.createRecurringAddressLine);
        deliveryDayText = findViewById(R.id.createRecurringDeliveryDay);
        deliverySlotText = findViewById(R.id.createRecurringDeliverySlot);
        productContainer = findViewById(R.id.createRecurringProductContainer);
        estimateAmountText = findViewById(R.id.createRecurringEstimateAmountText);
        carbonText = findViewById(R.id.createRecurringCarbonText);
        frequencyButtons[0] = findViewById(R.id.createRecurringFrequencyDay);
        frequencyButtons[1] = findViewById(R.id.createRecurringFrequencyWeek);
        frequencyButtons[2] = findViewById(R.id.createRecurringFrequencyMonth);
        frequencyButtons[3] = findViewById(R.id.createRecurringFrequencyQuarter);
        frequencyButtons[4] = findViewById(R.id.createRecurringFrequencyYear);
    }

    private void setupDefaults() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        selectedDeliveryDate = storageFormat.format(calendar.getTime());
        deliveryDayText.setText(displayFormat.format(calendar.getTime()));
        deliverySlotText.setText(selectedDeliverySlot);
        updateEstimateText();
    }

    private void setupFrequencyButtons() {
        for (TextView button : frequencyButtons) {
            button.setOnClickListener(v -> selectFrequency(((TextView) v).getText().toString()));
        }
        selectFrequency(selectedFrequency);
    }

    private void selectFrequency(String frequency) {
        selectedFrequency = frequency;
        for (TextView button : frequencyButtons) {
            boolean selected = frequency.equals(button.getText().toString());
            button.setBackgroundResource(selected ? R.drawable.bg_order_primary_button : R.drawable.bg_recurring_input);
            button.setTextColor(getColor(selected ? R.color.background_main : R.color.neutral_100));
        }
    }

    private void showDeliveryDatePicker() {
        Calendar initial = Calendar.getInstance();
        Date selectedDate = parseDate(selectedDeliveryDate);
        if (selectedDate != null) {
            initial.setTime(selectedDate);
        }
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    selectedDeliveryDate = storageFormat.format(picked.getTime());
                    deliveryDayText.setText(displayFormat.format(picked.getTime()));
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showDeliverySlotPicker() {
        Calendar initial = Calendar.getInstance();
        android.app.TimePickerDialog dialog = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    int endHour = (hourOfDay + 2) % 24;
                    selectedDeliverySlot = String.format(
                            Locale.getDefault(),
                            "%02d:%02d - %02d:%02d",
                            hourOfDay,
                            minute,
                            endHour,
                            minute
                    );
                    deliverySlotText.setText(selectedDeliverySlot);
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
        );
        dialog.show();
    }

    private void loadAddresses() {
        String customerId = preferences.getCustomerId();
        if (isBlank(customerId) || addressRepository == null) {
            addFallbackLocation();
            applySelectedLocation();
            return;
        }
        addressRepository.getAddresses(customerId, new AddressRepository.Callback<List<Address>>() {
            @Override
            public void onSuccess(List<Address> addresses) {
                locationItems.clear();
                selectedLocationIndex = 0;
                if (addresses != null) {
                    for (int i = 0; i < addresses.size(); i++) {
                        Address address = addresses.get(i);
                        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                                joinNameAndPhone(address.getName(), address.getPhone()),
                                joinAddress(address.getDetail(), address.getWard(), address.getDistrict(), address.getProvince()),
                                address.isDefault()
                        ));
                        if (address.isDefault()) {
                            selectedLocationIndex = i;
                        }
                    }
                }
                if (locationItems.isEmpty()) {
                    addFallbackLocation();
                }
                if (isBlank(selectedAddressLine)) {
                    applySelectedLocation();
                } else {
                    addressNameText.setText(joinNameAndPhone(selectedReceiverName, selectedReceiverPhone));
                    addressLineText.setText(selectedAddressLine);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (locationItems.isEmpty()) {
                    addFallbackLocation();
                }
                if (isBlank(selectedAddressLine)) {
                    applySelectedLocation();
                }
            }
        });
    }

    private void loadEditingOrderIfNeeded() {
        String editOrderId = getIntent().getStringExtra(EXTRA_EDIT_RECURRING_ORDER_ID);
        if (isBlank(editOrderId)) {
            return;
        }
        editingOrder = store.findById(editOrderId);
        if (editingOrder == null) {
            Toast.makeText(this, "Không tìm thấy đơn định kỳ", Toast.LENGTH_SHORT).show();
            return;
        }
        nameInput.setText(editingOrder.name);
        selectedFrequency = editingOrder.frequency;
        selectedDeliveryDate = editingOrder.deliveryDate;
        selectedDeliverySlot = editingOrder.deliverySlot;
        selectedReceiverName = defaultIfBlank(editingOrder.receiverName, "Người nhận");
        selectedReceiverPhone = defaultIfBlank(editingOrder.receiverPhone, "");
        selectedCity = defaultIfBlank(editingOrder.city, "");
        selectedDistrict = defaultIfBlank(editingOrder.district, "");
        selectedWard = defaultIfBlank(editingOrder.ward, "");
        selectedDetailAddress = defaultIfBlank(editingOrder.detailAddress, "");
        selectedAddressLine = editingOrder.displayAddress();

        deliverySlotText.setText(selectedDeliverySlot);
        Date date = parseDate(selectedDeliveryDate);
        deliveryDayText.setText(date == null ? selectedDeliveryDate : displayFormat.format(date));
        selectFrequency(selectedFrequency);
        addressNameText.setText(joinNameAndPhone(selectedReceiverName, selectedReceiverPhone));
        addressLineText.setText(selectedAddressLine);

        selectedProductItems.clear();
        if (editingOrder.items != null) {
            selectedProductItems.addAll(editingOrder.items);
        }
        recalculateSelectedProducts();
        renderSelectedProducts();
        updateEstimateText();
        ((TextView) findViewById(R.id.createRecurringScheduleButton)).setText("Cập nhật");
    }

    private void showLocationDialog() {
        if (locationItems.isEmpty()) {
            addFallbackLocation();
        }
        android.view.View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvLocationOptions);
        TextView btnCancelLocation = dialogView.findViewById(R.id.btnCancelLocation);
        TextView btnConfirmLocation = dialogView.findViewById(R.id.btnConfirmLocation);
        TextView btnAddLocation = dialogView.findViewById(R.id.btnAddLocation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new LocationOptionAdapter(locationItems, selectedLocationIndex, item ->
                selectedLocationIndex = locationItems.indexOf(item)
        ));

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnCancelLocation.setOnClickListener(v -> dialog.dismiss());
        btnConfirmLocation.setOnClickListener(v -> {
            applySelectedLocation();
            dialog.dismiss();
        });
        btnAddLocation.setOnClickListener(v -> {
            dialog.dismiss();
            addAddressLauncher.launch(new Intent(this, AddressFormActivity.class));
        });
        dialog.show();
    }

    private BottomSheetDialog createBottomSheetDialog(android.view.View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                BottomSheetBehavior<android.widget.FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    private void addFallbackLocation() {
        locationItems.clear();
        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                joinNameAndPhone(defaultIfBlank(preferences.getFullName(), "Người nhận"), preferences.getCurrentPhone()),
                "Vui lòng chọn hoặc thêm địa chỉ giao hàng",
                true
        ));
        selectedLocationIndex = 0;
    }

    private void applySelectedLocation() {
        if (selectedLocationIndex < 0 || selectedLocationIndex >= locationItems.size()) {
            selectedLocationIndex = 0;
        }
        LocationOptionAdapter.LocationItemUiModel item = locationItems.get(selectedLocationIndex);
        selectedAddressLine = item.address;
        String[] nameParts = item.name.split("\\s*-\\s*", 2);
        selectedReceiverName = nameParts.length > 0 ? nameParts[0] : "";
        selectedReceiverPhone = nameParts.length > 1 ? nameParts[1] : "";

        String[] addressParts = item.address.split("\\s*,\\s*");
        selectedDetailAddress = addressParts.length > 0 ? addressParts[0] : "";
        selectedWard = addressParts.length > 1 ? addressParts[1] : "";
        selectedDistrict = addressParts.length > 2 ? addressParts[2] : "";
        selectedCity = addressParts.length > 3 ? addressParts[3] : "";

        addressNameText.setText(item.name);
        addressLineText.setText(item.address);
    }

    private void openProductPicker() {
        productPickerLauncher.launch(new Intent(this, RecurringProductPickerActivity.class));
    }

    private void bindSelectedProduct(Intent data) {
        String json = data.getStringExtra(RecurringProductPickerActivity.EXTRA_SELECTED_PRODUCTS_JSON);
        Type type = new TypeToken<List<RecurringOrderStore.RecurringProductItem>>() {}.getType();
        List<RecurringOrderStore.RecurringProductItem> items = gson.fromJson(json, type);
        selectedProductItems.clear();
        if (items != null) {
            selectedProductItems.addAll(items);
        }
        recalculateSelectedProducts();
        renderSelectedProducts();
        updateEstimateText();
    }

    private void renderSelectedProducts() {
        productContainer.removeAllViews();
        if (selectedProductItems.isEmpty()) {
            return;
        }
        for (RecurringOrderStore.RecurringProductItem item : selectedProductItems) {
            productContainer.addView(createProductCard(item));
        }
    }

    private View createProductCard(RecurringOrderStore.RecurringProductItem item) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_profile_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(64),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        row.addView(card);

        ImageView image = new ImageView(this);
        image.setBackgroundResource(R.drawable.bg_order_product_thumb);
        image.setPadding(dp(6), dp(6), dp(6), dp(6));
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_leaf)
                .error(R.drawable.ic_leaf)
                .into(image);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        card.addView(image, imageParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        contentParams.setMarginStart(dp(12));
        card.addView(content, contentParams);

        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextColor(getColor(R.color.neutral_100));
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(name);

        TextView meta = new TextView(this);
        meta.setText("Phân loại: " + defaultIfBlank(item.unit, "1 phần")
                + "  •  Số lượng: " + item.quantity);
        meta.setTextColor(getColor(R.color.neutral_70));
        meta.setTextSize(13);
        meta.setPadding(0, dp(8), 0, 0);
        content.addView(meta);

        TextView total = new TextView(this);
        total.setText("Thành tiền: " + formatCurrency(item.totalPrice())
                + " • +" + formatCarbon(item.carbonPoints) + " điểm carbon");
        total.setTextColor(getColor(R.color.primary_hover));
        total.setTextSize(14);
        total.setTypeface(null, android.graphics.Typeface.BOLD);
        total.setPadding(0, dp(8), 0, 0);
        content.addView(total);

        TextView delete = new TextView(this);
        delete.setText("Xoá");
        delete.setGravity(Gravity.CENTER);
        delete.setTextColor(getColor(R.color.white));
        delete.setTextSize(14);
        delete.setTypeface(null, android.graphics.Typeface.BOLD);
        delete.setBackgroundColor(getColor(R.color.danger_main));
        delete.setOnClickListener(v -> confirmDeleteProduct(item));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.MATCH_PARENT);
        deleteParams.topMargin = dp(10);
        row.addView(delete, deleteParams);
        return scrollView;
    }

    private void confirmDeleteProduct(RecurringOrderStore.RecurringProductItem item) {
        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                "Xác nhận xoá sản phẩm",
                "Bạn có chắc chắn muốn xoá sản phẩm này khỏi đơn định kỳ?",
                "Xoá",
                "Huỷ",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                    selectedProductItems.remove(item);
                    recalculateSelectedProducts();
                    renderSelectedProducts();
                    updateEstimateText();
                    }
                }
        );
    }

    private void recalculateSelectedProducts() {
        estimatedTotal = 0;
        estimatedCarbonPoints = 0;
        for (RecurringOrderStore.RecurringProductItem item : selectedProductItems) {
            estimatedTotal += item.totalPrice();
            estimatedCarbonPoints += item.carbonPoints;
        }
    }

    private void createRecurringOrder() {
        if (!preferences.isLoggedIn() || isBlank(preferences.getCustomerId())) {
            Toast.makeText(this, "Vui lòng đăng nhập để tạo đơn định kỳ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isBlank(textOf(nameInput))) {
            Toast.makeText(this, "Vui lòng nhập tên đơn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isBlank(selectedAddressLine) || selectedAddressLine.contains("Vui lòng chọn")) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProductItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        RecurringOrderStore.RecurringOrder order = editingOrder != null ? editingOrder : new RecurringOrderStore.RecurringOrder();
        if (editingOrder == null) {
            order.id = "DK" + System.currentTimeMillis();
            order.createdAt = storageFormat.format(new Date());
        }
        order.customerId = preferences.getCustomerId();
        order.name = textOf(nameInput);
        order.frequency = selectedFrequency;
        order.deliveryDate = selectedDeliveryDate;
        order.deliverySlot = selectedDeliverySlot;
        order.receiverName = selectedReceiverName;
        order.receiverPhone = selectedReceiverPhone;
        order.city = selectedCity;
        order.district = selectedDistrict;
        order.ward = selectedWard;
        order.detailAddress = selectedDetailAddress;
        order.itemSummary = buildItemSummary();
        order.estimatedTotal = estimatedTotal;
        order.carbonPoints = estimatedCarbonPoints;
        order.items = new ArrayList<>(selectedProductItems);
        order.status = "Active";

        if (editingOrder == null) {
            store.add(order);
            Toast.makeText(this, "Đã tạo đơn định kỳ", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RecurringOrdersActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            store.update(order);
            Toast.makeText(this, "Đã cập nhật đơn định kỳ", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private String buildItemSummary() {
        List<String> names = new ArrayList<>();
        for (RecurringOrderStore.RecurringProductItem item : selectedProductItems) {
            names.add(item.name + " x" + item.quantity);
        }
        return String.join(", ", names);
    }

    private Date parseDate(String value) {
        try {
            return storageFormat.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String joinNameAndPhone(String name, String phone) {
        String safeName = name == null ? "" : name.trim();
        String safePhone = phone == null ? "" : phone.trim();
        if (safeName.isEmpty()) return safePhone;
        if (safePhone.isEmpty()) return safeName;
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
        if (!isBlank(value)) {
            parts.add(value.trim());
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private void updateEstimateText() {
        estimateAmountText.setText("Dự kiến " + formatCurrency(estimatedTotal) + "/lần");
        carbonText.setText("+" + formatCarbon(estimatedCarbonPoints) + " điểm carbon");
    }

    private String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }

    private String formatCarbon(double points) {
        if (Math.abs(points - Math.round(points)) < 0.0001) {
            return String.format(Locale.US, "%.0f", points);
        }
        return String.format(Locale.US, "%.1f", points);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
