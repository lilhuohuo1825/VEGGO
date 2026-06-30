package com.veggo.app.presentation.checkout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.PaymentItemAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.address.AddressTreeLoader;
import com.veggo.app.core.address.VietnamAddressTree;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.notification.EmulatorSmsSender;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.OrderDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutGuestActivity extends BaseActivity {
    private static final long DEFAULT_SHIPPING_FEE = 30_000L;
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String NAME_REGEX = "^[\\p{L}\\s'.-]{2,}$";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MS = 60_000;
    private static final int MAX_OTP_ATTEMPTS = 3;

    private EditText edtGuestName;
    private EditText edtGuestPhone;
    private EditText edtGuestAddressDetail;
    private TextView tvCheckoutNote;
    private TextView tvCheckoutSubtotal;
    private TextView tvCheckoutVoucherDiscount;
    private TextView tvCheckoutProductDiscount;
    private TextView tvCheckoutShippingFee;
    private TextView tvCheckoutShippingDiscount;
    private TextView tvCheckoutPaymentTotal;
    private TextView tvCheckoutBottomTotal;
    private TextView tvCheckoutCarbonPoints;
    private TextView tvCheckoutSelectedVoucher;
    private TextView tvScheduleDeliveryTime;
    private TextView tvGuestCity;
    private TextView tvGuestDistrict;
    private TextView tvGuestWard;
    private View layoutFastDelivery;
    private View layoutScheduleDelivery;
    private AppCompatRadioButton radioPaymentMomo;
    private AppCompatRadioButton radioPaymentBank;
    private AppCompatRadioButton radioPaymentCod;
    private PaymentItemAdapter paymentItemAdapter;
    private final List<CartDto.CartItemDto> cartItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> voucherItems = new ArrayList<>();
    private long currentSubtotal;
    private long currentProductDiscount;
    private long currentVoucherDiscount;
    private long currentShippingFee;
    private long currentShippingDiscount;
    private long currentPaymentTotal;
    private double currentCarbonPoints;
    private double currentCarbonEmission;
    private int currentProductCount;
    private VoucherOptionAdapter.VoucherItemUiModel selectedVoucher;
    private String selectedPaymentMethod = "momo";
    private String guestId;
    private boolean buyNowMode;
    private boolean isSearchingVoucher = false;
    private VietnamAddressTree addressTree;
    private String selectedProvince;
    private String selectedDistrict;
    private String selectedWard;
    private String checkedPhone = "";
    private boolean checkingPhone;
    private boolean phoneRequiresDifferentNumber;
    private UserDto existingAccountUser;
    private boolean existingAccountVerified;
    private String currentAccountOtp;
    private long accountOtpExpiresAt;
    private int accountOtpFailedAttempts;
    private CountDownTimer accountOtpTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_guest);

        PendingCheckoutStore store = new PendingCheckoutStore(this);
        guestId = store.guestId();
        buyNowMode = getIntent().getBooleanExtra(CheckoutActivity.EXTRA_BUY_NOW, false);

        edtGuestName = findViewById(R.id.edtGuestName);
        edtGuestPhone = findViewById(R.id.edtGuestPhone);
        edtGuestAddressDetail = findViewById(R.id.edtGuestAddressDetail);
        tvCheckoutNote = findViewById(R.id.tvCheckoutNote);
        tvCheckoutSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvCheckoutVoucherDiscount = findViewById(R.id.tvCheckoutVoucherDiscount);
        tvCheckoutProductDiscount = findViewById(R.id.tvCheckoutProductDiscount);
        tvCheckoutShippingFee = findViewById(R.id.tvCheckoutShippingFee);
        tvCheckoutShippingDiscount = findViewById(R.id.tvCheckoutShippingDiscount);
        tvCheckoutPaymentTotal = findViewById(R.id.tvCheckoutPaymentTotal);
        tvCheckoutBottomTotal = findViewById(R.id.tvCheckoutBottomTotal);
        tvCheckoutCarbonPoints = findViewById(R.id.tvCheckoutCarbonPoints);
        tvCheckoutSelectedVoucher = findViewById(R.id.tvCheckoutSelectedVoucher);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        tvGuestCity = findViewById(R.id.tvGuestCity);
        tvGuestDistrict = findViewById(R.id.tvGuestDistrict);
        tvGuestWard = findViewById(R.id.tvGuestWard);
        layoutFastDelivery = findViewById(R.id.layoutFastDelivery);
        layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);
        radioPaymentMomo = findViewById(R.id.radioPaymentMomo);
        radioPaymentBank = findViewById(R.id.radioPaymentCard);
        radioPaymentCod = findViewById(R.id.radioPaymentCod);

        RecyclerView rvCheckoutProducts = findViewById(R.id.rvCheckoutProducts);
        rvCheckoutProducts.setLayoutManager(new LinearLayoutManager(this));
        paymentItemAdapter = new PaymentItemAdapter(new ArrayList<>());
        rvCheckoutProducts.setAdapter(paymentItemAdapter);

        setupPaymentMethods();
        setupGuestInputValidation();
        setupAddressPickers();
        selectDeliveryMode(true);
        View btnBack = findViewById(R.id.checkoutBackButton);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        findViewById(R.id.layoutDeliveryOrigin).setOnClickListener(v -> showDeliveryOriginDialog());
        findViewById(R.id.layoutVoucher).setOnClickListener(v -> showVoucherDialog());
        findViewById(R.id.layoutNote).setOnClickListener(v -> showNoteDialog());
        layoutFastDelivery.setOnClickListener(v -> selectDeliveryMode(true));
        layoutScheduleDelivery.setOnClickListener(v -> {
            selectDeliveryMode(false);
            showScheduleTimePicker();
        });
        findViewById(R.id.btnPlaceOrder).setOnClickListener(v -> validatePhoneAndCreateOrder());
        restoreSelectedVoucherFromIntent();
        prefetchVoucherData();
        if (buyNowMode) {
            bindBuyNowItem();
        } else {
            loadGuestCart();
        }
    }

    private void setupGuestInputValidation() {
        edtGuestName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateGuestName();
            }
        });
        edtGuestName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isValidGuestName(s.toString())) {
                    edtGuestName.setError(null);
                }
            }
        });
        edtGuestPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkGuestPhoneAfterInput();
            }
        });
        edtGuestPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!s.toString().trim().equals(checkedPhone)) {
                    checkedPhone = "";
                    phoneRequiresDifferentNumber = false;
                    existingAccountUser = null;
                    existingAccountVerified = false;
                }
                if (s.length() == 10) {
                    validateGuestPhoneFormat();
                }
            }
        });
        edtGuestAddressDetail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateGuestAddressDetail();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (accountOtpTimer != null) {
            accountOtpTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EmulatorSmsSender.handlePermissionResult(this, requestCode, grantResults);
    }

    private void setupPaymentMethods() {
        selectPaymentMethod("momo");
        radioPaymentMomo.setOnClickListener(v -> selectPaymentMethod("momo"));
        radioPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        radioPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        View layoutPaymentMomo = findViewById(R.id.layoutPaymentMomo);
        View layoutPaymentCod = findViewById(R.id.layoutPaymentCod);
        View layoutPaymentBank = findViewById(R.id.layoutPaymentBank);
        if (layoutPaymentMomo != null) {
            layoutPaymentMomo.setOnClickListener(v -> selectPaymentMethod("momo"));
        }
        if (layoutPaymentCod != null) {
            layoutPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        }
        if (layoutPaymentBank != null) {
            layoutPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        }
    }

    private void selectPaymentMethod(String method) {
        selectedPaymentMethod = method;
        radioPaymentMomo.setChecked("momo".equals(method));
        radioPaymentBank.setChecked("bank".equals(method));
        radioPaymentCod.setChecked("cod".equals(method));
    }

    private void loadGuestCart() {
        ApiClient.createService(CartApi.class).getCart(guestId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                bindCart(response.body());
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                Toast.makeText(CheckoutGuestActivity.this, "Không thể tải giỏ hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBuyNowItem() {
        ProductDto product = new ProductDto();
        product.setId(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_PRODUCT_ID));
        product.setSku(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_SKU));
        product.setProductName(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_NAME));
        product.setPrice(getIntent().getLongExtra(CheckoutActivity.EXTRA_BUY_NOW_PRICE, 0));
        product.setOriginalPrice(getIntent().getLongExtra(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE, 0));
        product.setImageUrl(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_IMAGE));
        product.setWeight(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_WEIGHT));
        product.setCategoryId(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_CATEGORY_ID));
        product.setSubcategoryId(getIntent().getStringExtra(CheckoutActivity.EXTRA_BUY_NOW_SUBCATEGORY_ID));
        product.setCarbonSavingPoint(getIntent().getDoubleExtra(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT, 0));
        boolean hasWeightOptions = getIntent().getBooleanExtra(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS, false);
        double selectedWeight = getIntent().getDoubleExtra(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT, 1.0);
        if (hasWeightOptions) {
            List<Double> weights = new ArrayList<>();
            weights.add(selectedWeight > 0 ? selectedWeight : 1.0);
            product.setWeightOptions(weights);
        }

        CartDto.CartItemDto item = new CartDto.CartItemDto();
        item.setSku(product.getSku());
        item.setProduct(product);
        item.setQuantity(Math.max(1, getIntent().getIntExtra(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY, 1)));
        item.setSelectedWeight(selectedWeight > 0 ? selectedWeight : 1.0);
        CartDto cart = new CartDto();
        List<CartDto.CartItemDto> items = new ArrayList<>();
        items.add(item);
        cart.setItems(items);
        bindCart(cart);
    }

    private void bindCart(CartDto cart) {
        cartItems.clear();
        currentSubtotal = 0;
        currentProductDiscount = 0;
        currentCarbonPoints = 0;
        currentCarbonEmission = 0;
        currentProductCount = 0;
        List<PaymentItemAdapter.PaymentItemUiModel> uiItems = new ArrayList<>();
        if (cart != null && cart.getItems() != null) {
            for (CartDto.CartItemDto item : cart.getItems()) {
                ProductDto product = item.getProduct();
                long price = product != null ? product.getPrice() : item.getPrice();
                long originalPrice = product != null ? product.getOriginalPrice() : item.getOriginalPrice();
                if (originalPrice < price) originalPrice = price;
                int quantity = Math.max(1, item.getQuantity());
                boolean hasWeightOptions = product != null
                        && product.getWeightOptions() != null
                        && !product.getWeightOptions().isEmpty();
                double selectedWeight = item.getSelectedWeight() > 0 ? item.getSelectedWeight() : 1.0;
                long variantPrice = variantPrice(price, selectedWeight, hasWeightOptions);
                long variantOriginalPrice = variantPrice(originalPrice, selectedWeight, hasWeightOptions);
                currentSubtotal += variantPrice * quantity;
                currentProductDiscount += Math.max(0, variantOriginalPrice - variantPrice) * quantity;
                currentProductCount += quantity;
                if (product != null) {
                    double itemCarbonEmission = calculateCarbonEmission(product, quantity, selectedWeight, hasWeightOptions);
                    currentCarbonEmission += itemCarbonEmission;
                    currentCarbonPoints += itemCarbonEmission * 10;
                }
                cartItems.add(item);
                uiItems.add(new PaymentItemAdapter.PaymentItemUiModel(
                        product != null ? product.getProductName() : "Sản phẩm VEGGO",
                        resolveItemUnit(product, selectedWeight, hasWeightOptions),
                        variantPrice,
                        quantity,
                        R.drawable.ic_vegetable,
                        product != null ? product.getFirstImage() : ""
                ));
            }
        }
        paymentItemAdapter.setItems(uiItems);
        validateAppliedVoucher();
        bindPaymentSummary();
    }

    private void bindPaymentSummary() {
        long voucherDiscount = 0;
        long shippingFee = currentSubtotal > 0 ? DEFAULT_SHIPPING_FEE : 0;
        long shippingDiscount = currentSubtotal >= 300_000L ? shippingFee : 0;
        if (selectedVoucher != null && selectedVoucher.promotion != null) {
            if (isShippingPromotion(selectedVoucher.promotion)) {
                shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount,
                        calculatePromotionDiscount(selectedVoucher.promotion, shippingFee)));
            } else {
                voucherDiscount = Math.min(currentSubtotal,
                        calculatePromotionDiscount(selectedVoucher.promotion, currentSubtotal));
            }
        }
        currentVoucherDiscount = voucherDiscount;
        currentShippingFee = shippingFee;
        currentShippingDiscount = shippingDiscount;
        currentPaymentTotal = Math.max(0, currentSubtotal + shippingFee - voucherDiscount - shippingDiscount);
        tvCheckoutSubtotal.setText(formatCurrency(currentSubtotal));
        tvCheckoutVoucherDiscount.setText(formatDiscount(currentVoucherDiscount));
        tvCheckoutProductDiscount.setText(formatDiscount(currentProductDiscount));
        tvCheckoutShippingFee.setText(formatCurrency(currentShippingFee));
        tvCheckoutShippingDiscount.setText(formatDiscount(currentShippingDiscount));
        tvCheckoutPaymentTotal.setText(formatCurrency(currentPaymentTotal));
        tvCheckoutBottomTotal.setText(formatCurrency(currentPaymentTotal));
        setCarbonPointsText(currentCarbonPoints);
    }

    private void validatePhoneAndCreateOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedVoucher != null) {
            PromotionTargetDto target = null;
            for (PromotionTargetDto t : cachedTargets) {
                if (selectedVoucher.promotionId != null && selectedVoucher.promotionId.equals(t.getPromotionId())) {
                    target = t;
                    break;
                }
            }
            PromotionUsageDto usage = null;
            for (PromotionUsageDto u : cachedUsages) {
                if (selectedVoucher.promotionId != null && selectedVoucher.promotionId.equals(u.getPromotionId())) {
                    usage = u;
                    break;
                }
            }
            String reason = disabledReason(selectedVoucher.promotion, target, usage);
            if (reason != null) {
                Toast.makeText(this, "Voucher không còn hợp lệ: " + reason, Toast.LENGTH_LONG).show();
                selectedVoucher = null;
                updateSelectedVoucherUi();
                bindPaymentSummary();
                return;
            }
        }
        if (!validateGuestInputs()) {
            return;
        }
        String phone = edtGuestPhone.getText().toString().trim();
        if (checkingPhone) {
            Toast.makeText(this, "Đang kiểm tra số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneRequiresDifferentNumber) {
            edtGuestPhone.setError("Vui lòng dùng số điện thoại khác");
            edtGuestPhone.requestFocus();
            return;
        }
        if (existingAccountUser != null && phone.equals(checkedPhone)) {
            if (existingAccountVerified) {
                continueCheckoutWithAccount(existingAccountUser);
            } else {
                showExistingAccountDialog(existingAccountUser, true);
            }
            return;
        }
        ApiClient.createService(UserApi.class).getUserByPhone(phone).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    checkedPhone = phone;
                    existingAccountUser = response.body();
                    existingAccountVerified = false;
                    showExistingAccountDialog(response.body(), true);
                } else {
                    checkedPhone = phone;
                    existingAccountUser = null;
                    createOrder(guestId, true);
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                checkedPhone = phone;
                existingAccountUser = null;
                createOrder(guestId, true);
            }
        });
    }

    private boolean validateGuestInputs() {
        if (!validateGuestName()) {
            return false;
        }
        if (!validateGuestPhoneFormat()) {
            return false;
        }
        if (selectedProvince == null || selectedProvince.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedDistrict == null || selectedDistrict.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn Quận/Huyện", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedWard == null || selectedWard.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!validateGuestAddressDetail()) {
            return false;
        }
        return true;
    }

    private boolean validateGuestName() {
        String name = edtGuestName.getText().toString().trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            edtGuestName.setError("Vui lòng nhập họ tên");
            return false;
        }
        if (!isValidGuestName(name)) {
            edtGuestName.setError("Họ tên không được chứa số/ký tự lạ");
            return false;
        }
        edtGuestName.setError(null);
        return true;
    }

    private boolean isValidGuestName(String name) {
        String normalizedName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        return normalizedName.matches(NAME_REGEX);
    }

    private boolean validateGuestPhoneFormat() {
        String phone = edtGuestPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            edtGuestPhone.setError("Vui lòng nhập số điện thoại");
            return false;
        }
        if (!phone.matches(PHONE_REGEX)) {
            edtGuestPhone.setError("Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
            return false;
        }
        edtGuestPhone.setError(null);
        return true;
    }

    private boolean validateGuestAddressDetail() {
        String detail = edtGuestAddressDetail.getText().toString().trim();
        if (detail.isEmpty()) {
            edtGuestAddressDetail.setError("Vui lòng nhập địa chỉ giao hàng");
            return false;
        }
        if (detail.length() < 5 || !detail.matches(".*[\\p{L}\\d].*")) {
            edtGuestAddressDetail.setError("Địa chỉ cụ thể chưa hợp lệ");
            return false;
        }
        edtGuestAddressDetail.setError(null);
        return true;
    }

    private void checkGuestPhoneAfterInput() {
        if (!validateGuestPhoneFormat()) {
            return;
        }
        String phone = edtGuestPhone.getText().toString().trim();
        if (phone.equals(checkedPhone) || checkingPhone) {
            return;
        }
        checkingPhone = true;
        ApiClient.createService(UserApi.class).getUserByPhone(phone).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                checkingPhone = false;
                checkedPhone = phone;
                phoneRequiresDifferentNumber = false;
                existingAccountVerified = false;
                if (response.isSuccessful() && response.body() != null) {
                    existingAccountUser = response.body();
                    showExistingAccountDialog(existingAccountUser, false);
                    return;
                }
                existingAccountUser = null;
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                checkingPhone = false;
                checkedPhone = phone;
                existingAccountUser = null;
                phoneRequiresDifferentNumber = false;
                existingAccountVerified = false;
            }
        });
    }

    private void showExistingAccountDialog(UserDto user, boolean createOrderAfterVerify) {
        VeggoDialog.show(
                this,
                R.drawable.ic_phone,
                "Số điện thoại đã có tài khoản",
                "Số điện thoại này đã có tài khoản, bạn có muốn tiếp tục đặt hàng bằng tài khoản "
                        + safeText(user.getFullName(), user.getPhone()) + " không?",
                "Có",
                "Không",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        showAccountOtpDialog(user, createOrderAfterVerify);
                    }

                    @Override
                    public void onCancel() {
                        phoneRequiresDifferentNumber = true;
                        existingAccountVerified = false;
                        edtGuestPhone.setError("Vui lòng dùng số điện thoại khác");
                        edtGuestPhone.requestFocus();
                    }
                }
        );
    }

    private void showAccountOtpDialog(UserDto user, boolean createOrderAfterVerify) {
        currentAccountOtp = randomOtp();
        accountOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
        accountOtpFailedAttempts = 0;
        openAccountOtpDialog(user, createOrderAfterVerify);
    }

    private void openAccountOtpDialog(UserDto user, boolean createOrderAfterVerify) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_otp_veggo);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView otpMessage = dialog.findViewById(R.id.tvOtpMessage);
        TextView btnCancel = dialog.findViewById(R.id.btnOtpCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnOtpConfirm);
        TextView btnResend = dialog.findViewById(R.id.btnOtpResend);
        LinearLayout otpRow = dialog.findViewById(R.id.layoutOtpFields);
        EditText[] otpFields = new EditText[OTP_LENGTH];
        for (int i = 0; i < OTP_LENGTH; i++) {
            EditText field = new EditText(this);
            field.setGravity(android.view.Gravity.CENTER);
            field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            field.setTextColor(android.graphics.Color.parseColor("#1E1E1E"));
            field.setTextSize(18f);
            field.setBackgroundResource(R.drawable.bg_normal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(46));
            if (i > 0) {
                params.setMarginStart(dp(6));
            }
            otpRow.addView(field, params);
            otpFields[i] = field;
        }

        wireOtpFields(otpFields, () -> verifyExistingAccountOtp(user, createOrderAfterVerify, dialog, otpFields));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> verifyExistingAccountOtp(user, createOrderAfterVerify, dialog, otpFields));
        btnResend.setOnClickListener(v -> {
            currentAccountOtp = randomOtp();
            accountOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
            accountOtpFailedAttempts = 0;
            clearOtpFields(otpFields);
            btnResend.setVisibility(View.GONE);
            startAccountOtpTimer(otpMessage, btnResend);
            showOtpSmsOnDialog(dialog);
        });
        dialog.setOnDismissListener(dialogInterface -> {
            if (accountOtpTimer != null) {
                accountOtpTimer.cancel();
            }
        });
        dialog.show();
        startAccountOtpTimer(otpMessage, btnResend);
        showOtpSmsOnDialog(dialog);
        otpFields[0].requestFocus();
    }

    private void startAccountOtpTimer(TextView otpMessage, TextView btnResend) {
        if (accountOtpTimer != null) {
            accountOtpTimer.cancel();
        }
        accountOtpTimer = new CountDownTimer(OTP_TTL_MS, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                otpMessage.setText("Nhập mã OTP 6 số. Mã còn hiệu lực trong "
                        + Math.max(1, millisUntilFinished / 1000) + " giây.");
            }

            @Override public void onFinish() {
                otpMessage.setText("Mã xác thực đã hết hạn. Vui lòng gửi lại mã.");
                btnResend.setVisibility(View.VISIBLE);
                Toast.makeText(CheckoutGuestActivity.this, "Mã xác thực đã hết hạn", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void showOtpSmsOnDialog(Dialog dialog) {
        EmulatorSmsSender.send(
                this,
                "VEGGO: Ma OTP xac thuc tai khoan cua ban la " + currentAccountOtp
                        + ". Ma co hieu luc trong 60 giay."
        );
    }

    private void verifyExistingAccountOtp(UserDto user, boolean createOrderAfterVerify, Dialog dialog, EditText[] fields) {
        String otp = getOtpValue(fields);
        if (otp.length() < OTP_LENGTH) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() > accountOtpExpiresAt) {
            Toast.makeText(this, "Mã xác thực đã hết hạn. Vui lòng xác thực lại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (accountOtpFailedAttempts >= MAX_OTP_ATTEMPTS) {
            Toast.makeText(this, "Bạn đã nhập sai quá số lần cho phép. Vui lòng xác thực lại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentAccountOtp.equals(otp)) {
            accountOtpFailedAttempts++;
            clearOtpFields(fields);
            Toast.makeText(this,
                    "Mã xác thực không đúng (" + accountOtpFailedAttempts + "/" + MAX_OTP_ATTEMPTS + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        existingAccountVerified = true;
        phoneRequiresDifferentNumber = false;
        dialog.dismiss();
        continueCheckoutWithAccount(user);
    }

    private void continueCheckoutWithAccount(UserDto user) {
        String customerId = user.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tài khoản để tiếp tục thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        new AppPreferences(this).saveLoginSession(
                user.getPhone(),
                customerId,
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl()
        );
        Toast.makeText(this, "Xác thực thành công. Đang chuyển sang thanh toán tài khoản", Toast.LENGTH_SHORT).show();
        new PendingCheckoutStore(this).openAfterAuth(this, customerId);
        finish();
    }

    private void wireOtpFields(EditText[] fields, Runnable onComplete) {
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < fields.length - 1) {
                            fields[index + 1].requestFocus();
                        } else {
                            onComplete.run();
                        }
                    }
                }
            });
        }
    }

    private String getOtpValue(EditText[] fields) {
        StringBuilder otp = new StringBuilder();
        for (EditText field : fields) {
            otp.append(field.getText().toString().trim());
        }
        return otp.toString();
    }

    private void clearOtpFields(EditText[] fields) {
        for (EditText field : fields) {
            field.setText("");
        }
        fields[0].requestFocus();
    }

    private String randomOtp() {
        return String.format(Locale.US, "%06d", (int) (Math.random() * 1_000_000));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void createOrder(String customerId, boolean guestOrder) {
        ApiClient.createService(OrderApi.class).createOrder(buildOrderPayload(customerId, guestOrder))
                .enqueue(new Callback<OrderDto>() {
                    @Override
                    public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(CheckoutGuestActivity.this, "Không thể tạo đơn hàng", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        OrderDto order = response.body();
                        String orderId = order.getOrderId();
                        if (orderId == null || orderId.trim().isEmpty()) {
                            orderId = order.getId();
                        }
                        openPaymentStep(orderId, customerId, guestOrder);
                    }

                    @Override
                    public void onFailure(Call<OrderDto> call, Throwable t) {
                        Toast.makeText(CheckoutGuestActivity.this, "Không thể tạo đơn hàng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, Object> buildOrderPayload(String customerId, boolean guestOrder) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("CustomerID", customerId);
        payload.put("isGuestOrder", guestOrder);
        payload.put("paymentMethod", selectedPaymentMethod);
        payload.put("paymentStatus", "unpaid");
        payload.put("status", "pending");
        payload.put("subtotal", currentSubtotal);
        payload.put("shippingFee", currentShippingFee);
        payload.put("shippingDiscount", currentShippingDiscount);
        payload.put("discount", currentVoucherDiscount);
        payload.put("vatRate", 0);
        payload.put("vatAmount", 0);
        payload.put("totalAmount", currentPaymentTotal);
        payload.put("code", selectedVoucher != null ? safeText(selectedVoucher.promotionId) : "");
        payload.put("promotion_id", selectedVoucher != null ? safeText(selectedVoucher.promotionId) : null);
        payload.put("promotionName", selectedVoucher != null ? safeText(selectedVoucher.title) : "");
        payload.put("wantInvoice", false);
        payload.put("invoiceInfo", new HashMap<String, Object>());
        payload.put("consultantCode", "");
        payload.put("shippingInfo", buildShippingInfoPayload());
        payload.put("items", buildOrderItems());
        payload.put("CarbonPointEarned", Math.round(currentCarbonPoints));
        payload.put("TotalCarbonEmission", currentCarbonEmission);
        return payload;
    }

    private Map<String, Object> buildShippingInfoPayload() {
        Map<String, Object> shippingInfo = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("detail", edtGuestAddressDetail.getText().toString().trim());
        address.put("ward", safeText(selectedWard));
        address.put("district", safeText(selectedDistrict));
        address.put("city", safeText(selectedProvince));
        shippingInfo.put("fullName", edtGuestName.getText().toString().trim());
        shippingInfo.put("phone", edtGuestPhone.getText().toString().trim());
        shippingInfo.put("email", "");
        shippingInfo.put("address", address);
        shippingInfo.put("deliveryMethod", "standard");
        shippingInfo.put("notes", tvCheckoutNote.getText().toString().trim());
        shippingInfo.put("warehouse_id", "WH001");
        return shippingInfo;
    }

    private List<Map<String, Object>> buildOrderItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartDto.CartItemDto itemDto : cartItems) {
            ProductDto product = itemDto.getProduct();
            boolean hasWeightOptions = product != null
                    && product.getWeightOptions() != null
                    && !product.getWeightOptions().isEmpty();
            double selectedWeight = itemDto.getSelectedWeight() > 0 ? itemDto.getSelectedWeight() : 1.0;
            long unitPrice = product != null ? product.getPrice() : itemDto.getPrice();
            long originalPrice = product != null ? product.getOriginalPrice() : itemDto.getOriginalPrice();
            if (originalPrice < unitPrice) {
                originalPrice = unitPrice;
            }
            double carbonEmission = calculateCarbonEmission(product, itemDto.getQuantity(), selectedWeight, hasWeightOptions);
            long carbonPoints = Math.round(carbonEmission * 10);
            Map<String, Object> item = new HashMap<>();
            item.put("sku", itemDto.getSku());
            item.put("productName", product != null ? product.getProductName() : "Sản phẩm VEGGO");
            item.put("quantity", itemDto.getQuantity());
            item.put("price", variantPrice(unitPrice, selectedWeight, hasWeightOptions));
            item.put("originalPrice", variantPrice(originalPrice, selectedWeight, hasWeightOptions));
            item.put("image", product != null ? product.getFirstImage() : "");
            item.put("unit", resolveItemUnit(product, selectedWeight, hasWeightOptions));
            item.put("weight", resolveItemUnit(product, selectedWeight, hasWeightOptions));
            if (hasWeightOptions) {
                item.put("selectedWeight", selectedWeight);
            }
            item.put("itemType", "purchased");
            item.put("productId", product != null ? product.getId() : "");
            item.put("CategoryID", product != null ? product.getCategoryId() : "");
            item.put("SubcategoryID", product != null ? product.getSubcategoryId() : "");
            item.put("EmissionFactor", product != null ? product.getEmissionFactor() : 0);
            item.put("CarbonPointEarned", carbonPoints);
            item.put("TotalCarbonEmission", carbonEmission);
            items.add(item);
        }
        return items;
    }

    private void openPaymentStep(String orderId, String customerId, boolean guestOrder) {
        Intent intent = new Intent(this, QrPaymentActivity.class);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_METHOD, selectedPaymentMethod);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_AMOUNT, currentPaymentTotal);
        intent.putExtra(QrPaymentActivity.EXTRA_ITEM_COUNT, currentProductCount);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_CODE, "VG" + orderId);
        intent.putExtra(QrPaymentActivity.EXTRA_ORDER_ID, orderId);
        intent.putExtra(QrPaymentActivity.EXTRA_SHOW_SUCCESS_IMMEDIATELY, "cod".equals(selectedPaymentMethod) || "momo".equals(selectedPaymentMethod));
        intent.putExtra(QrPaymentActivity.EXTRA_CLEAR_CART_ON_SUCCESS, !buyNowMode);
        intent.putExtra(QrPaymentActivity.EXTRA_CUSTOMER_ID, customerId);
        intent.putExtra(QrPaymentActivity.EXTRA_IS_GUEST_ORDER, guestOrder);
        intent.putExtra(QrPaymentActivity.EXTRA_GUEST_PHONE, edtGuestPhone.getText().toString().trim());
        if (!buyNowMode) {
            ArrayList<String> skus = new ArrayList<>();
            double[] weights = new double[cartItems.size()];
            for (int i = 0; i < cartItems.size(); i++) {
                skus.add(cartItems.get(i).getSku());
                weights[i] = cartItems.get(i).getSelectedWeight() > 0 ? cartItems.get(i).getSelectedWeight() : 1.0;
            }
            intent.putStringArrayListExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_SKUS, skus);
            intent.putExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_WEIGHTS, weights);
        }
        startActivity(intent);
    }

    private void setupAddressPickers() {
        findViewById(R.id.layoutGuestCity).setOnClickListener(v -> showProvincePicker());
        findViewById(R.id.layoutGuestDistrict).setOnClickListener(v -> showDistrictPicker());
        findViewById(R.id.layoutGuestWard).setOnClickListener(v -> showWardPicker());
        loadAddressTree();
    }

    private void loadAddressTree() {
        new Thread(() -> {
            try {
                VietnamAddressTree tree = AddressTreeLoader.load();
                runOnUiThread(() -> addressTree = tree);
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Không thể tải dữ liệu Tỉnh/Huyện/Xã",
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showProvincePicker() {
        if (!isAddressTreeReady()) {
            return;
        }
        showPicker("Tỉnh/Thành phố", addressTree.getProvinces(), selectedProvince, value -> {
            if (value.equals(selectedProvince)) {
                return;
            }
            selectedProvince = value;
            selectedDistrict = null;
            selectedWard = null;
            updateLocationViews();
        });
    }

    private void showDistrictPicker() {
        if (!isAddressTreeReady()) {
            return;
        }
        if (selectedProvince == null) {
            Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố trước", Toast.LENGTH_SHORT).show();
            return;
        }
        showPicker("Quận/Huyện", addressTree.getDistricts(selectedProvince), selectedDistrict, value -> {
            if (value.equals(selectedDistrict)) {
                return;
            }
            selectedDistrict = value;
            selectedWard = null;
            updateLocationViews();
        });
    }

    private void showWardPicker() {
        if (!isAddressTreeReady()) {
            return;
        }
        if (selectedProvince == null || selectedDistrict == null) {
            Toast.makeText(this, "Vui lòng chọn Quận/Huyện trước", Toast.LENGTH_SHORT).show();
            return;
        }
        showPicker("Phường/Xã", addressTree.getWards(selectedProvince, selectedDistrict), selectedWard, value -> {
            selectedWard = value;
            updateLocationViews();
        });
    }

    private boolean isAddressTreeReady() {
        if (addressTree == null || addressTree.isEmpty()) {
            Toast.makeText(this, "Dữ liệu Tỉnh/Huyện/Xã chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showPicker(String title, List<String> options, String currentValue, PickerCallback callback) {
        if (options == null || options.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để chọn", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = options.toArray(new String[0]);
        int checkedItem = 0;
        if (currentValue != null) {
            for (int index = 0; index < options.size(); index++) {
                if (options.get(index).equals(currentValue)) {
                    checkedItem = index;
                    break;
                }
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    callback.onSelected(items[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void updateLocationViews() {
        setLocationText(tvGuestCity, selectedProvince, "Tỉnh/Thành phố");
        setLocationText(tvGuestDistrict, selectedDistrict, "Quận/Huyện");
        setLocationText(tvGuestWard, selectedWard, "Phường/Xã");
    }

    private void setLocationText(TextView view, String value, String hint) {
        if (value == null || value.trim().isEmpty()) {
            view.setText(hint);
            view.setTextColor(android.graphics.Color.parseColor("#777777"));
            return;
        }
        view.setText(value);
        view.setTextColor(android.graphics.Color.parseColor("#1E1E1E"));
    }

    private void showDeliveryOriginDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_origin, null, false);
        TextView tvAddress = dialogView.findViewById(R.id.tvDeliveryOriginAddress);
        TextView tvDistance = dialogView.findViewById(R.id.tvDeliveryOriginDistance);
        TextView btnClose = dialogView.findViewById(R.id.btnCloseDeliveryOrigin);

        tvAddress.setText("123 Nguyen Van Linh, P. Tan Phong, Quan 7, TP. Ho Chi Minh");
        tvDistance.setText("20 km ~ 45 phút");

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showVoucherDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvVoucherOptions);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvVoucherSelectedCount);
        TextView tvSelectedTitle = dialogView.findViewById(R.id.tvVoucherSelectedTitle);
        TextView btnApplyVoucher = dialogView.findViewById(R.id.btnApplyVoucher);
        EditText edtVoucherCode = dialogView.findViewById(R.id.edtVoucherCode);
        TextView btnSearchVoucher = dialogView.findViewById(R.id.btnSearchVoucher);
        TextView tvEmptyState = dialogView.findViewById(R.id.tvVoucherEmptyState);
        TextView tvListTitle = dialogView.findViewById(R.id.tvVoucherListTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvSelectedCount.setText("Đang tải khuyến mãi...");
        tvSelectedTitle.setText("");
        recyclerView.setAdapter(new VoucherOptionAdapter(new ArrayList<>(), RecyclerView.NO_POSITION, item -> {
        }));
        loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);

        BottomSheetDialog dialog = createHalfHeightBottomSheetDialog(dialogView);
        btnApplyVoucher.setOnClickListener(v -> dialog.dismiss());
        btnSearchVoucher.setOnClickListener(v -> {
            String code = edtVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);
            } else {
                searchVoucher(code, recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);
            }
        });
        dialog.show();
    }

    private List<PromotionTargetDto> cachedTargets = new ArrayList<>();
    private List<PromotionUsageDto> cachedUsages = new ArrayList<>();

    private void searchVoucher(String code, RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        isSearchingVoucher = true;

        // Phản hồi tức thì: Hiện tiêu đề tìm kiếm và thông báo đang kiểm tra
        recyclerView.setVisibility(View.GONE);
        tvListTitle.setVisibility(View.VISIBLE);
        tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Đang kiểm tra mã '" + code + "'...");
        selectedCountView.setText("Đang kiểm tra mã...");

        promotionApi.getPromotions(guestId, code, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PromotionDto> found = response.body();
                    runOnUiThread(() -> {
                        if (found.isEmpty()) {
                            showVoucherEmptyState("Không tìm thấy mã khuyến mãi '" + code + "'.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            tvListTitle.setVisibility(View.VISIBLE);
                            tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
                            recyclerView.setVisibility(View.VISIBLE);
                            renderVoucherOptions(found, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showVoucherEmptyState("Mã '" + code + "' không hợp lệ hoặc đã hết hạn.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                    });
                }
            }
            @Override public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    showVoucherEmptyState("Lỗi kết nối khi tìm mã. Vui lòng thử lại.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                });
            }
        });
    }

    private void showVoucherEmptyState(String message, RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        voucherItems.clear();
        if (recyclerView.getAdapter() != null) recyclerView.getAdapter().notifyDataSetChanged();
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvListTitle.setVisibility(View.VISIBLE);
        selectedCountView.setText("Không áp dụng được");
        selectedTitleView.setText("");
    }

    private void loadVoucherOptions(RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        isSearchingVoucher = false;
        List<PromotionDto> promotions = new ArrayList<>();
        cachedTargets.clear();
        cachedUsages.clear();
        AtomicInteger remainingCalls = new AtomicInteger(3);

        Runnable renderWhenReady = () -> {
            if (remainingCalls.decrementAndGet() == 0) {
                runOnUiThread(() -> {
                    if (isSearchingVoucher) return;
                    if (promotions.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Không có mã khuyến mãi khả dụng.");
                        recyclerView.setVisibility(View.GONE);
                        tvListTitle.setVisibility(View.VISIBLE);
                        tvListTitle.setText("Mã giảm giá");
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        tvListTitle.setVisibility(View.VISIBLE);
                        tvListTitle.setText("Mã giảm giá");
                    }
                    renderVoucherOptions(promotions, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView);
                });
            }
        };

        promotionApi.getPromotions(guestId, null, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) promotions.addAll(response.body());
                renderWhenReady.run();
            }
            @Override public void onFailure(Call<List<PromotionDto>> call, Throwable t) { renderWhenReady.run(); }
        });
        promotionApi.getPromotionTargets().enqueue(new Callback<ApiListResponseDto<PromotionTargetDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionTargetDto>> call, Response<ApiListResponseDto<PromotionTargetDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedTargets.addAll(response.body().getData());
                }
                renderWhenReady.run();
            }
            @Override public void onFailure(Call<ApiListResponseDto<PromotionTargetDto>> call, Throwable t) { renderWhenReady.run(); }
        });
        promotionApi.getPromotionUsages().enqueue(new Callback<ApiListResponseDto<PromotionUsageDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionUsageDto>> call, Response<ApiListResponseDto<PromotionUsageDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedUsages.addAll(response.body().getData());
                }
                renderWhenReady.run();
            }
            @Override public void onFailure(Call<ApiListResponseDto<PromotionUsageDto>> call, Throwable t) { renderWhenReady.run(); }
        });
    }

    private void renderVoucherOptions(List<PromotionDto> promotions, List<PromotionTargetDto> targets,
                                      List<PromotionUsageDto> usages, RecyclerView recyclerView,
                                      TextView selectedCountView, TextView selectedTitleView) {
        voucherItems.clear();
        Map<String, PromotionTargetDto> targetByPromotion = new HashMap<>();
        for (PromotionTargetDto target : targets) {
            if (target.getPromotionId() != null) targetByPromotion.put(target.getPromotionId(), target);
        }
        Map<String, PromotionUsageDto> usageByPromotion = new HashMap<>();
        for (PromotionUsageDto usage : usages) {
            if (usage.getPromotionId() != null) usageByPromotion.put(usage.getPromotionId(), usage);
        }

        for (PromotionDto promotion : promotions) {
            if (promotion.getPromotionKind() != null && "FlashSale".equalsIgnoreCase(promotion.getPromotionKind())) {
                continue;
            }
            PromotionTargetDto target = targetByPromotion.get(promotion.getPromotionId());
            PromotionUsageDto usage = usageByPromotion.get(promotion.getPromotionId());
            String disabledReason = disabledReason(promotion, target, usage);
            boolean enabled = disabledReason == null;

            voucherItems.add(new VoucherOptionAdapter.VoucherItemUiModel(
                    voucherTitle(promotion),
                    voucherCondition(promotion, target, disabledReason),
                    voucherExpiry(promotion),
                    R.drawable.ic_voucher,
                    promotion.getPromotionId(),
                    enabled,
                    promotion
            ));
        }

        java.util.Collections.sort(voucherItems, (v1, v2) -> {
            if (v1.enabled && !v2.enabled) {
                return -1;
            } else if (!v1.enabled && v2.enabled) {
                return 1;
            }
            return 0;
        });

        int selectedPosition = selectedVoucherPosition();
        updateVoucherSelectionText(selectedCountView, selectedTitleView);
        recyclerView.setAdapter(new VoucherOptionAdapter(voucherItems, selectedPosition, item -> {
            selectedVoucher = item;
            updateVoucherSelectionText(selectedCountView, selectedTitleView);
            updateSelectedVoucherUi();
            bindPaymentSummary();
        }));
    }

    private BottomSheetDialog createHalfHeightBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int halfScreenHeight = getResources().getDisplayMetrics().heightPixels / 2;
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                bottomSheet.getLayoutParams().height = halfScreenHeight;
                bottomSheet.requestLayout();
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(halfScreenHeight, true);
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
        return String.format(Locale.getDefault(),
                "Dự kiến nhận hàng hôm nay, %02d:%02d - %02d:%02d",
                startHour,
                startMinute,
                endHour,
                startMinute);
    }

    private void showNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note, null, false);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        TextView btnSave = dialogView.findViewById(R.id.btnSave);
        edtNote.setText(tvCheckoutNote.getText());
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
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    private void updateSelectedVoucherUi() {
        if (tvCheckoutSelectedVoucher == null) return;
        if (selectedVoucher != null) {
            tvCheckoutSelectedVoucher.setText(selectedVoucher.title);
            tvCheckoutSelectedVoucher.setTextColor(getResources().getColor(R.color.primary_main, getTheme()));
        } else {
            tvCheckoutSelectedVoucher.setText("Chọn hoặc nhập mã");
            tvCheckoutSelectedVoucher.setTextColor(android.graphics.Color.parseColor("#616161"));
        }
    }

    private void updateVoucherSelectionText(TextView selectedCountView, TextView selectedTitleView) {
        if (selectedVoucher != null) {
            selectedCountView.setText("1 mã đã được chọn");
            selectedTitleView.setText(selectedVoucher.title);
        } else if (voucherItems.isEmpty()) {
            selectedCountView.setText("Không có mã khuyến mãi khả dụng");
            selectedTitleView.setText("");
        } else {
            selectedCountView.setText("Chưa chọn mã khuyến mãi");
            selectedTitleView.setText("");
        }
    }

    private void restoreSelectedVoucherFromIntent() {
        String promotionId = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_ID);
        String title = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_TITLE);
        PromotionDto promotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_voucher_dto");
        if (title != null && !title.trim().isEmpty()) {
            selectedVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    title,
                    "",
                    "",
                    R.drawable.ic_voucher,
                    promotionId,
                    true,
                    promotion
            );
        }
        updateSelectedVoucherUi();
    }

    private void prefetchVoucherData() {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        promotionApi.getPromotionTargets().enqueue(new Callback<ApiListResponseDto<PromotionTargetDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionTargetDto>> call, Response<ApiListResponseDto<PromotionTargetDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedTargets.clear();
                    cachedTargets.addAll(response.body().getData());
                    validateAppliedVoucher();
                }
            }
            @Override public void onFailure(Call<ApiListResponseDto<PromotionTargetDto>> call, Throwable t) {}
        });

        promotionApi.getPromotionUsages().enqueue(new Callback<ApiListResponseDto<PromotionUsageDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionUsageDto>> call, Response<ApiListResponseDto<PromotionUsageDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedUsages.clear();
                    cachedUsages.addAll(response.body().getData());
                    validateAppliedVoucher();
                }
            }
            @Override public void onFailure(Call<ApiListResponseDto<PromotionUsageDto>> call, Throwable t) {}
        });
    }

    private void validateAppliedVoucher() {
        if (selectedVoucher == null || selectedVoucher.promotion == null) {
            return;
        }

        PromotionTargetDto target = null;
        for (PromotionTargetDto t : cachedTargets) {
            if (selectedVoucher.promotionId != null && selectedVoucher.promotionId.equals(t.getPromotionId())) {
                target = t;
                break;
            }
        }

        PromotionUsageDto usage = null;
        for (PromotionUsageDto u : cachedUsages) {
            if (selectedVoucher.promotionId != null && selectedVoucher.promotionId.equals(u.getPromotionId())) {
                usage = u;
                break;
            }
        }

        String reason = disabledReason(selectedVoucher.promotion, target, usage);
        if (reason != null) {
            selectedVoucher = null;
            updateSelectedVoucherUi();
            bindPaymentSummary();
            Toast.makeText(this, "Voucher đã được hủy vì giỏ hàng không còn đáp ứng điều kiện áp dụng.", Toast.LENGTH_LONG).show();
        }
    }

    private int selectedVoucherPosition() {
        if (selectedVoucher == null || selectedVoucher.promotionId == null) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < voucherItems.size(); i++) {
            VoucherOptionAdapter.VoucherItemUiModel item = voucherItems.get(i);
            if (item.enabled && selectedVoucher.promotionId.equals(item.promotionId)) {
                selectedVoucher = item;
                updateSelectedVoucherUi();
                bindPaymentSummary();
                return i;
            }
        }
        selectedVoucher = null;
        updateSelectedVoucherUi();
        bindPaymentSummary();
        return RecyclerView.NO_POSITION;
    }

    private String disabledReason(PromotionDto promotion, PromotionTargetDto target, PromotionUsageDto usage) {
        if (promotion == null) return null;

        if (!Boolean.TRUE.equals(promotion.getActive())) {
            return "Mã đã bị vô hiệu hóa";
        }

        long now = System.currentTimeMillis();
        Long start = parseDateMillis(promotion.getStartDate());
        Long end = parseDateMillis(promotion.getEndDate());
        if (start != null && now < start) {
            return "Chưa đến thời gian bắt đầu";
        }
        if (end != null && now > end) {
            return "Mã đã hết hạn";
        }

        double minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : 0;
        if (currentSubtotal < minOrder) {
            return "Đơn tối thiểu " + formatCurrency((long) minOrder);
        }
        if (!matchesTarget(target)) {
            // Check if it's a shipping voucher
            if ("Shipping".equalsIgnoreCase(promotion.getPromotionKind())) {
                return null;
            }
            return "Không áp dụng cho sản phẩm trong giỏ";
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageLimit() > 0 && usage != null
                && usage.getOrderIds() != null
                && usage.getOrderIds().size() >= promotion.getUsageLimit()) {
            return "Mã đã hết lượt sử dụng";
        }
        return null;
    }

    private boolean matchesTarget(PromotionTargetDto target) {
        if (target == null) {
            return true;
        }
        if (target.getTargetGroups() != null && !target.getTargetGroups().isEmpty()) {
            for (PromotionTargetDto.TargetGroupDto group : target.getTargetGroups()) {
                if (!matchesTargetGroup(group.getTargetType(), group.getTargetRefs())) {
                    return false;
                }
            }
            return true;
        }
        return matchesTargetGroup(target.getTargetType(), target.getTargetRefs());
    }

    private boolean matchesTargetGroup(String targetType, List<String> targetRefs) {
        if (targetType == null || targetRefs == null || targetRefs.isEmpty()) {
            return true;
        }
        if ("User".equalsIgnoreCase(targetType)
                || "Shipping".equalsIgnoreCase(targetType)
                || "Order".equalsIgnoreCase(targetType)) {
            return true;
        }
        Set<String> refs = new HashSet<>(targetRefs);
        for (CartDto.CartItemDto item : cartItems) {
            if ("Product".equalsIgnoreCase(targetType) && refs.contains(item.getSku())) {
                return true;
            }
            ProductDto product = item.getProduct();
            if (product == null) continue;
            if ("Category".equalsIgnoreCase(targetType) && refs.contains(product.getCategoryId())) {
                return true;
            }
            if ("Subcategory".equalsIgnoreCase(targetType) && refs.contains(product.getSubcategoryId())) {
                return true;
            }
            if ("Brand".equalsIgnoreCase(targetType) && refs.contains(product.getBrand())) {
                return true;
            }
        }
        return false;
    }

    private boolean isActivePromotion(PromotionDto promotion) {
        if (promotion == null || !Boolean.TRUE.equals(promotion.getActive())) return false;
        long now = System.currentTimeMillis();
        Long start = parseDateMillis(promotion.getStartDate());
        Long end = parseDateMillis(promotion.getEndDate());
        return (start == null || now >= start) && (end == null || now <= end);
    }

    private Long parseDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String normalized = value.trim();
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(pattern, Locale.US);
                if (pattern.endsWith("'Z'")) {
                    formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
                Date date = formatter.parse(normalized);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String voucherTitle(PromotionDto promotion) {
        if (promotion.getName() != null && !promotion.getName().trim().isEmpty()) return promotion.getName();
        if (promotion.getCode() != null && !promotion.getCode().trim().isEmpty()) return promotion.getCode();
        return promotion.getPromotionId() != null ? promotion.getPromotionId() : "Khuyến mãi";
    }

    private String voucherCondition(PromotionDto promotion, PromotionTargetDto target, String disabledReason) {
        StringBuilder text = new StringBuilder();
        Double discountValue = promotion.getDiscountValue();
        if (discountValue != null && discountValue > 0) {
            boolean fixed = "fixed".equalsIgnoreCase(promotion.getDiscountType());
            text.append(fixed ? "Giảm " + formatCurrency(discountValue.longValue()) : "Giảm " + trimTrailingZeros(discountValue) + "%");
        } else if (isShippingPromotion(promotion)) {
            text.append("Miễn phí vận chuyển");
        } else {
            text.append(promotion.getDescription() != null ? promotion.getDescription() : "Khuyến mãi");
        }
        if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue() > 0) {
            text.append(" | Đơn từ ").append(formatCurrency(promotion.getMinOrderValue().longValue()));
        }
        if (target != null && target.getTargetType() != null) {
            text.append(" | Áp dụng: ").append(target.getTargetType());
        }
        if (disabledReason != null) {
            text.append(" | ").append(disabledReason);
        }
        return text.toString();
    }

    private String voucherExpiry(PromotionDto promotion) {
        return promotion.getEndDate() != null && !promotion.getEndDate().trim().isEmpty()
                ? "Hết hạn: " + promotion.getEndDate()
                : "Không giới hạn thời gian";
    }

    private boolean isShippingPromotion(PromotionDto promotion) {
        String name = (promotion.getName() != null ? promotion.getName() : "").toLowerCase(Locale.US);
        String description = (promotion.getDescription() != null ? promotion.getDescription() : "").toLowerCase(Locale.US);
        String scope = (promotion.getScope() != null ? promotion.getScope() : "").toLowerCase(Locale.US);
        return scope.contains("shipping") || name.contains("vận chuyển") || description.contains("vận chuyển");
    }

    private long calculatePromotionDiscount(PromotionDto promotion, long baseAmount) {
        if (promotion == null || baseAmount <= 0) return 0;
        Double value = promotion.getDiscountValue();
        if (value == null || value <= 0) {
            return isShippingPromotion(promotion) ? baseAmount : 0;
        }
        long discount = "fixed".equalsIgnoreCase(promotion.getDiscountType())
                ? Math.round(value)
                : Math.round(baseAmount * value / 100d);
        if (promotion.getMaxDiscountValue() != null && promotion.getMaxDiscountValue() > 0) {
            discount = Math.min(discount, Math.round(promotion.getMaxDiscountValue()));
        }
        return Math.max(0, Math.min(discount, baseAmount));
    }

    private long variantPrice(long price, double selectedWeight, boolean hasWeightOptions) {
        double multiplier = hasWeightOptions ? selectedWeight : 1.0;
        return Math.round(price * multiplier);
    }

    private double calculateCarbonEmission(ProductDto product, int quantity, double selectedWeight, boolean hasWeightOptions) {
        if (product == null || quantity <= 0) {
            return 0;
        }
        double emissionFactor = product.getEmissionFactor();
        if (emissionFactor <= 0) {
            return product.getCarbonSavingPoint() > 0 ? (product.getCarbonSavingPoint() * quantity) / 10.0 : 0;
        }
        double kilograms = resolveCarbonKilograms(product, selectedWeight, hasWeightOptions);
        return kilograms * emissionFactor * quantity;
    }

    private double resolveCarbonKilograms(ProductDto product, double selectedWeight, boolean hasWeightOptions) {
        if (hasWeightOptions && selectedWeight > 0) {
            return selectedWeight;
        }
        double parsed = parseWeightInKilograms(product.getWeight());
        return parsed > 0 ? parsed : 1.0;
    }

    private double parseWeightInKilograms(String value) {
        String text = safeText(value).toLowerCase(Locale.US);
        if (text.isEmpty()) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+(?:[\\.,]\\d+)?)\\s*(kg|kilogram|g|gr|gram)")
                .matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        try {
            double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
            return matcher.group(2).startsWith("g") ? amount / 1000.0 : amount;
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String resolveItemUnit(ProductDto product, double selectedWeight, boolean hasWeightOptions) {
        if (hasWeightOptions) {
            return formatWeight(selectedWeight);
        }
        if (product == null) {
            return "";
        }
        String weight = safeText(product.getWeight());
        return weight.isEmpty() ? safeText(product.getUnit()) : weight;
    }

    private String formatWeight(double weight) {
        if (weight >= 1.0) {
            if (Math.abs(weight - Math.round(weight)) < 0.0001) {
                return String.format(Locale.US, "%.0fkg", weight);
            }
            return trimTrailingZeros(weight) + "kg";
        }
        return String.format(Locale.US, "%.0fg", weight * 1000);
    }

    private String trimTrailingZeros(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void setCarbonPointsText(double points) {
        if (tvCheckoutCarbonPoints == null) {
            return;
        }
        String pointsText = Math.abs(points - Math.round(points)) < 0.0001
                ? String.format(Locale.US, "%.0f", points)
                : String.format(Locale.US, "%.1f", points);
        String text = "Nhận " + pointsText + " điểm carbon";
        SpannableString spannable = new SpannableString(text);
        int start = text.indexOf(pointsText);
        int end = start + pointsText.length();
        if (start >= 0) {
            spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_main)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvCheckoutCarbonPoints.setText(spannable);
    }

    private String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }

    private String formatDiscount(long amount) {
        return amount > 0 ? "-" + formatCurrency(amount) : "0đ";
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeText(String preferred, String fallback) {
        return preferred == null || preferred.trim().isEmpty() ? fallback : preferred;
    }

    private interface PickerCallback {
        void onSelected(String value);
    }
}
