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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.LocationOptionAdapter;
import com.veggo.app.adapter.PaymentItemAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
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
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;
import com.veggo.app.presentation.cart.CartViewModel;
import com.veggo.app.presentation.profile.AddressFormActivity;

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

public class CheckoutActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_VOUCHER_ID = "extra_selected_voucher_id";
    public static final String EXTRA_SELECTED_VOUCHER_TITLE = "extra_selected_voucher_title";
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

    private static final long FREE_SHIPPING_THRESHOLD = 300_000L;
    private static final long DEFAULT_SHIPPING_FEE = 30_000L;

    private TextView tvCheckoutNote;
    private TextView tvScheduleDeliveryTime;
    private TextView tvAddressName;
    private TextView tvAddressPhone;
    private TextView tvAddressDetail;
    private TextView tvAddressDefaultBadge;
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
    private String customerId;
    private AppCompatRadioButton radioPaymentMomo;
    private AppCompatRadioButton radioPaymentBank;
    private AppCompatRadioButton radioPaymentCod;
    private AppCompatRadioButton radioPaymentVnpay;
    private String selectedPaymentMethod = "vnpay";
    private long currentPaymentTotal;
    private long currentSubtotal;
    private long currentProductDiscount;
    private long currentVoucherDiscount;
    private long currentShippingFee;
    private long currentShippingDiscount;
    private double currentCarbonPoints;
    private double currentCarbonEmission;
    private int currentProductCount;
    private VoucherOptionAdapter.VoucherItemUiModel selectedVoucher;
    private final List<PaymentItemAdapter.PaymentItemUiModel> previewItems = new ArrayList<>();
    private final List<CartDto.CartItemDto> cartItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> voucherItems = new ArrayList<>();
    private final List<LocationOptionAdapter.LocationItemUiModel> locationItems = new ArrayList<>();
    private Set<String> selectedCartLineKeys;
    private int selectedLocationIndex;
    private boolean buyNowMode;
    private boolean transferredCheckoutMode;
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
                applySelectedLocationToAddressCard();
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
        tvAddressName = findViewById(R.id.tvAddressName);
        tvAddressPhone = findViewById(R.id.tvAddressPhone);
        tvAddressDetail = findViewById(R.id.tvAddressDetail);
        tvAddressDefaultBadge = findViewById(R.id.tvAddressDefaultBadge);
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
        tvCheckoutNote.setText("");
        tvCheckoutNote.setHint("Nhập ghi chú");
        buyNowMode = getIntent().getBooleanExtra(EXTRA_BUY_NOW, false);
        transferredCheckoutMode = getIntent().getBooleanExtra(EXTRA_TRANSFERRED_CHECKOUT, false);
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
        layoutAddress.setOnClickListener(v -> showPopup(R.layout.dialog_location));
        layoutDeliveryOrigin.setOnClickListener(v -> showPopup(R.layout.dialog_delivery_origin));
        layoutVoucher.setOnClickListener(v -> showPopup(R.layout.dialog_voucher));
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

        cartViewModel.getCartData().observe(this, this::bindCart);
        cartViewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProducts(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        paymentItemAdapter = new PaymentItemAdapter(new ArrayList<>());
        recyclerView.setAdapter(paymentItemAdapter);
    }

    private void restoreSelectedVoucherFromIntent() {
        String promotionId = getIntent().getStringExtra(EXTRA_SELECTED_VOUCHER_ID);
        String title = getIntent().getStringExtra(EXTRA_SELECTED_VOUCHER_TITLE);
        if (title != null && !title.trim().isEmpty()) {
            selectedVoucher = new VoucherOptionAdapter.VoucherItemUiModel(
                    title,
                    "",
                    "",
                    R.drawable.ic_voucher,
                    promotionId,
                    true,
                    null
            );
        }
        updateSelectedVoucherUi();
    }

    private void setupPaymentMethods() {
        selectPaymentMethod("vnpay");
        radioPaymentMomo.setOnClickListener(v -> selectPaymentMethod("momo"));
        radioPaymentBank.setOnClickListener(v -> selectPaymentMethod("bank"));
        radioPaymentCod.setOnClickListener(v -> selectPaymentMethod("cod"));
        if (radioPaymentVnpay != null) radioPaymentVnpay.setOnClickListener(v -> selectPaymentMethod("vnpay"));
        View layoutPaymentMomo = findViewById(R.id.layoutPaymentMomo);
        View layoutPaymentCod = findViewById(R.id.layoutPaymentCod);
        View layoutPaymentBank = findViewById(R.id.layoutPaymentBank);
        View layoutPaymentVnpay = findViewById(R.id.layoutPaymentVnpay);
        if (layoutPaymentMomo != null) {
            layoutPaymentMomo.setOnClickListener(v -> selectPaymentMethod("momo"));
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
        if (radioPaymentMomo != null) radioPaymentMomo.setChecked("momo".equals(method));
        if (radioPaymentBank != null) radioPaymentBank.setChecked("bank".equals(method));
        if (radioPaymentCod  != null) radioPaymentCod.setChecked("cod".equals(method));
        if (radioPaymentVnpay != null) radioPaymentVnpay.setChecked("vnpay".equals(method));
    }

    private void createOrderAndOpenPaymentStep() {
        if ("momo".equals(selectedPaymentMethod) || "bank".equals(selectedPaymentMethod)) {
            Toast.makeText(this, "Hệ thống đang phát triển", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPlaceOrder).setEnabled(true);
            return;
        }
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("vnpay".equals(selectedPaymentMethod)) {
            String tempOrderId = "VG" + System.currentTimeMillis();
            openVnpayPayment(tempOrderId);
        } else {
            showProgress("Đang gửi yêu cầu đặt đơn hàng...");
            OrderApi orderApi = com.veggo.app.core.network.ApiClient.createService(OrderApi.class);
            orderApi.createOrder(buildOrderPayload()).enqueue(new Callback<OrderDto>() {
                @Override
                public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                    hideProgress();
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(CheckoutActivity.this, "Không thể tạo đơn hàng", Toast.LENGTH_SHORT).show();
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
        intent.putExtra(QrPaymentActivity.EXTRA_PAYMENT_CODE, buildPaymentCode());
        intent.putExtra(QrPaymentActivity.EXTRA_ORDER_ID, orderId);
        intent.putExtra(QrPaymentActivity.EXTRA_SHOW_SUCCESS_IMMEDIATELY, "cod".equals(selectedPaymentMethod) || "vnpay".equals(selectedPaymentMethod));
        intent.putExtra(QrPaymentActivity.EXTRA_CLEAR_CART_ON_SUCCESS, !buyNowMode && !transferredCheckoutMode);
        intent.putExtra(QrPaymentActivity.EXTRA_CUSTOMER_ID, customerId);
        if (!buyNowMode && !transferredCheckoutMode) {
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
                        intent.putExtra(VnpayWebViewActivity.EXTRA_CLEAR_CART, !buyNowMode && !transferredCheckoutMode);
                        if (!buyNowMode && !transferredCheckoutMode) {
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
        payload.put("code", selectedVoucher != null ? safeText(selectedVoucher.promotionId) : "");
        payload.put("promotion_id", selectedVoucher != null ? safeText(selectedVoucher.promotionId) : null);
        payload.put("promotionName", selectedVoucher != null ? safeText(selectedVoucher.title) : "");
        payload.put("wantInvoice", false);
        payload.put("invoiceInfo", new HashMap<String, Object>());
        payload.put("consultantCode", "");
        payload.put("shippingInfo", buildShippingInfoPayload());
        payload.put("items", buildOrderItemPayloads());
        payload.put("CarbonPointEarned", Math.round(currentCarbonPoints));
        payload.put("TotalCarbonEmission", currentCarbonEmission);
        return payload;
    }

    private Map<String, Object> buildShippingInfoPayload() {
        Map<String, Object> shippingInfo = new HashMap<>();
        Map<String, Object> address = new HashMap<>();

        String receiverName = tvAddressName != null ? safeText(tvAddressName.getText().toString()) : "";
        String phone = tvAddressPhone != null ? safeText(tvAddressPhone.getText().toString()) : "";
        String fullAddress = tvAddressDetail != null ? safeText(tvAddressDetail.getText().toString()) : "";
        String[] addressParts = fullAddress.split("\\s*,\\s*");
        address.put("detail", addressParts.length > 0 ? addressParts[0] : fullAddress);
        address.put("ward", addressParts.length > 1 ? addressParts[1] : "");
        address.put("district", addressParts.length > 2 ? addressParts[2] : "");
        address.put("city", addressParts.length > 3 ? addressParts[3] : "");

        shippingInfo.put("fullName", receiverName);
        shippingInfo.put("phone", phone);
        shippingInfo.put("email", safeText(new AppPreferences(this).getEmail()));
        shippingInfo.put("address", address);
        shippingInfo.put("deliveryMethod", "standard");
        shippingInfo.put("notes", tvCheckoutNote != null ? safeText(tvCheckoutNote.getText().toString()) : "");
        shippingInfo.put("warehouse_id", "WH001");
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
                        product != null ? product.getFirstImage() : null
                ));
            }
        }

        paymentItemAdapter.setItems(getVisiblePreviewItems());
        updateToggleProductsUi();
        currentSubtotal = subtotal;
        currentProductDiscount = productDiscount;
        currentCarbonPoints = carbonPoints;
        currentCarbonEmission = carbonEmission;
        bindPaymentSummary();
    }

    private void bindPaymentSummary() {
        long voucherDiscount = 0;
        long shippingFee = currentSubtotal > 0 ? DEFAULT_SHIPPING_FEE : 0;
        long shippingDiscount = currentSubtotal >= FREE_SHIPPING_THRESHOLD ? shippingFee : 0;
        if (selectedVoucher != null && selectedVoucher.promotion != null) {
            if (isShippingPromotion(selectedVoucher.promotion)) {
                shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount,
                        calculatePromotionDiscount(selectedVoucher.promotion, shippingFee)));
            } else {
                voucherDiscount = Math.min(currentSubtotal,
                        calculatePromotionDiscount(selectedVoucher.promotion, currentSubtotal));
            }
        }
        long total = Math.max(0, currentSubtotal + shippingFee - voucherDiscount - shippingDiscount);
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

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvSelectedCount.setText("Đang tải khuyến mãi...");
        tvSelectedTitle.setText("");
        recyclerView.setAdapter(new VoucherOptionAdapter(new ArrayList<>(), RecyclerView.NO_POSITION, item -> {
        }));
        loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle);

        BottomSheetDialog dialog = createHalfHeightBottomSheetDialog(dialogView);
        btnApplyVoucher.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadVoucherOptions(RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        List<PromotionDto> promotions = new ArrayList<>();
        List<PromotionTargetDto> targets = new ArrayList<>();
        List<PromotionUsageDto> usages = new ArrayList<>();
        AtomicInteger remainingCalls = new AtomicInteger(3);

        Runnable renderWhenReady = () -> {
            if (remainingCalls.decrementAndGet() == 0) {
                runOnUiThread(() -> renderVoucherOptions(
                        promotions, targets, usages, recyclerView, selectedCountView, selectedTitleView));
            }
        };

        promotionApi.getPromotions(customerId, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) promotions.addAll(response.body());
                renderWhenReady.run();
            }

            @Override
            public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
                renderWhenReady.run();
            }
        });
        promotionApi.getPromotionTargets().enqueue(new Callback<ApiListResponseDto<PromotionTargetDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionTargetDto>> call, Response<ApiListResponseDto<PromotionTargetDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    targets.addAll(response.body().getData());
                }
                renderWhenReady.run();
            }

            @Override
            public void onFailure(Call<ApiListResponseDto<PromotionTargetDto>> call, Throwable t) {
                renderWhenReady.run();
            }
        });
        promotionApi.getPromotionUsages().enqueue(new Callback<ApiListResponseDto<PromotionUsageDto>>() {
            @Override
            public void onResponse(Call<ApiListResponseDto<PromotionUsageDto>> call, Response<ApiListResponseDto<PromotionUsageDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    usages.addAll(response.body().getData());
                }
                renderWhenReady.run();
            }

            @Override
            public void onFailure(Call<ApiListResponseDto<PromotionUsageDto>> call, Throwable t) {
                renderWhenReady.run();
            }
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
            if (!isActivePromotion(promotion)) continue;
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

        int selectedPosition = selectedVoucherPosition();
        updateVoucherSelectionText(selectedCountView, selectedTitleView);
        recyclerView.setAdapter(new VoucherOptionAdapter(voucherItems, selectedPosition, item -> {
            selectedVoucher = item;
            updateVoucherSelectionText(selectedCountView, selectedTitleView);
            updateSelectedVoucherUi();
            bindPaymentSummary();
        }));
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
        tvDistance.setText("20 km ~ 45 phút");

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
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                bottomSheet.setPadding(0, 0, 0, 0);
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
        return String.format(
                Locale.getDefault(),
                "Dự kiến nhận hàng hôm nay, %02d:%02d - %02d:%02d",
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
        double minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : 0;
        if (currentSubtotal < minOrder) {
            return "Đơn tối thiểu " + formatCurrency((long) minOrder);
        }
        if (!matchesTarget(target)) {
            return "Không áp dụng cho sản phẩm trong giỏ";
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageLimit() > 0 && usage != null
                && usage.getOrderIds() != null
                && usage.getOrderIds().size() >= promotion.getUsageLimit()) {
            return "Đã hết lượt sử dụng";
        }
        if (promotion.getUserLimit() != null && promotion.getUserLimit() > 0 && usage != null && usage.getUserIds() != null) {
            int userUseCount = 0;
            for (String userId : usage.getUserIds()) {
                if (customerId != null && customerId.equals(userId)) userUseCount++;
            }
            if (userUseCount >= promotion.getUserLimit()) {
                return "Bạn đã sử dụng mã này";
            }
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

    private void ensureLocationItems() {
        if (!locationItems.isEmpty()) {
            return;
        }
        addFallbackLocation();
    }

    private void loadAddresses() {
        if (customerId == null || customerId.trim().isEmpty() || addressRepository == null) {
            addFallbackLocation();
            applySelectedLocationToAddressCard();
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
                applySelectedLocationToAddressCard();
            }

            @Override
            public void onError(Throwable t) {
                if (locationItems.isEmpty()) {
                    addFallbackLocation();
                }
                applySelectedLocationToAddressCard();
            }
        });
    }

    private void addFallbackLocation() {
        AppPreferences preferences = new AppPreferences(this);
        String name = safeText(preferences.getFullName());
        String phone = safeText(preferences.getCurrentPhone());
        String displayName = joinNameAndPhone(
                name.isEmpty() ? "Người nhận" : name,
                phone
        );
        locationItems.add(new LocationOptionAdapter.LocationItemUiModel(
                displayName,
                "Vui lòng chọn hoặc thêm địa chỉ giao hàng",
                true
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
