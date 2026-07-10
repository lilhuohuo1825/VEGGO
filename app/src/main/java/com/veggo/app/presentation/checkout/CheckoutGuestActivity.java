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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.veggo.app.core.otp.OtpAutoFillHelper;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.utils.DeliveryTimeUtils;
import com.veggo.app.core.utils.ProductCatalogImageResolver;
import com.veggo.app.core.utils.WarehouseDistanceUtils;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.AuthApi;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.api.PaymentApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.data.remote.dto.PaymentUrlDto;
import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.OrderDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.request.ForgotPasswordRequest;
import com.veggo.app.data.remote.request.VerifyGuestOrderOtpRequest;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.promotion.PromotionVoucherHelper;

import java.text.SimpleDateFormat;
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
    private static final String REQUIRED_FIELD_ERROR = "Thông tin này không được để trống";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MS = 60_000;
    private static final int MAX_OTP_ATTEMPTS = 3;

    private EditText edtGuestName;
    private EditText edtGuestPhone;
    private EditText edtGuestAddressDetail;
    private TextView tvGuestNameError;
    private TextView tvGuestPhoneError;
    private TextView tvGuestProvinceError;
    private TextView tvGuestDistrictError;
    private TextView tvGuestWardError;
    private TextView tvGuestAddressDetailError;
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
    private TextView tvVoucherLabel;
    private TextView tvFastDeliveryTime;
    private TextView tvScheduleDeliveryTime;
    private TextView tvGuestCity;
    private TextView tvGuestDistrict;
    private TextView tvGuestWard;
    private TextView tvCheckoutDeliveryOriginAddress;
    private TextView tvCheckoutDeliveryOriginDistance;
    private View layoutFastDelivery;
    private View layoutScheduleDelivery;
    private AppCompatRadioButton radioPaymentMomo;
    private AppCompatRadioButton radioPaymentBank;
    private AppCompatRadioButton radioPaymentCod;
    private AppCompatRadioButton radioPaymentVnpay;
    private PaymentItemAdapter paymentItemAdapter;
    private final List<CartDto.CartItemDto> cartItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> voucherItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> allVoucherItems = new ArrayList<>();
    private long currentSubtotal;
    private long currentProductDiscount;
    private long currentVoucherDiscount;
    private long currentShippingFee;
    private long currentShippingDiscount;
    private long currentPaymentTotal;
    private double currentCarbonPoints;
    private double currentCarbonEmission;
    private int currentProductCount;
    private VoucherOptionAdapter.VoucherItemUiModel selectedProductVoucher;
    private VoucherOptionAdapter.VoucherItemUiModel selectedShippingVoucher;
    private String selectedPaymentMethod = "cod";
    private android.app.Dialog progressDialog;
    private android.widget.TextView progressTextView;
    private String guestId;
    private boolean buyNowMode;
    private Set<String> selectedCartLineKeys;
    private boolean isSearchingVoucher = false;
    private PromotionVoucherHelper.VoucherFilter activeVoucherFilter = PromotionVoucherHelper.VoucherFilter.PRODUCT;
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
    private CountDownTimer guestOrderOtpTimer;
    private String currentGuestOrderOtp;
    private long guestOrderOtpExpiresAt;
    private int guestOrderOtpFailedAttempts;
    private boolean guestOrderOtpUsesLocalFallback;
    private boolean isFastDeliveryMode = true;
    private DeliveryTimeUtils.DeliveryWindow fastDeliveryWindow;
    private DeliveryTimeUtils.DeliveryWindow scheduledDeliveryWindow;
    private final CheckoutWarehouseHelper warehouseHelper = new CheckoutWarehouseHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_guest);

        PendingCheckoutStore store = new PendingCheckoutStore(this);
        guestId = store.guestId();
        buyNowMode = getIntent().getBooleanExtra(CheckoutActivity.EXTRA_BUY_NOW, false);
        ArrayList<String> selectedLineKeys = getIntent().getStringArrayListExtra(
                CheckoutActivity.EXTRA_SELECTED_CART_LINE_KEYS
        );
        selectedCartLineKeys = selectedLineKeys == null || selectedLineKeys.isEmpty()
                ? null
                : new HashSet<>(selectedLineKeys);

        edtGuestName = findViewById(R.id.edtGuestName);
        edtGuestPhone = findViewById(R.id.edtGuestPhone);
        edtGuestAddressDetail = findViewById(R.id.edtGuestAddressDetail);
        tvGuestNameError = findViewById(R.id.tvGuestNameError);
        tvGuestPhoneError = findViewById(R.id.tvGuestPhoneError);
        tvGuestProvinceError = findViewById(R.id.tvGuestProvinceError);
        tvGuestDistrictError = findViewById(R.id.tvGuestDistrictError);
        tvGuestWardError = findViewById(R.id.tvGuestWardError);
        tvGuestAddressDetailError = findViewById(R.id.tvGuestAddressDetailError);
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
        tvVoucherLabel = findViewById(R.id.tvVoucherLabel);
        tvFastDeliveryTime = findViewById(R.id.tvFastDeliveryTime);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        tvGuestCity = findViewById(R.id.tvGuestCity);
        tvGuestDistrict = findViewById(R.id.tvGuestDistrict);
        tvGuestWard = findViewById(R.id.tvGuestWard);
        tvCheckoutDeliveryOriginAddress = findViewById(R.id.tvCheckoutDeliveryOriginAddress);
        tvCheckoutDeliveryOriginDistance = findViewById(R.id.tvCheckoutDeliveryOriginDistance);
        layoutFastDelivery = findViewById(R.id.layoutFastDelivery);
        layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);
        radioPaymentMomo = findViewById(R.id.radioPaymentMomo);
        radioPaymentBank = findViewById(R.id.radioPaymentCard);
        radioPaymentCod = findViewById(R.id.radioPaymentCod);
        radioPaymentVnpay = findViewById(R.id.radioPaymentVnpay);

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
        findViewById(R.id.layoutDeliveryOrigin).setOnClickListener(v ->
                warehouseHelper.showSelectionDialog(this, this::bindSelectedWarehouseUi, null));
        findViewById(R.id.layoutVoucher).setOnClickListener(v -> showVoucherDialog());
        findViewById(R.id.layoutNote).setOnClickListener(v -> showNoteDialog());
        layoutFastDelivery.setOnClickListener(v -> selectDeliveryMode(true));
        layoutScheduleDelivery.setOnClickListener(v -> {
            selectDeliveryMode(false);
            showScheduleTimePicker();
        });
        findViewById(R.id.btnPlaceOrder).setOnClickListener(v -> {
            findViewById(R.id.btnPlaceOrder).setEnabled(false);
            validatePhoneAndCreateOrder();
        });
        restoreSelectedVoucherFromIntent();
        prefetchVoucherData();
        refreshGuestWarehouse();
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
                    showFieldError(tvGuestNameError, "");
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
                String phone = s.toString().trim();
                if (phone.isEmpty() || phone.matches(PHONE_REGEX)) {
                    showFieldError(tvGuestPhoneError, "");
                } else if (phone.length() >= 10) {
                    validateGuestPhoneFormat();
                }
            }
        });
        edtGuestAddressDetail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateGuestAddressDetail();
            }
        });
        edtGuestAddressDetail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String detail = s.toString().trim();
                if (!detail.isEmpty() && detail.length() >= 5 && detail.matches(".*[\\p{L}\\d].*")) {
                    showFieldError(tvGuestAddressDetailError, "");
                }
                refreshGuestWarehouse();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (accountOtpTimer != null) {
            accountOtpTimer.cancel();
        }
        if (guestOrderOtpTimer != null) {
            guestOrderOtpTimer.cancel();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EmulatorSmsSender.handlePermissionResult(this, requestCode, grantResults);
    }

    private void setupPaymentMethods() {
        selectPaymentMethod("cod");
        radioPaymentMomo.setOnClickListener(v -> selectPaymentMethod("veggopay"));
        radioPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        radioPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        if (radioPaymentVnpay != null) {
            radioPaymentVnpay.setOnClickListener(v -> selectPaymentMethod("vnpay"));
        }
        View layoutPaymentMomo = findViewById(R.id.layoutPaymentMomo);
        View layoutPaymentCod = findViewById(R.id.layoutPaymentCod);
        View layoutPaymentBank = findViewById(R.id.layoutPaymentBank);
        View layoutPaymentVnpay = findViewById(R.id.layoutPaymentVnpay);
        if (layoutPaymentMomo != null) {
            layoutPaymentMomo.setOnClickListener(v -> selectPaymentMethod("veggopay"));
        }
        if (layoutPaymentCod != null) {
            layoutPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        }
        if (layoutPaymentBank != null) {
            layoutPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        }
        if (layoutPaymentVnpay != null) {
            layoutPaymentVnpay.setOnClickListener(v -> selectPaymentMethod("vnpay"));
        }
    }

    private void selectPaymentMethod(String method) {
        selectedPaymentMethod = method;
        if (radioPaymentMomo != null) {
            radioPaymentMomo.setChecked("veggopay".equals(method));
        }
        if (radioPaymentBank != null) {
            radioPaymentBank.setChecked("bank".equals(method));
        }
        if (radioPaymentCod != null) {
            radioPaymentCod.setChecked("cod".equals(method));
        }
        if (radioPaymentVnpay != null) {
            radioPaymentVnpay.setChecked("vnpay".equals(method));
        }
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
                double selectedWeight = item.getSelectedWeight() > 0 ? item.getSelectedWeight() : 1.0;
                if (selectedCartLineKeys != null
                        && !selectedCartLineKeys.contains(cartLineKey(item.getSku(), selectedWeight))) {
                    continue;
                }
                ProductDto product = item.getProduct();
                long price = product != null ? product.getPrice() : item.getPrice();
                long originalPrice = product != null ? product.getOriginalPrice() : item.getOriginalPrice();
                if (originalPrice < price) originalPrice = price;
                int quantity = Math.max(1, item.getQuantity());
                boolean hasWeightOptions = product != null
                        && product.getWeightOptions() != null
                        && !product.getWeightOptions().isEmpty();
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
                        ProductCatalogImageResolver.resolveCheckoutImage(this, product, item.getSku())
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
        if (selectedProductVoucher != null && selectedProductVoucher.promotion != null) {
            voucherDiscount = Math.min(currentSubtotal,
                    calculatePromotionDiscount(selectedProductVoucher.promotion, currentSubtotal));
        }
        if (selectedShippingVoucher != null && selectedShippingVoucher.promotion != null) {
            shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount,
                    calculatePromotionDiscount(selectedShippingVoucher.promotion, shippingFee)));
        }
        currentVoucherDiscount = voucherDiscount;
        currentShippingFee = shippingFee;
        currentShippingDiscount = shippingDiscount;
        currentPaymentTotal = Math.max(
                0,
                currentSubtotal - currentProductDiscount + shippingFee - voucherDiscount - shippingDiscount
        );
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
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validateSelectedVouchersBeforeCheckout()) {
            return;
        }
        if ("veggopay".equals(selectedPaymentMethod)) {
            Toast.makeText(this, "Ví VeggoPay yêu cầu đăng nhập tài khoản để sử dụng", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (!validateGuestInputs()) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        String phone = edtGuestPhone.getText().toString().trim();
        if (checkingPhone) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            Toast.makeText(this, "Đang kiểm tra số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneRequiresDifferentNumber) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            showFieldError(tvGuestPhoneError, "Vui lòng dùng số điện thoại khác");
            edtGuestPhone.requestFocus();
            return;
        }
        if (existingAccountUser != null && phone.equals(checkedPhone)) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
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
                    findViewById(R.id.btnPlaceOrder).setEnabled(true);
                    showExistingAccountDialog(response.body(), true);
                } else {
                    checkedPhone = phone;
                    existingAccountUser = null;
                    startGuestOrderOtpVerification();
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                checkedPhone = phone;
                existingAccountUser = null;
                startGuestOrderOtpVerification();
            }
        });
    }

    private void startGuestOrderOtpVerification() {
        String phone = edtGuestPhone.getText().toString().trim();
        showProgress("Đang gửi mã OTP...");
        ApiClient.createService(AuthApi.class)
                .sendGuestOrderOtp(new ForgotPasswordRequest(phone))
                .enqueue(new Callback<java.util.Map<String, String>>() {
                    @Override
                    public void onResponse(
                            Call<java.util.Map<String, String>> call,
                            Response<java.util.Map<String, String>> response
                    ) {
                        hideProgress();
                        if (response.isSuccessful() && response.body() != null) {
                            guestOrderOtpUsesLocalFallback = false;
                            openGuestOrderOtpDialog(phone, response.body().get("otp"));
                            return;
                        }
                        openGuestOrderOtpWithLocalFallback(phone);
                    }

                    @Override
                    public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                        hideProgress();
                        openGuestOrderOtpWithLocalFallback(phone);
                    }
                });
    }

    private void openGuestOrderOtpWithLocalFallback(String phone) {
        guestOrderOtpUsesLocalFallback = true;
        currentGuestOrderOtp = randomOtp();
        guestOrderOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
        guestOrderOtpFailedAttempts = 0;
        openGuestOrderOtpDialog(phone, currentGuestOrderOtp);
    }

    private void openGuestOrderOtpDialog(String phone, @Nullable String otpForSms) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_otp_veggo);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvOtpTitle = dialog.findViewById(R.id.tvOtpTitle);
        TextView otpMessage = dialog.findViewById(R.id.tvOtpMessage);
        TextView tvOtpError = dialog.findViewById(R.id.tvOtpError);
        TextView btnCancel = dialog.findViewById(R.id.btnOtpCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnOtpConfirm);
        TextView btnResend = dialog.findViewById(R.id.btnOtpResend);
        LinearLayout otpRow = dialog.findViewById(R.id.layoutOtpFields);
        otpRow.removeAllViews();
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

        if (tvOtpTitle != null) {
            tvOtpTitle.setText("Xác thực đặt hàng");
        }
        hideOtpError(tvOtpError);
        otpMessage.setText("Nhập mã OTP 6 số đã gửi đến số điện thoại " + maskPhone(phone) + ".");

        wireOtpFields(otpFields, () -> verifyGuestOrderOtp(phone, dialog, otpFields, tvOtpError, btnResend));
        final OtpAutoFillHelper otpAutoFillHelper = OtpAutoFillHelper.create(this, otpFields);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> verifyGuestOrderOtp(phone, dialog, otpFields, tvOtpError, btnResend));
        btnResend.setOnClickListener(v ->
                resendGuestOrderOtp(phone, otpFields, otpMessage, btnResend, tvOtpError));
        dialog.setOnDismissListener(dialogInterface -> {
            otpAutoFillHelper.stop();
            if (guestOrderOtpTimer != null) {
                guestOrderOtpTimer.cancel();
            }
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
        });
        dialog.show();
        startGuestOrderOtpTimer(otpMessage, btnResend);
        otpAutoFillHelper.start();
        if (otpForSms != null && !otpForSms.trim().isEmpty()) {
            EmulatorSmsSender.send(
                    this,
                    "VEGGO: Ma OTP dat hang cua ban la " + otpForSms.trim()
                            + ". Ma co hieu luc trong 60 giay."
            );
        }
        otpFields[0].requestFocus();
    }

    private void resendGuestOrderOtp(
            String phone,
            EditText[] otpFields,
            TextView otpMessage,
            TextView btnResend,
            TextView tvOtpError
    ) {
        hideOtpError(tvOtpError);
        clearOtpFields(otpFields);
        btnResend.setVisibility(View.GONE);
        if (guestOrderOtpUsesLocalFallback) {
            currentGuestOrderOtp = randomOtp();
            guestOrderOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
            guestOrderOtpFailedAttempts = 0;
            EmulatorSmsSender.send(
                    this,
                    "VEGGO: Ma OTP dat hang cua ban la " + currentGuestOrderOtp
                            + ". Ma co hieu luc trong 60 giay."
            );
            startGuestOrderOtpTimer(otpMessage, btnResend);
            otpFields[0].requestFocus();
            return;
        }
        showProgress("Đang gửi lại mã OTP...");
        ApiClient.createService(AuthApi.class)
                .sendGuestOrderOtp(new ForgotPasswordRequest(phone))
                .enqueue(new Callback<java.util.Map<String, String>>() {
                    @Override
                    public void onResponse(
                            Call<java.util.Map<String, String>> call,
                            Response<java.util.Map<String, String>> response
                    ) {
                        hideProgress();
                        if (response.isSuccessful() && response.body() != null) {
                            String otp = response.body().get("otp");
                            if (otp != null && !otp.trim().isEmpty()) {
                                EmulatorSmsSender.send(
                                        CheckoutGuestActivity.this,
                                        "VEGGO: Ma OTP dat hang cua ban la " + otp.trim()
                                                + ". Ma co hieu luc trong 60 giay."
                                );
                            }
                            startGuestOrderOtpTimer(otpMessage, btnResend);
                            otpFields[0].requestFocus();
                            return;
                        }
                        guestOrderOtpUsesLocalFallback = true;
                        currentGuestOrderOtp = randomOtp();
                        guestOrderOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
                        guestOrderOtpFailedAttempts = 0;
                        EmulatorSmsSender.send(
                                CheckoutGuestActivity.this,
                                "VEGGO: Ma OTP dat hang cua ban la " + currentGuestOrderOtp
                                        + ". Ma co hieu luc trong 60 giay."
                        );
                        startGuestOrderOtpTimer(otpMessage, btnResend);
                        otpFields[0].requestFocus();
                    }

                    @Override
                    public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                        hideProgress();
                        guestOrderOtpUsesLocalFallback = true;
                        currentGuestOrderOtp = randomOtp();
                        guestOrderOtpExpiresAt = System.currentTimeMillis() + OTP_TTL_MS;
                        guestOrderOtpFailedAttempts = 0;
                        EmulatorSmsSender.send(
                                CheckoutGuestActivity.this,
                                "VEGGO: Ma OTP dat hang cua ban la " + currentGuestOrderOtp
                                        + ". Ma co hieu luc trong 60 giay."
                        );
                        startGuestOrderOtpTimer(otpMessage, btnResend);
                        otpFields[0].requestFocus();
                    }
                });
    }

    private void verifyGuestOrderOtp(
            String phone,
            Dialog dialog,
            EditText[] fields,
            TextView tvOtpError,
            TextView btnResend
    ) {
        String otp = getOtpValue(fields);
        if (otp.length() < OTP_LENGTH) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }
        hideOtpError(tvOtpError);
        if (guestOrderOtpUsesLocalFallback) {
            verifyGuestOrderOtpLocally(dialog, fields, tvOtpError, btnResend, otp);
            return;
        }
        showProgress("Đang xác thực mã OTP...");
        ApiClient.createService(AuthApi.class)
                .verifyGuestOrderOtp(new VerifyGuestOrderOtpRequest(phone, otp))
                .enqueue(new Callback<java.util.Map<String, String>>() {
                    @Override
                    public void onResponse(
                            Call<java.util.Map<String, String>> call,
                            Response<java.util.Map<String, String>> response
                    ) {
                        hideProgress();
                        if (!response.isSuccessful()) {
                            showOtpError(tvOtpError, resolveGuestOtpErrorMessage(response, "Mã OTP không chính xác"));
                            if (btnResend != null) {
                                btnResend.setVisibility(View.VISIBLE);
                            }
                            clearOtpFields(fields);
                            fields[0].requestFocus();
                            return;
                        }
                        dialog.dismiss();
                        proceedGuestCheckout();
                    }

                    @Override
                    public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {
                        hideProgress();
                        showOtpError(tvOtpError, "Mã OTP không chính xác");
                        if (btnResend != null) {
                            btnResend.setVisibility(View.VISIBLE);
                        }
                        clearOtpFields(fields);
                        fields[0].requestFocus();
                    }
                });
    }

    private void verifyGuestOrderOtpLocally(
            Dialog dialog,
            EditText[] fields,
            TextView tvOtpError,
            TextView btnResend,
            String otp
    ) {
        if (System.currentTimeMillis() > guestOrderOtpExpiresAt) {
            showOtpError(tvOtpError, "Mã xác thực đã hết hạn. Vui lòng gửi lại mã");
            if (btnResend != null) {
                btnResend.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (guestOrderOtpFailedAttempts >= MAX_OTP_ATTEMPTS) {
            showOtpError(tvOtpError, "Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã");
            if (btnResend != null) {
                btnResend.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (!currentGuestOrderOtp.equals(otp)) {
            guestOrderOtpFailedAttempts++;
            showOtpError(tvOtpError, "Mã OTP không chính xác");
            if (btnResend != null) {
                btnResend.setVisibility(View.VISIBLE);
            }
            clearOtpFields(fields);
            fields[0].requestFocus();
            return;
        }
        dialog.dismiss();
        proceedGuestCheckout();
    }

    private String resolveGuestOtpErrorMessage(
            Response<java.util.Map<String, String>> response,
            String fallback
    ) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (body.contains("Mã OTP không chính xác")) {
                    return "Mã OTP không chính xác";
                }
                if (body.contains("Mã xác thực đã hết hạn")) {
                    return "Mã xác thực đã hết hạn. Vui lòng gửi lại mã";
                }
                if (body.contains("nhập sai quá số lần")) {
                    return "Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã";
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void startGuestOrderOtpTimer(TextView otpMessage, TextView btnResend) {
        if (guestOrderOtpTimer != null) {
            guestOrderOtpTimer.cancel();
        }
        guestOrderOtpTimer = new CountDownTimer(OTP_TTL_MS, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                otpMessage.setText("Nhập mã OTP 6 số. Mã còn hiệu lực trong "
                        + Math.max(1, millisUntilFinished / 1000) + " giây.");
            }

            @Override public void onFinish() {
                otpMessage.setText("Mã xác thực đã hết hạn. Vui lòng gửi lại mã.");
                btnResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void showOtpError(@Nullable TextView tvOtpError, String message) {
        if (tvOtpError == null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }
        tvOtpError.setText(message);
        tvOtpError.setVisibility(View.VISIBLE);
    }

    private void hideOtpError(@Nullable TextView tvOtpError) {
        if (tvOtpError != null) {
            tvOtpError.setVisibility(View.GONE);
            tvOtpError.setText("");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    private void proceedGuestCheckout() {
        if ("vnpay".equals(selectedPaymentMethod)) {
            String tempOrderId = "VG" + System.currentTimeMillis();
            openVnpayPayment(tempOrderId);
            return;
        }
        showProgress("Đang gửi yêu cầu đặt đơn hàng...");
        createOrder(guestId, true);
    }

    private boolean validateGuestInputs() {
        boolean isValid = true;
        isValid &= validateGuestName();
        isValid &= validateGuestPhoneFormat();
        isValid &= validateGuestProvince();
        isValid &= validateGuestDistrict();
        isValid &= validateGuestWard();
        isValid &= validateGuestAddressDetail();
        return isValid;
    }

    private boolean validateGuestName() {
        String name = edtGuestName.getText().toString().trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            return showFieldError(tvGuestNameError, REQUIRED_FIELD_ERROR);
        }
        if (!isValidGuestName(name)) {
            return showFieldError(tvGuestNameError, "Họ tên không được chứa số/ký tự lạ");
        }
        return showFieldError(tvGuestNameError, "");
    }

    private boolean isValidGuestName(String name) {
        String normalizedName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        return normalizedName.matches(NAME_REGEX);
    }

    private boolean validateGuestPhoneFormat() {
        String phone = edtGuestPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            return showFieldError(tvGuestPhoneError, REQUIRED_FIELD_ERROR);
        }
        if (!phone.matches(PHONE_REGEX)) {
            return showFieldError(tvGuestPhoneError,
                    "Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
        }
        return showFieldError(tvGuestPhoneError, "");
    }

    private boolean validateGuestProvince() {
        if (selectedProvince == null || selectedProvince.trim().isEmpty()) {
            return showFieldError(tvGuestProvinceError, REQUIRED_FIELD_ERROR);
        }
        return showFieldError(tvGuestProvinceError, "");
    }

    private boolean validateGuestDistrict() {
        if (selectedDistrict == null || selectedDistrict.trim().isEmpty()) {
            return showFieldError(tvGuestDistrictError, REQUIRED_FIELD_ERROR);
        }
        return showFieldError(tvGuestDistrictError, "");
    }

    private boolean validateGuestWard() {
        if (selectedWard == null || selectedWard.trim().isEmpty()) {
            return showFieldError(tvGuestWardError, REQUIRED_FIELD_ERROR);
        }
        return showFieldError(tvGuestWardError, "");
    }

    private boolean validateGuestAddressDetail() {
        String detail = edtGuestAddressDetail.getText().toString().trim();
        if (detail.isEmpty()) {
            return showFieldError(tvGuestAddressDetailError, REQUIRED_FIELD_ERROR);
        }
        if (detail.length() < 5 || !detail.matches(".*[\\p{L}\\d].*")) {
            return showFieldError(tvGuestAddressDetailError, "Địa chỉ cụ thể chưa hợp lệ");
        }
        return showFieldError(tvGuestAddressDetailError, "");
    }

    private boolean showFieldError(TextView errorView, String error) {
        if (errorView == null) {
            return error == null || error.isEmpty();
        }
        boolean hasError = error != null && !error.isEmpty();
        errorView.setText(hasError ? error : "");
        errorView.setVisibility(hasError ? View.VISIBLE : View.GONE);
        return !hasError;
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
                        findViewById(R.id.btnPlaceOrder).setEnabled(true);
                        showFieldError(tvGuestPhoneError, "Vui lòng dùng số điện thoại khác");
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
        final OtpAutoFillHelper otpAutoFillHelper = OtpAutoFillHelper.create(this, otpFields);
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
            otpAutoFillHelper.stop();
            if (accountOtpTimer != null) {
                accountOtpTimer.cancel();
            }
        });
        dialog.show();
        startAccountOtpTimer(otpMessage, btnResend);
        otpAutoFillHelper.start();
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
                        hideProgress();
                        if (!response.isSuccessful() || response.body() == null) {
                            findViewById(R.id.btnPlaceOrder).setEnabled(true);
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
                        hideProgress();
                        findViewById(R.id.btnPlaceOrder).setEnabled(true);
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
        payload.put("code", selectedProductVoucher != null ? safeText(selectedProductVoucher.promotionId) : "");
        payload.put("promotion_id", selectedProductVoucher != null ? safeText(selectedProductVoucher.promotionId) : null);
        payload.put("promotionName", selectedProductVoucher != null ? safeText(selectedProductVoucher.title) : "");
        payload.put("shippingPromotionId", selectedShippingVoucher != null ? safeText(selectedShippingVoucher.promotionId) : null);
        payload.put("shippingPromotionName", selectedShippingVoucher != null ? safeText(selectedShippingVoucher.title) : "");
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
        shippingInfo.put("deliveryMethod", isFastDeliveryMode ? "fast" : "scheduled");
        DeliveryTimeUtils.DeliveryWindow activeWindow = getActiveDeliveryWindow();
        if (activeWindow != null) {
            shippingInfo.put("deliveryWindowStart", formatDeliveryTimestamp(activeWindow.start));
            shippingInfo.put("deliveryWindowEnd", formatDeliveryTimestamp(activeWindow.end));
            shippingInfo.put("deliveryTimeText", activeWindow.displayText());
        }
        shippingInfo.put("notes", tvCheckoutNote.getText().toString().trim());
        shippingInfo.put("warehouse_id", safeText(warehouseHelper.getSelectedWarehouseCode()));
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
            item.put("image", ProductCatalogImageResolver.resolveCheckoutImage(this, product, itemDto.getSku()));
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
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_CODE,
                "bank".equals(selectedPaymentMethod) ? orderId : buildPaymentCode());
        intent.putExtra(QrPaymentActivity.EXTRA_ORDER_ID, orderId);
        intent.putExtra(QrPaymentActivity.EXTRA_SHOW_SUCCESS_IMMEDIATELY,
                "cod".equals(selectedPaymentMethod) || "vnpay".equals(selectedPaymentMethod));
        intent.putExtra(QrPaymentActivity.EXTRA_CLEAR_CART_ON_SUCCESS, !buyNowMode);
        intent.putExtra(QrPaymentActivity.EXTRA_CUSTOMER_ID, customerId);
        intent.putExtra(QrPaymentActivity.EXTRA_IS_GUEST_ORDER, guestOrder);
        intent.putExtra(QrPaymentActivity.EXTRA_GUEST_PHONE, edtGuestPhone.getText().toString().trim());
        if (!buyNowMode) {
            intent.putStringArrayListExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_SKUS, selectedCleanupSkus());
            intent.putExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_WEIGHTS, selectedCleanupWeights());
        }
        startActivity(intent);
        finish();
    }

    private void openVnpayPayment(String orderId) {
        showProgress("Đang tạo liên kết thanh toán VNPay...");
        new Thread(() -> {
            try {
                PaymentApi paymentApi = ApiClient.createService(PaymentApi.class);
                Map<String, Object> body = new HashMap<>();
                body.put("orderId", orderId);
                body.put("amount", currentPaymentTotal);
                body.put("orderInfo", "Thanh toan don hang VEGGO " + orderId);
                Response<PaymentUrlDto> resp = paymentApi.createVnpayUrl(body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    String paymentUrl = resp.body().getPaymentUrl();
                    if (paymentUrl == null || paymentUrl.isEmpty()) {
                        runOnUiThread(() -> {
                            hideProgress();
                            findViewById(R.id.btnPlaceOrder).setEnabled(true);
                            Toast.makeText(this, "Lỗi: paymentUrl rỗng từ server", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        hideProgress();
                        Intent intent = new Intent(this, VnpayWebViewActivity.class);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_ORDER_ID, orderId);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_CUSTOMER_ID, guestId);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_CLEAR_CART, !buyNowMode);
                        if (!buyNowMode) {
                            intent.putStringArrayListExtra(VnpayWebViewActivity.EXTRA_CLEANUP_SKUS, selectedCleanupSkus());
                            intent.putExtra(VnpayWebViewActivity.EXTRA_CLEANUP_WEIGHTS, selectedCleanupWeights());
                        }
                        startActivityForResult(intent, 1001);
                    });
                } else {
                    String errBody = "";
                    try {
                        if (resp.errorBody() != null) {
                            errBody = resp.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }
                    final String finalErr = errBody;
                    runOnUiThread(() -> {
                        hideProgress();
                        findViewById(R.id.btnPlaceOrder).setEnabled(true);
                        Toast.makeText(this,
                                "Không thể tạo URL thanh toán (HTTP " + resp.code() + "): " + finalErr,
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgress();
                    findViewById(R.id.btnPlaceOrder).setEnabled(true);
                    Toast.makeText(this, "Lỗi kết nối VNPAY: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void createOrderAfterVnpaySuccess(String orderId) {
        Map<String, Object> payload = buildOrderPayload(guestId, true);
        payload.put("orderId", orderId);
        payload.put("paymentStatus", "paid");
        ApiClient.createService(OrderApi.class).createOrder(payload).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                hideProgress();
                String finalId = orderId;
                if (response.isSuccessful() && response.body() != null) {
                    finalId = response.body().getOrderId() != null
                            ? response.body().getOrderId()
                            : response.body().getId();
                }
                openPaymentStep(finalId, guestId, true);
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                hideProgress();
                Toast.makeText(CheckoutGuestActivity.this,
                        "Đã thanh toán nhưng lỗi tạo đơn: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                openPaymentStep(orderId, guestId, true);
            }
        });
    }

    private ArrayList<String> selectedCleanupSkus() {
        ArrayList<String> skus = new ArrayList<>();
        for (CartDto.CartItemDto item : cartItems) {
            skus.add(safeText(item.getSku()));
        }
        return skus;
    }

    private double[] selectedCleanupWeights() {
        double[] weights = new double[cartItems.size()];
        for (int i = 0; i < cartItems.size(); i++) {
            double selectedWeight = cartItems.get(i).getSelectedWeight();
            weights[i] = selectedWeight > 0 ? selectedWeight : 1.0;
        }
        return weights;
    }

    private String buildPaymentCode() {
        long now = System.currentTimeMillis() % 1_000_000L;
        String suffix = guestId == null ? "GUEST" : guestId.replaceAll("[^A-Za-z0-9]", "");
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return String.format(Locale.US, "VG%s%06d", suffix, now);
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
            showFieldError(tvGuestProvinceError, "");
            showFieldError(tvGuestDistrictError, "");
            showFieldError(tvGuestWardError, "");
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
            showFieldError(tvGuestDistrictError, "");
            showFieldError(tvGuestWardError, "");
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
            showFieldError(tvGuestWardError, "");
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
        refreshGuestWarehouse();
    }

    private void refreshGuestWarehouse() {
        warehouseHelper.updateDeliveryAddress(buildGuestDeliveryAddress(), this::bindSelectedWarehouseUi);
    }

    private String buildGuestDeliveryAddress() {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, edtGuestAddressDetail != null
                ? edtGuestAddressDetail.getText().toString().trim()
                : "");
        appendAddressPart(builder, selectedWard);
        appendAddressPart(builder, selectedDistrict);
        appendAddressPart(builder, selectedProvince);
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, @Nullable String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private void bindSelectedWarehouseUi(
            @NonNull WarehouseDistanceUtils.RankedWarehouse warehouse,
            @NonNull String distanceText
    ) {
        if (warehouse.warehouse == null) {
            return;
        }
        if (tvCheckoutDeliveryOriginAddress != null) {
            String address = warehouse.warehouse.getAddress();
            tvCheckoutDeliveryOriginAddress.setText(address == null ? "" : address);
        }
        if (tvCheckoutDeliveryOriginDistance != null) {
            tvCheckoutDeliveryOriginDistance.setText(distanceText);
        }
        refreshDeliveryTimes();
    }

    private void refreshDeliveryTimes() {
        int travelMinutes = warehouseHelper.getSelectedWarehouseEtaMinutes();
        fastDeliveryWindow = DeliveryTimeUtils.calculateFastDelivery(travelMinutes);
        if (tvFastDeliveryTime != null) {
            tvFastDeliveryTime.setText(fastDeliveryWindow.displayText());
        }

        DeliveryTimeUtils.DeliveryWindow defaultScheduled = DeliveryTimeUtils.defaultScheduledWindow(travelMinutes);
        if (scheduledDeliveryWindow == null
                || !DeliveryTimeUtils.isValidScheduledWindow(scheduledDeliveryWindow.start, travelMinutes)) {
            scheduledDeliveryWindow = defaultScheduled;
        }
        if (tvScheduleDeliveryTime != null) {
            tvScheduleDeliveryTime.setText(scheduledDeliveryWindow.displayText());
        }
    }

    @Nullable
    private DeliveryTimeUtils.DeliveryWindow getActiveDeliveryWindow() {
        if (isFastDeliveryMode) {
            if (fastDeliveryWindow == null) {
                refreshDeliveryTimes();
            }
            return fastDeliveryWindow;
        }
        if (scheduledDeliveryWindow == null) {
            refreshDeliveryTimes();
        }
        return scheduledDeliveryWindow;
    }

    private String formatDeliveryTimestamp(@NonNull Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(calendar.getTime());
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
        TextView tvFilterProduct = dialogView.findViewById(R.id.tvVoucherFilterProduct);
        TextView tvFilterShipping = dialogView.findViewById(R.id.tvVoucherFilterShipping);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvSelectedCount.setText("Đang tải khuyến mãi...");
        tvSelectedTitle.setText("");

        VoucherOptionAdapter adapter = new VoucherOptionAdapter(
                voucherItems,
                selectedProductVoucher == null ? null : selectedProductVoucher.promotionId,
                selectedShippingVoucher == null ? null : selectedShippingVoucher.promotionId,
                (productVoucher, shippingVoucher) -> {
                    if (activeVoucherFilter == PromotionVoucherHelper.VoucherFilter.PRODUCT) {
                        selectedProductVoucher = productVoucher;
                    } else {
                        selectedShippingVoucher = shippingVoucher;
                    }
                    updateSelectedVoucherUi();
                    bindPaymentSummary();
                    updateVoucherSelectionText(tvSelectedCount, tvSelectedTitle);
                }
        );
        recyclerView.setAdapter(adapter);

        Runnable refreshFilteredList = () -> applyVoucherFilter(
                recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle
        );
        tvFilterProduct.setOnClickListener(v -> {
            activeVoucherFilter = PromotionVoucherHelper.VoucherFilter.PRODUCT;
            updateVoucherFilterTags(tvFilterProduct, tvFilterShipping);
            refreshFilteredList.run();
        });
        tvFilterShipping.setOnClickListener(v -> {
            activeVoucherFilter = PromotionVoucherHelper.VoucherFilter.SHIPPING;
            updateVoucherFilterTags(tvFilterProduct, tvFilterShipping);
            refreshFilteredList.run();
        });
        updateVoucherFilterTags(tvFilterProduct, tvFilterShipping);

        isSearchingVoucher = false;
        loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);

        BottomSheetDialog dialog = createHalfHeightBottomSheetDialog(dialogView);
        btnApplyVoucher.setOnClickListener(v -> dialog.dismiss());
        btnSearchVoucher.setOnClickListener(v -> {
            String code = edtVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                isSearchingVoucher = false;
                loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);
            } else {
                isSearchingVoucher = true;
                searchVoucher(code, recyclerView, tvSelectedCount, tvSelectedTitle, tvEmptyState, tvListTitle);
            }
        });
        dialog.show();
    }

    private void updateVoucherFilterTags(TextView productTag, TextView shippingTag) {
        boolean productSelected = activeVoucherFilter == PromotionVoucherHelper.VoucherFilter.PRODUCT;
        productTag.setBackground(null);
        shippingTag.setBackground(null);
        productTag.setTextColor(getColor(productSelected ? R.color.primary_main : R.color.neutral_60));
        shippingTag.setTextColor(getColor(productSelected ? R.color.neutral_60 : R.color.primary_main));
    }

    private void applyVoucherFilter(
            RecyclerView recyclerView,
            TextView selectedCountView,
            TextView selectedTitleView,
            TextView tvEmptyState,
            TextView tvListTitle
    ) {
        voucherItems.clear();
        for (VoucherOptionAdapter.VoucherItemUiModel item : allVoucherItems) {
            boolean showProduct = activeVoucherFilter == PromotionVoucherHelper.VoucherFilter.PRODUCT && !item.shipping;
            boolean showShipping = activeVoucherFilter == PromotionVoucherHelper.VoucherFilter.SHIPPING && item.shipping;
            if (showProduct || showShipping) {
                voucherItems.add(item);
            }
        }
        syncSelectedVoucherReferences();

        if (voucherItems.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (tvListTitle != null) {
                tvListTitle.setVisibility(View.GONE);
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (tvListTitle != null) {
                tvListTitle.setVisibility(View.VISIBLE);
            }
        }

        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter instanceof VoucherOptionAdapter) {
            ((VoucherOptionAdapter) adapter).setSelectedPromotionIds(
                    selectedProductVoucher == null ? null : selectedProductVoucher.promotionId,
                    selectedShippingVoucher == null ? null : selectedShippingVoucher.promotionId
            );
        } else if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateVoucherSelectionText(selectedCountView, selectedTitleView);
    }

    private void syncSelectedVoucherReferences() {
        if (selectedProductVoucher != null) {
            selectedProductVoucher = findVoucherItem(selectedProductVoucher.promotionId, false);
        }
        if (selectedShippingVoucher != null) {
            selectedShippingVoucher = findVoucherItem(selectedShippingVoucher.promotionId, true);
        }
    }

    @Nullable
    private VoucherOptionAdapter.VoucherItemUiModel findVoucherItem(
            @Nullable String promotionId,
            boolean shipping
    ) {
        if (promotionId == null) {
            return null;
        }
        for (VoucherOptionAdapter.VoucherItemUiModel item : allVoucherItems) {
            if (promotionId.equals(item.promotionId) && item.shipping == shipping) {
                return item;
            }
        }
        return null;
    }

    private List<PromotionTargetDto> cachedTargets = new ArrayList<>();
    private List<PromotionUsageDto> cachedUsages = new ArrayList<>();

    private void searchVoucher(String code, RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        isSearchingVoucher = true;

        allVoucherItems.clear();
        voucherItems.clear();
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
        recyclerView.setVisibility(View.GONE);
        tvListTitle.setVisibility(View.VISIBLE);
        tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Đang kiểm tra mã '" + code + "'...");
        selectedCountView.setText("Đang kiểm tra...");

        promotionApi.getPromotions(guestId, code, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (!isSearchingVoucher) {
                    return;
                }
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
                            renderVoucherOptions(found, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showVoucherEmptyState("Mã '" + code + "' không hợp lệ hoặc đã hết hạn.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                    });
                }
            }
            @Override public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
                if (!isSearchingVoucher) {
                    return;
                }
                runOnUiThread(() -> {
                    showVoucherEmptyState("Lỗi kết nối khi tìm mã. Vui lòng thử lại.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                });
            }
        });
    }

    private void showVoucherEmptyState(String message, RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        allVoucherItems.clear();
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
                    renderVoucherOptions(promotions, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
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
                                      TextView selectedCountView, TextView selectedTitleView,
                                      TextView tvEmptyState, TextView tvListTitle) {
        allVoucherItems.clear();
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
            if (!isSearchingVoucher && PromotionVoucherHelper.isPromotionExpired(promotion)) {
                continue;
            }
            PromotionTargetDto target = targetByPromotion.get(promotion.getPromotionId());
            PromotionUsageDto usage = usageByPromotion.get(promotion.getPromotionId());
            String disabledReason = disabledReason(promotion, target, usage);
            boolean enabled = disabledReason == null;

            allVoucherItems.add(new VoucherOptionAdapter.VoucherItemUiModel(
                    PromotionVoucherHelper.voucherTitle(promotion),
                    PromotionVoucherHelper.voucherCondition(promotion, target, disabledReason),
                    PromotionVoucherHelper.voucherExpiry(promotion),
                    R.drawable.ic_voucher,
                    promotion.getPromotionId(),
                    enabled,
                    promotion
            ));
        }

        java.util.Collections.sort(allVoucherItems, (v1, v2) -> {
            if (v1.enabled && !v2.enabled) {
                return -1;
            } else if (!v1.enabled && v2.enabled) {
                return 1;
            }
            return 0;
        });

        syncSelectedVoucherReferences();
        applyVoucherFilter(recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
        updateSelectedVoucherUi();
        bindPaymentSummary();
    }

    private BottomSheetDialog createHalfHeightBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int halfScreenHeight = getResources().getDisplayMetrics().heightPixels / 2;
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (view, windowInsets) -> {
                    int bottomInset = windowInsets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                    ).bottom;
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = halfScreenHeight + bottomInset;
                    view.setLayoutParams(layoutParams);
                    view.setPadding(0, 0, 0, bottomInset);
                    return windowInsets;
                });
                androidx.core.view.ViewCompat.requestApplyInsets(bottomSheet);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(halfScreenHeight, true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    private void showScheduleTimePicker() {
        int travelMinutes = warehouseHelper.getSelectedWarehouseEtaMinutes();
        DeliveryTimeUtils.DeliveryWindow minimumWindow = DeliveryTimeUtils.defaultScheduledWindow(travelMinutes);
        Calendar minimum = DeliveryTimeUtils.calculateScheduledMinimum(travelMinutes);
        Calendar initial = scheduledDeliveryWindow != null
                && DeliveryTimeUtils.isValidScheduledWindow(scheduledDeliveryWindow.start, travelMinutes)
                ? scheduledDeliveryWindow.start
                : minimumWindow.start;
        if (initial.before(minimum)) {
            initial = minimum;
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    DeliveryTimeUtils.DeliveryWindow picked =
                            DeliveryTimeUtils.resolveScheduledPick(travelMinutes, hourOfDay, minute);
                    if (!DeliveryTimeUtils.isValidScheduledWindow(picked.start, travelMinutes)) {
                        Toast.makeText(
                                this,
                                "Khung giờ phải sau " + DeliveryTimeUtils.formatMinimumScheduleHint(travelMinutes),
                                Toast.LENGTH_LONG
                        ).show();
                        picked = minimumWindow;
                    }
                    scheduledDeliveryWindow = picked;
                    if (tvScheduleDeliveryTime != null) {
                        tvScheduleDeliveryTime.setText(picked.displayText());
                    }
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void selectDeliveryMode(boolean isFastDelivery) {
        isFastDeliveryMode = isFastDelivery;
        if (layoutFastDelivery != null) {
            layoutFastDelivery.setBackgroundResource(isFastDelivery ? R.drawable.bg_selected : R.drawable.bg_normal);
        }
        if (layoutScheduleDelivery != null) {
            layoutScheduleDelivery.setBackgroundResource(isFastDelivery ? R.drawable.bg_normal : R.drawable.bg_selected);
        }
        if (!isFastDelivery && scheduledDeliveryWindow == null) {
            refreshDeliveryTimes();
        }
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
        if (tvCheckoutSelectedVoucher == null) {
            return;
        }
        int selectedCount = countSelectedVouchers();
        PromotionVoucherHelper.bindVoucherSelectionRow(
                tvVoucherLabel,
                tvCheckoutSelectedVoucher,
                selectedCount > 0 ? buildSelectedVoucherSummary() : null,
                getResources().getColor(R.color.primary_main, getTheme()),
                android.graphics.Color.parseColor("#616161")
        );
    }

    private int countSelectedVouchers() {
        int count = 0;
        if (selectedProductVoucher != null) {
            count++;
        }
        if (selectedShippingVoucher != null) {
            count++;
        }
        return count;
    }

    private String buildSelectedVoucherSummary() {
        if (selectedProductVoucher != null && selectedShippingVoucher != null) {
            return selectedProductVoucher.title + " + " + selectedShippingVoucher.title;
        }
        if (selectedProductVoucher != null) {
            return selectedProductVoucher.title;
        }
        if (selectedShippingVoucher != null) {
            return selectedShippingVoucher.title;
        }
        return "Chọn hoặc nhập mã";
    }

    private void updateVoucherSelectionText(TextView selectedCountView, TextView selectedTitleView) {
        int selectedCount = countSelectedVouchers();
        if (selectedCount > 0) {
            selectedCountView.setText(selectedCount + " mã đã được chọn");
            selectedTitleView.setText(buildSelectedVoucherSummary());
        } else if (allVoucherItems.isEmpty()) {
            selectedCountView.setText("Không có mã khuyến mãi khả dụng");
            selectedTitleView.setText("");
        } else {
            selectedCountView.setText("Chưa chọn mã khuyến mãi");
            selectedTitleView.setText("");
        }
    }

    private void restoreSelectedVoucherFromIntent() {
        PromotionDto productPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_product_voucher_dto");
        PromotionDto shippingPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_shipping_voucher_dto");
        String productId = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_ID);
        String productTitle = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_TITLE);
        String shippingId = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_ID);
        String shippingTitle = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_TITLE);

        if (productTitle != null && !productTitle.trim().isEmpty()) {
            selectedProductVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    productTitle, "", "", R.drawable.ic_voucher, productId, true, productPromotion
            );
        }
        if (shippingTitle != null && !shippingTitle.trim().isEmpty()) {
            selectedShippingVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    shippingTitle, "", "", R.drawable.ic_voucher, shippingId, true, shippingPromotion
            );
        }

        String legacyId = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_ID);
        String legacyTitle = getIntent().getStringExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_TITLE);
        PromotionDto legacyPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_voucher_dto");
        if (legacyTitle != null && !legacyTitle.trim().isEmpty()
                && selectedProductVoucher == null && selectedShippingVoucher == null) {
            VoucherOptionAdapter.VoucherItemUiModel legacy = new VoucherOptionAdapter.VoucherItemUiModel(
                    legacyTitle, "", "", R.drawable.ic_voucher, legacyId, true, legacyPromotion
            );
            if (legacy.shipping) {
                selectedShippingVoucher = legacy;
            } else {
                selectedProductVoucher = legacy;
            }
        }
        updateSelectedVoucherUi();
    }

    private boolean validateSelectedVouchersBeforeCheckout() {
        String invalidReason = validateVoucherSelection(selectedProductVoucher);
        if (invalidReason != null) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            Toast.makeText(this, "Voucher sản phẩm không còn hợp lệ: " + invalidReason, Toast.LENGTH_LONG).show();
            selectedProductVoucher = null;
            updateSelectedVoucherUi();
            bindPaymentSummary();
            return false;
        }
        invalidReason = validateVoucherSelection(selectedShippingVoucher);
        if (invalidReason != null) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            Toast.makeText(this, "Voucher vận chuyển không còn hợp lệ: " + invalidReason, Toast.LENGTH_LONG).show();
            selectedShippingVoucher = null;
            updateSelectedVoucherUi();
            bindPaymentSummary();
            return false;
        }
        return true;
    }

    @Nullable
    private String validateVoucherSelection(@Nullable VoucherOptionAdapter.VoucherItemUiModel voucher) {
        if (voucher == null || voucher.promotion == null) {
            return null;
        }
        PromotionTargetDto target = null;
        for (PromotionTargetDto t : cachedTargets) {
            if (voucher.promotionId != null && voucher.promotionId.equals(t.getPromotionId())) {
                target = t;
                break;
            }
        }
        PromotionUsageDto usage = null;
        for (PromotionUsageDto u : cachedUsages) {
            if (voucher.promotionId != null && voucher.promotionId.equals(u.getPromotionId())) {
                usage = u;
                break;
            }
        }
        return disabledReason(voucher.promotion, target, usage);
    }

    private void prefetchVoucherData() {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        promotionApi.getPromotionTargets().enqueue(new Callback<ApiListResponseDto<PromotionTargetDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionTargetDto>> call, Response<ApiListResponseDto<PromotionTargetDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedTargets.clear();
                    cachedTargets.addAll(response.body().getData());
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
                }
            }
            @Override public void onFailure(Call<ApiListResponseDto<PromotionUsageDto>> call, Throwable t) {}
        });
    }

    private void validateAppliedVoucher() {
        if (cartItems.isEmpty() || currentSubtotal <= 0) {
            return;
        }
        boolean hadProductVoucher = selectedProductVoucher != null;
        boolean hadShippingVoucher = selectedShippingVoucher != null;
        boolean changed = false;
        if (validateVoucherSelection(selectedProductVoucher) != null) {
            selectedProductVoucher = null;
            changed = true;
        }
        if (validateVoucherSelection(selectedShippingVoucher) != null) {
            selectedShippingVoucher = null;
            changed = true;
        }
        if (changed) {
            updateSelectedVoucherUi();
            bindPaymentSummary();
            if (hadProductVoucher || hadShippingVoucher) {
                Toast.makeText(this, "Voucher đã được hủy vì giỏ hàng không còn đáp ứng điều kiện áp dụng.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String disabledReason(PromotionDto promotion, PromotionTargetDto target, PromotionUsageDto usage) {
        return PromotionVoucherHelper.evaluateDisabledReason(
                promotion,
                target,
                usage,
                currentSubtotal,
                guestId,
                PromotionVoucherHelper.matchesPromotionTarget(
                        promotion,
                        target,
                        buildVoucherCartLines()
                )
        );
    }

    private List<PromotionVoucherHelper.VoucherCartLine> buildVoucherCartLines() {
        List<PromotionVoucherHelper.VoucherCartLine> lines = new ArrayList<>();
        for (CartDto.CartItemDto item : cartItems) {
            lines.add(new PromotionVoucherHelper.VoucherCartLine(item.getSku(), item.getProduct()));
        }
        return lines;
    }

    private String cartLineKey(@Nullable String sku, double selectedWeight) {
        return (sku == null ? "" : sku) + "#" + trimTrailingZeros(selectedWeight > 0 ? selectedWeight : 1.0);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                showProgress("Đang đồng bộ và khởi tạo đơn hàng VNPay...");
                String tempOrderId = data != null ? data.getStringExtra("orderId") : ("VG" + System.currentTimeMillis());
                createOrderAfterVnpaySuccess(tempOrderId);
            } else {
                findViewById(R.id.btnPlaceOrder).setEnabled(true);
                Toast.makeText(this, "Thanh toán VNPay thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            android.widget.RelativeLayout rootLayout = new android.widget.RelativeLayout(this);
            rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#99000000"));
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setGravity(android.view.Gravity.CENTER);
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
            progressTextView = new android.widget.TextView(this);
            progressTextView.setText(message);
            progressTextView.setTextColor(android.graphics.Color.WHITE);
            progressTextView.setTextSize(16);
            progressTextView.setGravity(android.view.Gravity.CENTER);
            progressTextView.setPadding(30, 32, 30, 0);
            container.addView(progressBar);
            container.addView(progressTextView);
            android.widget.RelativeLayout.LayoutParams layoutParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            rootLayout.addView(container, layoutParams);
            progressDialog.setContentView(rootLayout);
            progressDialog.setCancelable(false);
        }
        if (progressTextView != null) {
            progressTextView.setText(message);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private interface PickerCallback {
        void onSelected(String value);
    }
}
