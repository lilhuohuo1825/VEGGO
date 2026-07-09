package com.veggo.app.presentation.checkout;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.KeyEvent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import java.util.concurrent.Executor;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.LocationOptionAdapter;
import com.veggo.app.adapter.PaymentItemAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.core.notification.RecurringConfirmationHelper;
import com.veggo.app.core.utils.DeliveryTimeUtils;
import com.veggo.app.core.utils.ProductCatalogImageResolver;
import com.veggo.app.core.utils.WarehouseDistanceUtils;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.OrderDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.data.repository.WalletRepository;
import com.veggo.app.data.remote.dto.WalletDto;
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;
import com.veggo.app.presentation.cart.CartViewModel;
import com.veggo.app.presentation.profile.AddressFormActivity;
import com.veggo.app.presentation.order.RecurringCheckoutStore;
import com.veggo.app.presentation.promotion.PromotionVoucherHelper;
import com.veggo.app.presentation.order.RecurringOrderStore;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
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

public class CheckoutActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_VOUCHER_ID = "extra_selected_voucher_id";
    public static final String EXTRA_SELECTED_VOUCHER_TITLE = "extra_selected_voucher_title";
    public static final String EXTRA_SELECTED_PRODUCT_VOUCHER_ID = "extra_selected_product_voucher_id";
    public static final String EXTRA_SELECTED_PRODUCT_VOUCHER_TITLE = "extra_selected_product_voucher_title";
    public static final String EXTRA_SELECTED_SHIPPING_VOUCHER_ID = "extra_selected_shipping_voucher_id";
    public static final String EXTRA_SELECTED_SHIPPING_VOUCHER_TITLE = "extra_selected_shipping_voucher_title";
    public static final String EXTRA_SELECTED_CART_LINE_KEYS = "extra_selected_cart_line_keys";
    public static final String EXTRA_TRANSFERRED_CHECKOUT = "extra_transferred_checkout";
    public static final String EXTRA_BUY_NOW = "extra_buy_now";
    public static final String EXTRA_BUY_NOW_PRODUCT_ID = "extra_buy_now_product_id";
    public static final String EXTRA_BUY_NOW_SKU = "extra_buy_now_sku";
    public static final String EXTRA_BUY_NOW_NAME = "extra_buy_now_name";
    public static final String EXTRA_BUY_NOW_PRICE = "extra_buy_now_price";
    public static final String EXTRA_BUY_NOW_ORIGINAL_PRICE = "extra_buy_now_original_price";
    public static final String EXTRA_BUY_NOW_IMAGE = "extra_buy_now_image";
    public static final String EXTRA_BUY_NOW_WEIGHT = "extra_buy_now_weight";
    public static final String EXTRA_BUY_NOW_SELECTED_WEIGHT = "extra_buy_now_selected_weight";
    public static final String EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS = "extra_buy_now_has_weight_options";
    public static final String EXTRA_BUY_NOW_QUANTITY = "extra_buy_now_quantity";
    public static final String EXTRA_BUY_NOW_CATEGORY_ID = "extra_buy_now_category_id";
    public static final String EXTRA_BUY_NOW_SUBCATEGORY_ID = "extra_buy_now_subcategory_id";
    public static final String EXTRA_BUY_NOW_CARBON_POINT = "extra_buy_now_carbon_point";
    public static final String EXTRA_RECURRING_CHECKOUT = "extra_recurring_checkout";
    public static final String EXTRA_RECURRING_ORDER_ID = "extra_recurring_order_id";
    public static final String EXTRA_RECURRING_OCCURRENCE_DATE = "extra_recurring_occurrence_date";

    private static final long FREE_SHIPPING_THRESHOLD = 300_000L;
    private static final long DEFAULT_SHIPPING_FEE = 30_000L;

    private TextView tvCheckoutNote;
    private TextView tvFastDeliveryTime;
    private TextView tvScheduleDeliveryTime;
    private TextView tvAddressName;
    private TextView tvAddressPhone;
    private TextView tvAddressDetail;
    private TextView tvAddressDefaultBadge;
    private View layoutAddressSaved;
    private View layoutAddressEmpty;
    private TextView tvVoucherLabel;
    private TextView tvCheckoutDeliveryOriginAddress;
    private TextView tvCheckoutDeliveryOriginDistance;
    private TextView tvCheckoutSubtotal;
    private TextView tvCheckoutVoucherDiscount;
    private TextView tvCheckoutProductDiscount;
    private TextView tvCheckoutShippingFee;
    private TextView tvCheckoutShippingDiscount;
    private TextView tvCheckoutPaymentTotal;
    private TextView tvCheckoutBottomTotal;
    private TextView tvCheckoutCarbonPoints;
    private TextView tvCheckoutSelectedVoucher;
    private LinearLayout layoutToggleProducts;
    private View layoutFastDelivery;
    private View layoutScheduleDelivery;
    private PaymentItemAdapter paymentItemAdapter;
    private CartViewModel cartViewModel;
    private AddressRepository addressRepository;
    private WalletRepository walletRepository;
    private double veggoPayBalance = 0;
    private List<WalletDto.LinkedBankDto> veggoPayLinkedBanks = new ArrayList<>();
    private String currentWalletPassword = "";
    private String customerId;
    private AppCompatRadioButton radioPaymentMomo;
    private AppCompatRadioButton radioPaymentBank;
    private AppCompatRadioButton radioPaymentCod;
    private AppCompatRadioButton radioPaymentVnpay;
    private View layoutPaymentMomo;
    private TextView tvPaymentMomoLabel;
    private TextView btnVeggoPayTopUp;
    private boolean pendingSelectVeggoPayAfterTopUp = false;
    private String selectedPaymentMethod = "cod";
    private long currentPaymentTotal;
    private long currentSubtotal;
    private long currentProductDiscount;
    private long currentVoucherDiscount;
    private long currentShippingFee;
    private long currentShippingDiscount;
    private double currentCarbonPoints;
    private double currentCarbonEmission;
    private int currentProductCount;
    private VoucherOptionAdapter.VoucherItemUiModel selectedProductVoucher;
    private VoucherOptionAdapter.VoucherItemUiModel selectedShippingVoucher;
    private final List<PaymentItemAdapter.PaymentItemUiModel> previewItems = new ArrayList<>();
    private final List<CartDto.CartItemDto> cartItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> voucherItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> allVoucherItems = new ArrayList<>();
    private final List<LocationOptionAdapter.LocationItemUiModel> locationItems = new ArrayList<>();
    private Set<String> selectedCartLineKeys;
    private int selectedLocationIndex;
    private boolean buyNowMode;
    private boolean transferredCheckoutMode;
    private boolean recurringCheckoutMode;
    private String recurringOrderId;
    private String recurringOccurrenceDate;
    private RecurringCheckoutStore.CheckoutContext recurringCheckoutContext;
    private boolean isFastDeliveryMode = true;
    private DeliveryTimeUtils.DeliveryWindow fastDeliveryWindow;
    private DeliveryTimeUtils.DeliveryWindow scheduledDeliveryWindow;
    private boolean isSearchingVoucher = false;
    private PromotionVoucherHelper.VoucherFilter activeVoucherFilter = PromotionVoucherHelper.VoucherFilter.PRODUCT;
    private final CheckoutWarehouseHelper warehouseHelper = new CheckoutWarehouseHelper();
    private final ActivityResultLauncher<Intent> addAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    return;
                }
                if (result.getData() == null) {
                    loadAddresses();
                    return;
                }
                addLocationFromResult(result.getData());
                updateAddressDisplayMode();
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
        tvFastDeliveryTime = findViewById(R.id.tvFastDeliveryTime);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        layoutToggleProducts = findViewById(R.id.layoutToggleProducts);
        tvAddressName = findViewById(R.id.tvAddressName);
        tvAddressPhone = findViewById(R.id.tvAddressPhone);
        tvAddressDetail = findViewById(R.id.tvAddressDetail);
        tvAddressDefaultBadge = findViewById(R.id.tvAddressDefaultBadge);
        layoutAddressSaved = findViewById(R.id.layoutAddressSaved);
        layoutAddressEmpty = findViewById(R.id.layoutAddressEmpty);
        TextView btnOpenAddressBook = findViewById(R.id.btnOpenAddressBook);
        tvVoucherLabel = findViewById(R.id.tvVoucherLabel);
        tvCheckoutDeliveryOriginAddress = findViewById(R.id.tvCheckoutDeliveryOriginAddress);
        tvCheckoutDeliveryOriginDistance = findViewById(R.id.tvCheckoutDeliveryOriginDistance);
        tvCheckoutSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvCheckoutVoucherDiscount = findViewById(R.id.tvCheckoutVoucherDiscount);
        tvCheckoutProductDiscount = findViewById(R.id.tvCheckoutProductDiscount);
        tvCheckoutShippingFee = findViewById(R.id.tvCheckoutShippingFee);
        tvCheckoutShippingDiscount = findViewById(R.id.tvCheckoutShippingDiscount);
        tvCheckoutPaymentTotal = findViewById(R.id.tvCheckoutPaymentTotal);
        tvCheckoutBottomTotal = findViewById(R.id.tvCheckoutBottomTotal);
        tvCheckoutCarbonPoints = findViewById(R.id.tvCheckoutCarbonPoints);
        tvCheckoutSelectedVoucher = findViewById(R.id.tvCheckoutSelectedVoucher);
        layoutFastDelivery = findViewById(R.id.layoutFastDelivery);
        layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);
        radioPaymentMomo = findViewById(R.id.radioPaymentMomo);
        radioPaymentBank = findViewById(R.id.radioPaymentCard);
        radioPaymentCod = findViewById(R.id.radioPaymentCod);
        radioPaymentVnpay = findViewById(R.id.radioPaymentVnpay);
        layoutPaymentMomo = findViewById(R.id.layoutPaymentMomo);
        tvPaymentMomoLabel = findViewById(R.id.tvPaymentMomoLabel);
        btnVeggoPayTopUp = findViewById(R.id.btnVeggoPayTopUp);
        tvCheckoutNote.setText("");
        tvCheckoutNote.setHint("Nhập ghi chú");
        buyNowMode = getIntent().getBooleanExtra(EXTRA_BUY_NOW, false);
        transferredCheckoutMode = getIntent().getBooleanExtra(EXTRA_TRANSFERRED_CHECKOUT, false);
        recurringCheckoutMode = getIntent().getBooleanExtra(EXTRA_RECURRING_CHECKOUT, false);
        recurringOrderId = getIntent().getStringExtra(EXTRA_RECURRING_ORDER_ID);
        recurringOccurrenceDate = getIntent().getStringExtra(EXTRA_RECURRING_OCCURRENCE_DATE);
        ArrayList<String> selectedLineKeys = getIntent().getStringArrayListExtra(EXTRA_SELECTED_CART_LINE_KEYS);
        selectedCartLineKeys = selectedLineKeys == null || selectedLineKeys.isEmpty()
                ? null
                : new HashSet<>(selectedLineKeys);
        restoreSelectedVoucherFromIntent();

        customerId = resolveCustomerId();
        setupViewModel();
        setupProducts(rvCheckoutProducts);
        setupPaymentMethods();
        loadAddresses();
        selectDeliveryMode(true);
        prefetchVoucherData();
        layoutAddress.setOnClickListener(v -> showLocationDialog());
        if (btnOpenAddressBook != null) {
            btnOpenAddressBook.setOnClickListener(v -> showLocationDialog());
        }
        layoutDeliveryOrigin.setOnClickListener(v -> warehouseHelper.showSelectionDialog(this, this::bindSelectedWarehouseUi, null));
        layoutVoucher.setOnClickListener(v -> showVoucherDialog());
        layoutNote.setOnClickListener(v -> showNoteDialog());
        layoutFastDelivery.setOnClickListener(v -> selectDeliveryMode(true));
        layoutScheduleDelivery.setOnClickListener(v -> {
            selectDeliveryMode(false);
            showScheduleTimePicker();
        });
        btnPlaceOrder.setOnClickListener(v -> {
            btnPlaceOrder.setEnabled(false);
            createOrderAndOpenPaymentStep();
        });

        if (buyNowMode) {
            bindBuyNowItemFromIntent();
        } else if (recurringCheckoutMode) {
            bindRecurringCheckoutFromStore();
        } else if (transferredCheckoutMode) {
            CartDto transferredCart = PendingCheckoutStore.consumeTransferredCart();
            if (transferredCart != null && transferredCart.getItems() != null && !transferredCart.getItems().isEmpty()) {
                bindCart(transferredCart);
            } else {
                Toast.makeText(this, "Không tìm thấy sản phẩm đang thanh toán", Toast.LENGTH_SHORT).show();
            }
        } else if (customerId != null && !customerId.trim().isEmpty()) {
            cartViewModel.fetchCart(customerId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideCartRepository(this));
        cartViewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);
        addressRepository = AppModule.provideAddressRepository(this);
        walletRepository = AppModule.provideWalletRepository();

        cartViewModel.getCartData().observe(this, this::bindCart);
        cartViewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        loadWalletInfo();
    }

    private void loadWalletInfo() {
        if (customerId == null || customerId.trim().isEmpty()) return;
        walletRepository.getWalletInfo(customerId, new WalletRepository.ResultCallback<WalletDto>() {
            @Override
            public void onSuccess(WalletDto result) {
                runOnUiThread(() -> {
                    veggoPayBalance = result.getBalance();
                    veggoPayLinkedBanks = result.getLinkedBanks() == null ? new ArrayList<>() : result.getLinkedBanks();
                    if (tvPaymentMomoLabel != null) {
                        tvPaymentMomoLabel.setText("Ví VeggoPay (Số dư: " + com.veggo.app.core.utils.CurrencyFormatter.formatVnd((long) veggoPayBalance) + ")");
                    }
                    updateVeggoPayPaymentRowUi();
                });
            }

            @Override
            public void onError(Throwable error) {
                // Ignore or log
            }
        });
    }

    private void setupProducts(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        paymentItemAdapter = new PaymentItemAdapter(new ArrayList<>());
        recyclerView.setAdapter(paymentItemAdapter);
    }

    private void restoreSelectedVoucherFromIntent() {
        PromotionDto productPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_product_voucher_dto");
        PromotionDto shippingPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_shipping_voucher_dto");
        String productId = getIntent().getStringExtra(EXTRA_SELECTED_PRODUCT_VOUCHER_ID);
        String productTitle = getIntent().getStringExtra(EXTRA_SELECTED_PRODUCT_VOUCHER_TITLE);
        String shippingId = getIntent().getStringExtra(EXTRA_SELECTED_SHIPPING_VOUCHER_ID);
        String shippingTitle = getIntent().getStringExtra(EXTRA_SELECTED_SHIPPING_VOUCHER_TITLE);

        if (hasVoucherText(productTitle)) {
            selectedProductVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    productTitle, "", "", R.drawable.ic_voucher, productId, true, productPromotion
            );
        }
        if (hasVoucherText(shippingTitle)) {
            selectedShippingVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    shippingTitle, "", "", R.drawable.ic_voucher, shippingId, true, shippingPromotion
            );
        }

        String legacyId = getIntent().getStringExtra(EXTRA_SELECTED_VOUCHER_ID);
        String legacyTitle = getIntent().getStringExtra(EXTRA_SELECTED_VOUCHER_TITLE);
        PromotionDto legacyPromotion = (PromotionDto) getIntent().getSerializableExtra("extra_selected_voucher_dto");
        if (hasVoucherText(legacyTitle) && selectedProductVoucher == null && selectedShippingVoucher == null) {
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

    private boolean hasVoucherText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setupPaymentMethods() {
        selectPaymentMethod("cod");
        radioPaymentMomo.setOnClickListener(v -> {
            if (canPayWithVeggoPay()) {
                selectPaymentMethod("veggopay");
            } else {
                openVeggoPayTopUp();
            }
        });
        radioPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        radioPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        if (radioPaymentVnpay != null) radioPaymentVnpay.setOnClickListener(v -> selectPaymentMethod("vnpay"));
        View layoutPaymentCod = findViewById(R.id.layoutPaymentCod);
        View layoutPaymentBank = findViewById(R.id.layoutPaymentBank);
        View layoutPaymentVnpay = findViewById(R.id.layoutPaymentVnpay);
        if (layoutPaymentMomo != null) {
            layoutPaymentMomo.setOnClickListener(v -> {
                if (canPayWithVeggoPay()) {
                    selectPaymentMethod("veggopay");
                } else {
                    openVeggoPayTopUp();
                }
            });
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

        if (btnVeggoPayTopUp != null) {
            btnVeggoPayTopUp.setOnClickListener(v -> openVeggoPayTopUp());
        }
        updateVeggoPayPaymentRowUi();
    }

    private void selectPaymentMethod(String method) {
        selectedPaymentMethod = method;
        if (radioPaymentMomo != null) radioPaymentMomo.setChecked("veggopay".equals(method));
        if (radioPaymentBank != null) radioPaymentBank.setChecked("bank".equals(method));
        if (radioPaymentCod  != null) radioPaymentCod.setChecked("cod".equals(method));
        if (radioPaymentVnpay != null) radioPaymentVnpay.setChecked("vnpay".equals(method));
    }

    private void createOrderAndOpenPaymentStep() {
        if ("veggopay".equals(selectedPaymentMethod)) {
            if (veggoPayBalance < currentPaymentTotal) {
                Toast.makeText(this, "Số dư ví VeggoPay không đủ để thanh toán. Vui lòng nạp thêm tiền.", Toast.LENGTH_LONG).show();
                findViewById(R.id.btnPlaceOrder).setEnabled(true);
                return;
            }
        }
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (locationItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ vào sổ địa chỉ để đặt hàng", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (selectedLocationIndex < 0 || selectedLocationIndex >= locationItems.size()
                || tvAddressDetail.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng hợp lệ", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (!validateSelectedVouchersBeforeCheckout()) {
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }

        if ("veggopay".equals(selectedPaymentMethod)) {
            com.veggo.app.core.preferences.AppPreferences prefs = new com.veggo.app.core.preferences.AppPreferences(this);
            if (prefs.isBiometricEnabled(customerId) && checkBiometricAvailableForPayment()) {
                showBiometricPaymentPrompt(prefs.getBiometricPin(customerId));
            } else {
                showPaymentPasswordDialog();
            }
        } else if ("vnpay".equals(selectedPaymentMethod)) {
            String tempOrderId = "VG" + System.currentTimeMillis();
            openVnpayPayment(tempOrderId);
        } else {
            executeCreateOrderRequest();
        }
    }

    private void showPaymentPasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggopay_password_input);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvPinTitle);
        TextView tvDesc = dialog.findViewById(R.id.tvPinDescription);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText("Thanh toán VeggoPay");
        tvDesc.setText("Nhập mật khẩu ví 6 số để xác nhận thanh toán đơn hàng");

        EditText[] pinFields = new EditText[] {
                dialog.findViewById(R.id.edtPin1),
                dialog.findViewById(R.id.edtPin2),
                dialog.findViewById(R.id.edtPin3),
                dialog.findViewById(R.id.edtPin4),
                dialog.findViewById(R.id.edtPin5),
                dialog.findViewById(R.id.edtPin6)
        };

        for (int i = 0; i < pinFields.length; i++) {
            final int index = i;
            pinFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < pinFields.length - 1) {
                            pinFields[index + 1].requestFocus();
                        } else {
                            btnConfirm.performClick();
                        }
                    }
                }
            });
            pinFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (pinFields[index].getText().length() == 0 && index > 0) {
                        pinFields[index - 1].requestFocus();
                        pinFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }

        pinFields[0].requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
        });

        btnConfirm.setOnClickListener(v -> {
            StringBuilder pinBuilder = new StringBuilder();
            for (EditText edt : pinFields) {
                pinBuilder.append(edt.getText().toString().trim());
            }
            String pinCode = pinBuilder.toString();
            if (pinCode.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            currentWalletPassword = pinCode;
            executeCreateOrderRequest();
        });

        dialog.show();
    }

    private boolean checkBiometricAvailableForPayment() {
        BiometricManager biometricManager = BiometricManager.from(this);
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private int checkoutBiometricFailedCount = 0;
    private BiometricPrompt checkoutBiometricPrompt;
    private boolean isCheckoutTransitioningToPin = false;

    private void showBiometricPaymentPrompt(final String savedPin) {
        checkoutBiometricFailedCount = 0;
        isCheckoutTransitioningToPin = false;
        Executor executor = ContextCompat.getMainExecutor(this);
        checkoutBiometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                runOnUiThread(() -> {
                    if (!isCheckoutTransitioningToPin) {
                        isCheckoutTransitioningToPin = true;
                        Toast.makeText(CheckoutActivity.this, "Xác thực thất bại/đã hủy. Vui lòng nhập mã PIN.", Toast.LENGTH_SHORT).show();
                        showPaymentPasswordDialog();
                    }
                });
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                runOnUiThread(() -> {
                    currentWalletPassword = savedPin;
                    executeCreateOrderRequest();
                });
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                checkoutBiometricFailedCount++;
                runOnUiThread(() -> {
                    if (checkoutBiometricFailedCount >= 3 && !isCheckoutTransitioningToPin) {
                        isCheckoutTransitioningToPin = true;
                        Toast.makeText(CheckoutActivity.this, "Thử lại quá 3 lần. Vui lòng sử dụng mã PIN.", Toast.LENGTH_SHORT).show();
                        if (checkoutBiometricPrompt != null) {
                            checkoutBiometricPrompt.cancelAuthentication();
                        }
                        showPaymentPasswordDialog();
                    }
                });
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực thanh toán")
                .setSubtitle("Xác thực sinh trắc học để thanh toán VeggoPay")
                .setNegativeButtonText("Dùng mã PIN")
                .build();

        checkoutBiometricPrompt.authenticate(promptInfo);
    }

    private void executeCreateOrderRequest() {
        showProgress("Đang gửi yêu cầu đặt đơn hàng...");
        OrderApi orderApi = com.veggo.app.core.network.ApiClient.createService(OrderApi.class);
        orderApi.createOrder(buildOrderPayload()).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                hideProgress();
                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = "Không thể tạo đơn hàng";
                    if (response.errorBody() != null) {
                        try {
                            org.json.JSONObject errObj = new org.json.JSONObject(response.errorBody().string());
                            if (errObj.has("message")) {
                                errorMsg = errObj.getString("message");
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(CheckoutActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnPlaceOrder).setEnabled(true);
                    return;
                }
                OrderDto order = response.body();
                String orderId = order.getOrderId();
                if (orderId == null || orderId.trim().isEmpty()) {
                    orderId = order.getId();
                }
                openPaymentStep(orderId);
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                hideProgress();
                Toast.makeText(CheckoutActivity.this, "Không thể tạo đơn hàng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnPlaceOrder).setEnabled(true);
            }
        });
    }

    private void bindRecurringCheckoutFromStore() {
        RecurringCheckoutStore checkoutStore = new RecurringCheckoutStore(this);
        RecurringCheckoutStore.CheckoutSession session = checkoutStore.consumeSession();
        if (session == null || session.cart == null || session.cart.getItems() == null || session.cart.getItems().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm định kỳ", Toast.LENGTH_SHORT).show();
            return;
        }
        recurringCheckoutContext = session.context;
        bindCart(session.cart);
        selectDeliveryMode(false);
        DeliveryTimeUtils.DeliveryWindow window = RecurringConfirmationHelper.buildDeliveryWindow(
                recurringOccurrenceDate,
                recurringCheckoutContext == null ? "" : recurringCheckoutContext.deliverySlot
        );
        if (window != null) {
            scheduledDeliveryWindow = window;
            if (tvScheduleDeliveryTime != null) {
                tvScheduleDeliveryTime.setText(window.displayText());
            }
        }
    }

    private void bindBuyNowItemFromIntent() {
        ProductDto product = new ProductDto();
        product.setId(getIntent().getStringExtra(EXTRA_BUY_NOW_PRODUCT_ID));
        product.setSku(getIntent().getStringExtra(EXTRA_BUY_NOW_SKU));
        product.setProductName(getIntent().getStringExtra(EXTRA_BUY_NOW_NAME));
        product.setPrice(getIntent().getLongExtra(EXTRA_BUY_NOW_PRICE, 0));
        product.setOriginalPrice(getIntent().getLongExtra(EXTRA_BUY_NOW_ORIGINAL_PRICE, 0));
        product.setImageUrl(getIntent().getStringExtra(EXTRA_BUY_NOW_IMAGE));
        product.setWeight(getIntent().getStringExtra(EXTRA_BUY_NOW_WEIGHT));
        product.setCategoryId(getIntent().getStringExtra(EXTRA_BUY_NOW_CATEGORY_ID));
        product.setSubcategoryId(getIntent().getStringExtra(EXTRA_BUY_NOW_SUBCATEGORY_ID));
        product.setCarbonSavingPoint(getIntent().getDoubleExtra(EXTRA_BUY_NOW_CARBON_POINT, 0));

        boolean hasWeightOptions = getIntent().getBooleanExtra(EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS, false);
        double selectedWeight = getIntent().getDoubleExtra(EXTRA_BUY_NOW_SELECTED_WEIGHT, 1.0);
        if (hasWeightOptions) {
            List<Double> weights = new ArrayList<>();
            weights.add(selectedWeight > 0 ? selectedWeight : 1.0);
            product.setWeightOptions(weights);
        }

        CartDto.CartItemDto item = new CartDto.CartItemDto();
        item.setSku(safeText(product.getSku()));
        item.setProduct(product);
        item.setQuantity(Math.max(1, getIntent().getIntExtra(EXTRA_BUY_NOW_QUANTITY, 1)));
        item.setSelectedWeight(selectedWeight > 0 ? selectedWeight : 1.0);

        CartDto buyNowCart = new CartDto();
        List<CartDto.CartItemDto> items = new ArrayList<>();
        items.add(item);
        buyNowCart.setItems(items);
        bindCart(buyNowCart);
    }

    private void openPaymentStep(String orderId) {
        Intent intent = new Intent(this, QrPaymentActivity.class);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_METHOD, selectedPaymentMethod);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_AMOUNT, currentPaymentTotal);
        intent.putExtra(QrPaymentActivity.EXTRA_ITEM_COUNT, currentProductCount);
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_CODE,
                "bank".equals(selectedPaymentMethod) ? orderId : buildPaymentCode());
        intent.putExtra(QrPaymentActivity.EXTRA_ORDER_ID, orderId);
        intent.putExtra(QrPaymentActivity.EXTRA_SHOW_SUCCESS_IMMEDIATELY,
                "cod".equals(selectedPaymentMethod) || "vnpay".equals(selectedPaymentMethod) || "veggopay".equals(selectedPaymentMethod));
        intent.putExtra(QrPaymentActivity.EXTRA_CLEAR_CART_ON_SUCCESS, !buyNowMode && !transferredCheckoutMode && !recurringCheckoutMode);
        intent.putExtra(QrPaymentActivity.EXTRA_CUSTOMER_ID, customerId);
        if (!buyNowMode && !transferredCheckoutMode && !recurringCheckoutMode) {
            intent.putStringArrayListExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_SKUS, selectedCleanupSkus());
            intent.putExtra(QrPaymentActivity.EXTRA_CART_CLEANUP_WEIGHTS, selectedCleanupWeights());
        }
        if (recurringCheckoutMode) {
            intent.putExtra(QrPaymentActivity.EXTRA_RECURRING_ORDER_ID, recurringOrderId);
            intent.putExtra(QrPaymentActivity.EXTRA_RECURRING_OCCURRENCE_DATE, recurringOccurrenceDate);
        }
        startActivity(intent);
        finish();
    }

    private void openVnpayPayment(String orderId) {
        showProgress("Đang tạo liên kết thanh toán VNPay...");
        new Thread(() -> {
            try {
                com.veggo.app.data.remote.api.PaymentApi paymentApi =
                    com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.PaymentApi.class);
                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("orderId", orderId);
                body.put("amount", currentPaymentTotal);
                body.put("orderInfo", "Thanh toan don hang VEGGO " + orderId);
                retrofit2.Response<com.veggo.app.data.remote.dto.PaymentUrlDto> resp =
                    paymentApi.createVnpayUrl(body).execute();
                android.util.Log.d("VNPAY", "HTTP code: " + resp.code() + " successful: " + resp.isSuccessful());
                if (resp.isSuccessful() && resp.body() != null) {
                    String paymentUrl = resp.body().getPaymentUrl();
                    android.util.Log.d("VNPAY", "paymentUrl: " + (paymentUrl != null ? paymentUrl.substring(0, Math.min(100, paymentUrl.length())) : "NULL"));
                    if (paymentUrl == null || paymentUrl.isEmpty()) {
                        runOnUiThread(() -> {
                            hideProgress();
                            Toast.makeText(this, "Lỗi: paymentUrl rỗng từ server", Toast.LENGTH_LONG).show();
                            findViewById(R.id.btnPlaceOrder).setEnabled(true);
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        hideProgress();
                        Intent intent = new Intent(this, VnpayWebViewActivity.class);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_ORDER_ID, orderId);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_CUSTOMER_ID, customerId);
                        intent.putExtra(VnpayWebViewActivity.EXTRA_CLEAR_CART, !buyNowMode && !transferredCheckoutMode && !recurringCheckoutMode);
                        if (!buyNowMode && !transferredCheckoutMode && !recurringCheckoutMode) {
                            intent.putStringArrayListExtra(VnpayWebViewActivity.EXTRA_CLEANUP_SKUS, selectedCleanupSkus());
                            intent.putExtra(VnpayWebViewActivity.EXTRA_CLEANUP_WEIGHTS, selectedCleanupWeights());
                        }
                        startActivityForResult(intent, 1001);
                    });
                } else {
                    String errBody = "";
                    try { if (resp.errorBody() != null) errBody = resp.errorBody().string(); } catch (Exception ignored) {}
                    final String finalErr = errBody;
                    android.util.Log.e("VNPAY", "Error response: " + resp.code() + " body: " + finalErr);
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(this, "Không thể tạo URL thanh toán (HTTP " + resp.code() + "): " + finalErr, Toast.LENGTH_LONG).show();
                        findViewById(R.id.btnPlaceOrder).setEnabled(true);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("VNPAY", "Exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, "Lỗi kết nối VNPAY: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    findViewById(R.id.btnPlaceOrder).setEnabled(true);
                });
            }
        }).start();
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

    private Map<String, Object> buildOrderPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("CustomerID", customerId);
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
        payload.put("items", buildOrderItemPayloads());
        payload.put("CarbonPointEarned", Math.round(currentCarbonPoints));
        payload.put("TotalCarbonEmission", currentCarbonEmission);
        if ("veggopay".equals(selectedPaymentMethod)) {
            payload.put("walletPassword", currentWalletPassword);
        }
        return payload;
    }

    private Map<String, Object> buildShippingInfoPayload() {
        Map<String, Object> shippingInfo = new HashMap<>();
        Map<String, Object> address = new HashMap<>();

        String receiverName = tvAddressName != null ? safeText(tvAddressName.getText().toString()) : "";
        String phone = tvAddressPhone != null ? safeText(tvAddressPhone.getText().toString()) : "";
        String fullAddress = tvAddressDetail != null ? safeText(tvAddressDetail.getText().toString()) : "";
        String[] addressParts = fullAddress.split("\\s*,\\s*");
        String detail = addressParts.length > 0 ? addressParts[0] : fullAddress;
        String ward = addressParts.length > 1 ? addressParts[1] : "";
        String district = addressParts.length > 2 ? addressParts[2] : "";
        String city = addressParts.length > 3 ? addressParts[3] : "";

        address.put("detail", detail);
        address.put("ward", ward);
        address.put("district", district);
        address.put("city", city);

        shippingInfo.put("fullName", receiverName);
        shippingInfo.put("phone", phone);
        shippingInfo.put("email", safeText(new AppPreferences(this).getEmail()));
        shippingInfo.put("address", address);
        shippingInfo.put("deliveryMethod", isFastDeliveryMode ? "fast" : "scheduled");
        DeliveryTimeUtils.DeliveryWindow activeWindow = getActiveDeliveryWindow();
        if (activeWindow != null) {
            shippingInfo.put("deliveryWindowStart", formatDeliveryTimestamp(activeWindow.start));
            shippingInfo.put("deliveryWindowEnd", formatDeliveryTimestamp(activeWindow.end));
            shippingInfo.put("deliveryTimeText", activeWindow.displayText());
        }
        shippingInfo.put("notes", tvCheckoutNote != null ? safeText(tvCheckoutNote.getText().toString()) : "");
        shippingInfo.put("warehouse_id", safeText(warehouseHelper.getSelectedWarehouseCode()));
        if (recurringCheckoutMode) {
            shippingInfo.put("isRecurring", true);
            shippingInfo.put("orderSource", "recurring");
            shippingInfo.put("recurringOrderId", safeText(recurringOrderId));
            shippingInfo.put("occurrenceDate", safeText(recurringOccurrenceDate));
            RecurringOrderStore.RecurringOrder recurringOrder =
                    new RecurringOrderStore(this).findById(recurringOrderId);
            if (recurringOrder != null) {
                shippingInfo.put("recurringOrderName", safeText(recurringOrder.name));
                shippingInfo.put("recurringFrequency", safeText(recurringOrder.frequency));
                shippingInfo.put("deliverySlot", safeText(recurringOrder.deliverySlot));
            }
        } else {
            shippingInfo.put("isRecurring", false);
            shippingInfo.put("orderSource", isFastDeliveryMode ? "fast" : "standard");
        }
        return shippingInfo;
    }

    private List<Map<String, Object>> buildOrderItemPayloads() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartDto.CartItemDto itemDto : cartItems) {
            ProductDto product = itemDto.getProduct();
            boolean hasWeightOptions = product != null
                    && product.getWeightOptions() != null
                    && !product.getWeightOptions().isEmpty();
            double selectedWeight = itemDto.getSelectedWeight() > 0 ? itemDto.getSelectedWeight() : 1.0;
            long unitPrice = product != null ? product.getPrice() : 0;
            long originalPrice = product != null ? product.getOriginalPrice() : 0;
            if (unitPrice <= 0) {
                unitPrice = itemDto.getPrice();
            }
            if (originalPrice <= 0) {
                originalPrice = itemDto.getOriginalPrice();
            }
            if (originalPrice < unitPrice) {
                originalPrice = unitPrice;
            }
            double carbonEmission = calculateCarbonEmission(product, itemDto.getQuantity(), selectedWeight, hasWeightOptions);
            long carbonPoints = Math.round(carbonEmission * 10);

            Map<String, Object> item = new HashMap<>();
            item.put("sku", itemDto.getSku());
            item.put("productName", product != null ? safeText(product.getProductName()) : "Sản phẩm VEGGO");
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

    private void bindCart(CartDto cartDto) {
        previewItems.clear();
        cartItems.clear();
        long subtotal = 0;
        long productDiscount = 0;
        double carbonPoints = 0;
        double carbonEmission = 0;
        currentProductCount = 0;

        if (cartDto != null && cartDto.getItems() != null) {
            for (CartDto.CartItemDto itemDto : cartDto.getItems()) {
                ProductDto product = itemDto.getProduct();
                double selectedWeight = itemDto.getSelectedWeight() > 0 ? itemDto.getSelectedWeight() : 1.0;
                if (selectedCartLineKeys != null
                        && !selectedCartLineKeys.contains(cartLineKey(itemDto.getSku(), selectedWeight))) {
                    continue;
                }

                cartItems.add(itemDto);
                currentProductCount += itemDto.getQuantity();
                String name = product != null && product.getProductName() != null && !product.getProductName().isEmpty()
                        ? product.getProductName()
                        : "Sản phẩm VEGGO";
                boolean hasWeightOptions = product != null
                        && product.getWeightOptions() != null
                        && !product.getWeightOptions().isEmpty();
                long unitPrice = product != null ? product.getPrice() : 0;
                long originalPrice = product != null ? product.getOriginalPrice() : 0;
                if (unitPrice <= 0) {
                    unitPrice = itemDto.getPrice();
                }
                if (originalPrice <= 0) {
                    originalPrice = itemDto.getOriginalPrice();
                }
                if (originalPrice < unitPrice) {
                    originalPrice = unitPrice;
                }
                long variantUnitPrice = variantPrice(unitPrice, selectedWeight, hasWeightOptions);
                long variantOriginalPrice = variantPrice(originalPrice, selectedWeight, hasWeightOptions);

                subtotal += variantUnitPrice * itemDto.getQuantity();
                productDiscount += Math.max(0, variantOriginalPrice - variantUnitPrice) * itemDto.getQuantity();
                if (product != null) {
                    double itemCarbonEmission = calculateCarbonEmission(product, itemDto.getQuantity(), selectedWeight, hasWeightOptions);
                    carbonEmission += itemCarbonEmission;
                    carbonPoints += itemCarbonEmission * 10;
                }
                previewItems.add(new PaymentItemAdapter.PaymentItemUiModel(
                        name,
                        resolveItemUnit(product, selectedWeight, hasWeightOptions),
                        variantUnitPrice,
                        itemDto.getQuantity(),
                        R.drawable.ic_vegetable,
                        ProductCatalogImageResolver.resolveCheckoutImage(this, product, itemDto.getSku())
                ));
            }
        }

        paymentItemAdapter.setItems(getVisiblePreviewItems());
        updateToggleProductsUi();
        currentSubtotal = subtotal;
        currentProductDiscount = productDiscount;
        currentCarbonPoints = carbonPoints;
        currentCarbonEmission = carbonEmission;
        validateAppliedVoucher();
        bindPaymentSummary();
    }

    private void bindPaymentSummary() {
        long voucherDiscount = 0;
        long shippingFee = currentSubtotal > 0 ? DEFAULT_SHIPPING_FEE : 0;
        long shippingDiscount = currentSubtotal >= FREE_SHIPPING_THRESHOLD ? shippingFee : 0;
        if (selectedProductVoucher != null && selectedProductVoucher.promotion != null) {
            voucherDiscount = Math.min(currentSubtotal,
                    calculatePromotionDiscount(selectedProductVoucher.promotion, currentSubtotal));
        }
        if (selectedShippingVoucher != null && selectedShippingVoucher.promotion != null) {
            shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount,
                    calculatePromotionDiscount(selectedShippingVoucher.promotion, shippingFee)));
        }
        long total = Math.max(
                0,
                currentSubtotal - currentProductDiscount + shippingFee - voucherDiscount - shippingDiscount
        );
        currentPaymentTotal = total;
        currentVoucherDiscount = voucherDiscount;
        currentShippingFee = shippingFee;
        currentShippingDiscount = shippingDiscount;

        tvCheckoutSubtotal.setText(formatCurrency(currentSubtotal));
        tvCheckoutVoucherDiscount.setText(formatDiscount(voucherDiscount));
        tvCheckoutProductDiscount.setText(formatDiscount(currentProductDiscount));
        tvCheckoutShippingFee.setText(formatCurrency(shippingFee));
        tvCheckoutShippingDiscount.setText(formatDiscount(shippingDiscount));
        tvCheckoutPaymentTotal.setText(formatCurrency(total));
        tvCheckoutBottomTotal.setText(formatCurrency(total));
        setCarbonPointsText(currentCarbonPoints);
        updateVeggoPayPaymentRowUi();
    }

    private boolean canPayWithVeggoPay() {
        return veggoPayBalance >= currentPaymentTotal;
    }

    private void updateVeggoPayPaymentRowUi() {
        if (radioPaymentMomo == null || btnVeggoPayTopUp == null) {
            return;
        }
        boolean enough = canPayWithVeggoPay();
        radioPaymentMomo.setVisibility(enough ? View.VISIBLE : View.GONE);
        btnVeggoPayTopUp.setVisibility(enough ? View.GONE : View.VISIBLE);

        if (!enough && "veggopay".equals(selectedPaymentMethod)) {
            selectPaymentMethod("cod");
        }

        if (pendingSelectVeggoPayAfterTopUp && enough) {
            pendingSelectVeggoPayAfterTopUp = false;
            selectPaymentMethod("veggopay");
        }
    }

    private void openVeggoPayTopUp() {
        pendingSelectVeggoPayAfterTopUp = true;
        showDepositDialogInCheckout();
    }

    private void showDepositDialogInCheckout() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wallet_deposit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText edtAmount = dialog.findViewById(R.id.edtAmount);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        if (veggoPayLinkedBanks == null || veggoPayLinkedBanks.isEmpty()) {
            Toast.makeText(this, "Vui lòng liên kết tài khoản ngân hàng trước khi nạp tiền!", Toast.LENGTH_LONG).show();
            return;
        }

        final WalletDto.LinkedBankDto[] selectedBankArr = new WalletDto.LinkedBankDto[1];
        for (WalletDto.LinkedBankDto b : veggoPayLinkedBanks) {
            if (b.isDefault()) {
                selectedBankArr[0] = b;
                break;
            }
        }
        if (selectedBankArr[0] == null) {
            selectedBankArr[0] = veggoPayLinkedBanks.get(0);
        }

        View layoutSelectBank = dialog.findViewById(R.id.layoutSelectBank);
        ImageView imgSelectedBankLogo = dialog.findViewById(R.id.imgSelectedBankLogo);
        TextView tvSelectedBankInfo = dialog.findViewById(R.id.tvSelectedBankInfo);

        autoUpdateSelectedBankDisplay(selectedBankArr[0], imgSelectedBankLogo, tvSelectedBankInfo);

        if (layoutSelectBank != null) {
            layoutSelectBank.setOnClickListener(v -> {
                String[] bankOptions = new String[veggoPayLinkedBanks.size()];
                for (int i = 0; i < veggoPayLinkedBanks.size(); i++) {
                    WalletDto.LinkedBankDto bankItem = veggoPayLinkedBanks.get(i);
                    bankOptions[i] = bankItem.getBankCode() + " - " + bankItem.getAccountNumber() + (bankItem.isDefault() ? " (Mặc định)" : "");
                }

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Chọn tài khoản ngân hàng nguồn")
                        .setItems(bankOptions, (dialogInterface, index) -> {
                            selectedBankArr[0] = veggoPayLinkedBanks.get(index);
                            autoUpdateSelectedBankDisplay(selectedBankArr[0], imgSelectedBankLogo, tvSelectedBankInfo);
                        })
                        .show();
            });
        }

        if (edtAmount != null) {
            edtAmount.addTextChangedListener(new TextWatcher() {
                private String current = "";
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (!s.toString().equals(current)) {
                        edtAmount.removeTextChangedListener(this);

                        String cleanString = s.toString().replaceAll("[.,]", "");
                        if (!cleanString.isEmpty()) {
                            try {
                                double parsed = Double.parseDouble(cleanString);
                                java.text.DecimalFormat formatter = (java.text.DecimalFormat) java.text.NumberFormat.getInstance(java.util.Locale.US);
                                formatter.applyPattern("#,###");
                                String formatted = formatter.format(parsed).replace(",", ".");
                                current = formatted;
                                edtAmount.setText(formatted);
                                edtAmount.setSelection(formatted.length());
                            } catch (NumberFormatException ignored) {}
                        } else {
                            current = "";
                            edtAmount.setText("");
                        }

                        edtAmount.addTextChangedListener(this);
                    }
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String rawAmount = edtAmount == null ? "" : edtAmount.getText().toString().trim().replaceAll("[.,]", "");
                if (rawAmount.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(rawAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (amount < 10000) {
                    Toast.makeText(this, "Số tiền nạp tối thiểu là 10,000 ₫", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                showDepositPasswordConfirmDialogInCheckout(amount, selectedBankArr[0].getBankCode());
            });
        }

        dialog.show();
    }

    private void autoUpdateSelectedBankDisplay(WalletDto.LinkedBankDto bank, ImageView imgLogo, TextView tvInfo) {
        if (bank == null || imgLogo == null || tvInfo == null) return;
        tvInfo.setText(bank.getBankCode() + " - " + bank.getAccountNumber());
        int logoRes = R.drawable.ic_card;
        String codeLower = bank.getBankCode().toLowerCase();
        if (codeLower.contains("vietcombank") || codeLower.equals("vcb")) {
            logoRes = R.drawable.logo_vcb;
        } else if (codeLower.contains("techcombank") || codeLower.equals("tcb")) {
            logoRes = R.drawable.logo_tcb;
        } else if (codeLower.contains("mb") || codeLower.contains("mbbank")) {
            logoRes = R.drawable.logo_mb;
        } else if (codeLower.contains("bidv")) {
            logoRes = R.drawable.logo_bidv;
        } else if (codeLower.contains("vietin") || codeLower.equals("ctg")) {
            logoRes = R.drawable.logo_ctg;
        }
        imgLogo.setImageResource(logoRes);
    }

    private void showDepositPasswordConfirmDialogInCheckout(final double amount, final String bankCode) {
        Dialog pinDialog = new Dialog(this);
        pinDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pinDialog.setContentView(R.layout.dialog_veggopay_password_input);
        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            pinDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = pinDialog.findViewById(R.id.tvPinTitle);
        TextView tvDesc = pinDialog.findViewById(R.id.tvPinDescription);
        TextView btnCancel = pinDialog.findViewById(R.id.btnCancel);
        TextView btnConfirm = pinDialog.findViewById(R.id.btnConfirm);

        if (tvTitle != null) tvTitle.setText("Xác thực giao dịch");
        if (tvDesc != null) tvDesc.setText("Nhập mật khẩu ví 6 số để xác nhận nạp " + com.veggo.app.core.utils.CurrencyFormatter.formatVnd((long) amount));

        EditText[] pinFields = new EditText[] {
                pinDialog.findViewById(R.id.edtPin1),
                pinDialog.findViewById(R.id.edtPin2),
                pinDialog.findViewById(R.id.edtPin3),
                pinDialog.findViewById(R.id.edtPin4),
                pinDialog.findViewById(R.id.edtPin5),
                pinDialog.findViewById(R.id.edtPin6)
        };

        for (int i = 0; i < pinFields.length; i++) {
            final int index = i;
            if (pinFields[i] == null) continue;
            pinFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < pinFields.length - 1 && pinFields[index + 1] != null) {
                            pinFields[index + 1].requestFocus();
                        } else if (btnConfirm != null) {
                            btnConfirm.performClick();
                        }
                    }
                }
            });
            pinFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (pinFields[index].getText().length() == 0 && index > 0 && pinFields[index - 1] != null) {
                        pinFields[index - 1].requestFocus();
                        pinFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }

        if (pinFields[0] != null) pinFields[0].requestFocus();
        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> pinDialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                StringBuilder pinBuilder = new StringBuilder();
                for (EditText edt : pinFields) {
                    if (edt != null) pinBuilder.append(edt.getText().toString().trim());
                }
                String pinCode = pinBuilder.toString();
                if (pinCode.length() < 6) {
                    Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số mật khẩu", Toast.LENGTH_SHORT).show();
                    return;
                }

                pinDialog.dismiss();
                showProgress("Đang xử lý nạp tiền...");
                walletRepository.deposit(customerId, amount, bankCode, pinCode, new WalletRepository.ResultCallback<WalletDto>() {
                    @Override
                    public void onSuccess(WalletDto result) {
                        runOnUiThread(() -> {
                            hideProgress();
                            Toast.makeText(CheckoutActivity.this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                            loadWalletInfo();
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        runOnUiThread(() -> {
                            hideProgress();
                            Toast.makeText(CheckoutActivity.this, "Nạp tiền thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
        }

        pinDialog.show();
    }

    private List<PaymentItemAdapter.PaymentItemUiModel> getVisiblePreviewItems() {
        return new ArrayList<>(previewItems);
    }

    private void updateToggleProductsUi() {
        if (layoutToggleProducts == null) {
            return;
        }
        layoutToggleProducts.setVisibility(View.GONE);
    }

    private void showPopup(int layoutResId) {
        if (layoutResId == R.layout.dialog_voucher) {
            showVoucherDialog();
            return;
        }
        if (layoutResId == R.layout.dialog_delivery_origin) {
            warehouseHelper.showSelectionDialog(this, this::bindSelectedWarehouseUi, null);
            return;
        }
        if (layoutResId == R.layout.dialog_location) {
            showLocationDialog();
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
                if (customerId == null) {
                    customerId = resolveCustomerId();
                }
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
            VoucherOptionAdapter.VoucherItemUiModel resolved = findVoucherItem(selectedProductVoucher.promotionId, false);
            selectedProductVoucher = resolved;
        }
        if (selectedShippingVoucher != null) {
            VoucherOptionAdapter.VoucherItemUiModel resolved = findVoucherItem(selectedShippingVoucher.promotionId, true);
            selectedShippingVoucher = resolved;
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

        // Phản hồi tức thì
        recyclerView.setVisibility(View.GONE);
        tvListTitle.setVisibility(View.VISIBLE);
        tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Đang kiểm tra mã '" + code + "'...");
        selectedCountView.setText("Đang kiểm tra mã...");

        promotionApi.getPromotions(customerId, code, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PromotionDto> foundPromotions = response.body();
                    runOnUiThread(() -> {
                        if (foundPromotions.isEmpty()) {
                            showVoucherEmptyState("Không tìm thấy mã khuyến mãi '" + code + "'.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            tvListTitle.setVisibility(View.VISIBLE);
                            tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
                            recyclerView.setVisibility(View.VISIBLE);
                            renderVoucherOptions(foundPromotions, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showVoucherEmptyState("Mã '" + code + "' không hợp lệ hoặc đã hết hạn.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
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

        promotionApi.getPromotions(customerId, null, null).enqueue(new Callback<List<PromotionDto>>() {
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

    private boolean validateSelectedVouchersBeforeCheckout() {
        String invalidReason = validateVoucherSelection(selectedProductVoucher);
        if (invalidReason != null) {
            Toast.makeText(this, "Voucher sản phẩm không còn hợp lệ: " + invalidReason, Toast.LENGTH_LONG).show();
            selectedProductVoucher = null;
            updateSelectedVoucherUi();
            bindPaymentSummary();
            return false;
        }
        invalidReason = validateVoucherSelection(selectedShippingVoucher);
        if (invalidReason != null) {
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

    private void showLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvLocationOptions);
        TextView btnCancelLocation = dialogView.findViewById(R.id.btnCancelLocation);
        TextView btnConfirmLocation = dialogView.findViewById(R.id.btnConfirmLocation);
        TextView btnAddLocation = dialogView.findViewById(R.id.btnAddLocation);

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ensureLocationItems();
        recyclerView.setAdapter(new LocationOptionAdapter(
                locationItems,
                selectedLocationIndex,
                item -> selectedLocationIndex = locationItems.indexOf(item),
                item -> {
                    dialog.dismiss();
                    openEditAddressFromLocationItem(item);
                }
        ));
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

    private void openEditAddressFromLocationItem(@NonNull LocationOptionAdapter.LocationItemUiModel item) {
        if (customerId == null || customerId.trim().isEmpty() || addressRepository == null) {
            openAddAddressForm();
            return;
        }
        addressRepository.getAddresses(customerId, new AddressRepository.Callback<List<Address>>() {
            @Override
            public void onSuccess(List<Address> addresses) {
                if (addresses == null) {
                    openAddAddressForm();
                    return;
                }
                for (Address address : addresses) {
                    String displayName = joinNameAndPhone(address.getName(), address.getPhone());
                    String displayAddress = joinAddress(address.getDetail(), address.getWard(), address.getDistrict(), address.getProvince());
                    if (displayName.equals(item.name) && displayAddress.equals(item.address)) {
                        openEditAddressForm(address);
                        return;
                    }
                }
                openAddAddressForm();
            }

            @Override
            public void onError(Throwable t) {
                openAddAddressForm();
            }
        });
    }

    private void openEditAddressForm(@NonNull Address address) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        intent.putExtra(AddressFormActivity.EXTRA_ADDRESS_ID, address.getId());
        intent.putExtra(AddressFormActivity.EXTRA_NAME, address.getName());
        intent.putExtra(AddressFormActivity.EXTRA_PHONE, address.getPhone());
        intent.putExtra(AddressFormActivity.EXTRA_EMAIL, address.getEmail());
        intent.putExtra(AddressFormActivity.EXTRA_DETAIL, address.getDetail());
        intent.putExtra(AddressFormActivity.EXTRA_WARD, address.getWard());
        intent.putExtra(AddressFormActivity.EXTRA_DISTRICT, address.getDistrict());
        intent.putExtra(AddressFormActivity.EXTRA_CITY, address.getProvince());
        intent.putExtra(AddressFormActivity.EXTRA_IS_DEFAULT, address.isDefault());
        intent.putExtra(AddressFormActivity.EXTRA_EDIT_MODE, true);
        addAddressLauncher.launch(intent);
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

    private BottomSheetDialog createHalfHeightBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                int halfScreenHeight = getResources().getDisplayMetrics().heightPixels / 2;
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                bottomSheet.setPadding(0, 0, 0, 0);
                ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (view, windowInsets) -> {
                    int bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = halfScreenHeight + bottomInset;
                    view.setLayoutParams(layoutParams);
                    view.setPadding(0, 0, 0, bottomInset);
                    return windowInsets;
                });
                ViewCompat.requestApplyInsets(bottomSheet);
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

    private List<VoucherOptionAdapter.VoucherItemUiModel> buildVoucherItems() {
        return new ArrayList<>();
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

    @NonNull
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
        if (customerId == null) {
            customerId = resolveCustomerId();
        }
        return PromotionVoucherHelper.evaluateDisabledReason(
                promotion,
                target,
                usage,
                currentSubtotal,
                customerId,
                matchesTarget(target)
        );
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
        if (promotion == null || !Boolean.TRUE.equals(promotion.getActive())) {
            return false;
        }
        return !PromotionVoucherHelper.isPromotionExpired(promotion)
                && !PromotionVoucherHelper.isPromotionNotStarted(promotion);
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

    private void ensureLocationItems() {
        // Allow opening address book even when the list is empty.
    }

    private void loadAddresses() {
        if (customerId == null || customerId.trim().isEmpty() || addressRepository == null) {
            locationItems.clear();
            selectedLocationIndex = 0;
            updateAddressDisplayMode();
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
                applyRecurringAddressIfNeeded();
                updateAddressDisplayMode();
            }

            @Override
            public void onError(Throwable t) {
                if (locationItems.isEmpty()) {
                    selectedLocationIndex = 0;
                }
                applyRecurringAddressIfNeeded();
                updateAddressDisplayMode();
            }
        });
    }

    private void applyRecurringAddressIfNeeded() {
        if (!recurringCheckoutMode || recurringCheckoutContext == null) {
            return;
        }
        String displayName = joinNameAndPhone(
                recurringCheckoutContext.receiverName,
                recurringCheckoutContext.receiverPhone
        );
        String displayAddress = recurringCheckoutContext.fullAddressLine();
        int matchedIndex = -1;
        for (int i = 0; i < locationItems.size(); i++) {
            LocationOptionAdapter.LocationItemUiModel item = locationItems.get(i);
            if (displayAddress.equals(item.address)) {
                matchedIndex = i;
                break;
            }
        }
        if (matchedIndex >= 0) {
            selectedLocationIndex = matchedIndex;
            return;
        }
        locationItems.add(0, new LocationOptionAdapter.LocationItemUiModel(
                displayName,
                displayAddress,
                true
        ));
        selectedLocationIndex = 0;
    }

    private void updateAddressDisplayMode() {
        boolean hasSavedAddress = !locationItems.isEmpty();
        if (layoutAddressSaved != null) {
            layoutAddressSaved.setVisibility(hasSavedAddress ? View.VISIBLE : View.GONE);
        }
        if (layoutAddressEmpty != null) {
            layoutAddressEmpty.setVisibility(hasSavedAddress ? View.GONE : View.VISIBLE);
        }
        if (hasSavedAddress) {
            applySelectedLocationToAddressCard();
        }
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
        boolean isDefault = data.getBooleanExtra(AddressFormActivity.EXTRA_IS_DEFAULT, false);
        String displayName = joinNameAndPhone(name, phone);
        String displayAddress = joinAddress(detail, ward, district, city);

        if (isDefault) {
            for (int i = 0; i < locationItems.size(); i++) {
                LocationOptionAdapter.LocationItemUiModel item = locationItems.get(i);
                locationItems.set(i, new LocationOptionAdapter.LocationItemUiModel(
                        item.name,
                        item.address,
                        false
                ));
            }
        }

        for (int i = locationItems.size() - 1; i >= 0; i--) {
            LocationOptionAdapter.LocationItemUiModel item = locationItems.get(i);
            if (displayName.equals(item.name) && displayAddress.equals(item.address)) {
                locationItems.remove(i);
            }
        }

        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(displayName, displayAddress, isDefault));
        selectedLocationIndex = locationItems.size() - 1;
        updateAddressDisplayMode();
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

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(this);
        String resolvedCustomerId = appPreferences.getCustomerId();
        if (resolvedCustomerId == null || resolvedCustomerId.trim().isEmpty()) {
            resolvedCustomerId = new PreferencesManager(this).getUserId();
        }
        return resolvedCustomerId;
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
        double amount;
        try {
            amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException exception) {
            return 0;
        }
        String unit = matcher.group(2);
        return unit.startsWith("g") ? amount / 1000.0 : amount;
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

    private String cartLineKey(String sku, double selectedWeight) {
        return safeText(sku) + "#" + trimTrailingZeros(selectedWeight > 0 ? selectedWeight : 1.0);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveItemUnit(ProductDto product, double selectedWeight, boolean hasWeightOptions) {
        if (hasWeightOptions) {
            return formatWeight(selectedWeight);
        }
        if (product == null) {
            return "";
        }
        String weight = safeText(product.getWeight());
        if (!weight.isEmpty()) {
            return weight;
        }
        return safeText(product.getUnit());
    }

    private String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }

    private android.app.Dialog progressDialog;
    private android.widget.TextView progressTextView;

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            
            // Full screen layout with translucent black background
            android.widget.RelativeLayout rootLayout = new android.widget.RelativeLayout(this);
            rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#99000000")); // ~60% black opacity overlay
            
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setGravity(android.view.Gravity.CENTER);
            
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
            
            progressTextView = new android.widget.TextView(this);
            progressTextView.setText(message);
            progressTextView.setTextColor(android.graphics.Color.WHITE);
            progressTextView.setTextSize(16);
            progressTextView.setGravity(android.view.Gravity.CENTER);
            progressTextView.setPadding(30, 32, 30, 0); // padding horizontal and space below spinner
            
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

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
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
                Toast.makeText(this, "Thanh toán VNPay thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnPlaceOrder).setEnabled(true);
            }
        }
    }

    private void createOrderAfterVnpaySuccess(String orderId) {
        java.util.Map<String, Object> payload = buildOrderPayload();
        payload.put("orderId", orderId);
        payload.put("paymentStatus", "paid");
        
        OrderApi orderApi = com.veggo.app.core.network.ApiClient.createService(OrderApi.class);
        orderApi.createOrder(payload).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                hideProgress();
                String finalId = orderId;
                if (response.isSuccessful() && response.body() != null) {
                    finalId = response.body().getOrderId() != null ? response.body().getOrderId() : response.body().getId();
                }
                openPaymentStep(finalId);
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                hideProgress();
                Toast.makeText(CheckoutActivity.this, "Đã thanh toán nhưng lỗi tạo đơn: " + t.getMessage(), Toast.LENGTH_LONG).show();
                openPaymentStep(orderId);
            }
        });
    }

    private String formatDiscount(long amount) {
        return amount > 0 ? "-" + formatCurrency(amount) : "0đ";
    }

    private String formatCarbonPoints(double points) {
        if (Math.abs(points - Math.round(points)) < 0.0001) {
            return String.format(Locale.US, "%.0f", points);
        }
        return String.format(Locale.US, "%.1f", points);
    }

    private void setCarbonPointsText(double points) {
        String pointsText = formatCarbonPoints(points);
        String text = "Nhận " + pointsText + " điểm carbon";
        SpannableString spannable = new SpannableString(text);
        int start = text.indexOf(pointsText);
        int end = start + pointsText.length();
        if (start >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_main)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        tvCheckoutCarbonPoints.setText(spannable);
    }

    private String buildPaymentCode() {
        long now = System.currentTimeMillis() % 1_000_000L;
        String suffix = customerId == null ? "GUEST" : customerId.replaceAll("[^A-Za-z0-9]", "");
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return String.format(Locale.US, "VG%s%06d", suffix, now);
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
        warehouseHelper.updateDeliveryAddress(selectedItem.address, this::bindSelectedWarehouseUi);
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

    private String[] splitNameAndPhone(String value) {
        String safeValue = value == null ? "" : value;
        String[] parts = safeValue.split(" - ", 2);
        if (parts.length == 2) {
            return parts;
        }
        return new String[]{safeValue, ""};
    }
}
