package com.veggo.app.presentation.cart;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.adapter.CartAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.checkout.CheckoutGuestActivity;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;
import com.veggo.app.presentation.auth.LoginActivity;
import com.veggo.app.presentation.product.ProductDetailActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
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

public class CartFragment extends BaseFragment implements CartAdapter.CartItemActionListener {
    public static final String ARG_SELECTED_SKUS = "arg_selected_skus";
    private static final int MIN_QUANTITY = 1;
    private static final long FREE_SHIPPING_THRESHOLD = 300_000L;
    private static final long DEFAULT_SHIPPING_FEE = 30_000L;

    private final List<CartAdapter.CartItemUiModel> cartItems = new ArrayList<>();
    private final List<VoucherOptionAdapter.VoucherItemUiModel> voucherItems = new ArrayList<>();
    private View rootView;
    private PopupWindow activePopup;
    private View scrimView;
    private ImageView imgPaymentChevron;
    private ImageView imgCbAll;
    private RecyclerView layoutCartItemsContainer;
    private View layoutCartEmptyState;
    private CartAdapter cartAdapter;
    private TextView tvCartTitle;
    private TextView tvCartTotal;
    private TextView btnCheckout;
    private TextView tvSelectedVoucher;
    private TextView tvCartCarbonQuote;
    private boolean isAllChecked = true;
    private CartViewModel viewModel;
    private String customerId;
    private VoucherOptionAdapter.VoucherItemUiModel selectedVoucher;
    private Set<String> selectedSkuFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cart, container, false);
        rootView.findViewById(R.id.cartBackButton).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        scrimView = rootView.findViewById(R.id.viewScrim);
        layoutCartItemsContainer = rootView.findViewById(R.id.layoutCartItemsContainer);

        View layoutVoucher = rootView.findViewById(R.id.layoutVoucher);
        View layoutPaymentDetail = rootView.findViewById(R.id.layoutPaymentDetail);
        View layoutBottomSummaryRow = rootView.findViewById(R.id.layoutBottomSummaryRow);
        tvCartTitle = rootView.findViewById(R.id.tvCartTitle);
        tvCartTotal = rootView.findViewById(R.id.tvCartTotal);
        btnCheckout = rootView.findViewById(R.id.btnCheckout);
        tvSelectedVoucher = rootView.findViewById(R.id.tvSelectedVoucher);
        tvCartCarbonQuote = rootView.findViewById(R.id.tvCartCarbonQuote);
        imgCbAll = rootView.findViewById(R.id.imgCbAll);
        imgPaymentChevron = rootView.findViewById(R.id.imgPaymentChevron);
        layoutCartEmptyState = rootView.findViewById(R.id.layoutCartEmptyState);

        customerId = resolveCustomerId();
        ArrayList<String> selectedSkus = getArguments() == null
                ? null
                : getArguments().getStringArrayList(ARG_SELECTED_SKUS);
        selectedSkuFilter = selectedSkus == null || selectedSkus.isEmpty()
                ? null
                : new HashSet<>(selectedSkus);
        
        android.util.Log.d("VEGGO_DEBUG", "CartFragment is using CustomerId: " + customerId);
        
        setupViewModel();
        setupCartList();
        observeViewModel();
        
        viewModel.fetchCart(customerId);

        layoutVoucher.setOnClickListener(v -> showVoucherDialog());
        layoutPaymentDetail.setOnClickListener(v -> showPaymentDetailDialog());
        scrimView.setOnClickListener(v -> dismissActivePopup());
        imgCbAll.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        rootView.findViewById(R.id.btnEmptyCartShopNow).setOnClickListener(v -> openShoppingCategoryList());
        btnCheckout.setOnClickListener(v -> {
            ArrayList<String> selectedCartLineKeys = selectedCartLineKeys();
            if (selectedCartLineKeys.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn sản phẩm để mua hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoggedIn()) {
                startAccountCheckout(selectedCartLineKeys);
            } else {
                showCheckoutRoleDialog(selectedCartLineKeys);
            }
        });

        return rootView;
    }

    private void openShoppingCategoryList() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.setBottomNavVisible(true);
            requireActivity().getSupportFragmentManager().popBackStack();
            activity.openCategoryDetail(null, null);
        }
    }

    private boolean isLoggedIn() {
        return new AppPreferences(requireContext()).isLoggedIn();
    }

    private void startAccountCheckout(ArrayList<String> selectedCartLineKeys) {
        Intent intent = new Intent(requireContext(), CheckoutActivity.class);
        intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_LINE_KEYS, selectedCartLineKeys);
        if (selectedVoucher != null) {
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_ID, selectedVoucher.promotionId);
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_TITLE, selectedVoucher.title);
        }
        startActivity(intent);
    }

    private void showCheckoutRoleDialog(ArrayList<String> selectedCartLineKeys) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkout_type, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);
        dialogView.findViewById(R.id.btnOpenCheckout).setOnClickListener(v -> {
            new PendingCheckoutStore(requireContext()).saveCartCheckout(
                    selectedCartLineKeys,
                    selectedVoucher == null ? null : selectedVoucher.promotionId,
                    selectedVoucher == null ? null : selectedVoucher.title
            );
            dialog.dismiss();
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });
        dialogView.findViewById(R.id.btnOpenCheckoutGuest).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CheckoutGuestActivity.class);
            intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_LINE_KEYS, selectedCartLineKeys);
            if (selectedVoucher != null) {
                intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_ID, selectedVoucher.promotionId);
                intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_TITLE, selectedVoucher.title);
            }
            dialog.dismiss();
            startActivity(intent);
        });
        dialog.show();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideCartRepository(requireContext()));
        viewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getCartData().observe(getViewLifecycleOwner(), cartDto -> {
            if (cartDto != null) {
                mapDtoToUiModel(cartDto);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Có thể thêm loading state UI ở đây
        });
    }

    private void mapDtoToUiModel(CartDto cartDto) {
        // Lưu lại trạng thái checkbox hiện tại theo SKU + biến thể khối lượng
        java.util.Map<String, Boolean> checkStates = new java.util.HashMap<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            checkStates.put(item.cartLineKey(), item.isChecked);
        }

        cartItems.clear();
        if (cartDto.getItems() != null) {
            for (CartDto.CartItemDto itemDto : cartDto.getItems()) {
                ProductDto product = itemDto.getProduct();
                String sku = itemDto.getSku();

                String name = "Sản phẩm VEGGO";
                String imageUrl = "";
                long price = 0;
                long originalPrice = 0;
                double carbonPoint = 0;

                if (product != null) {
                    if (product.getProductName() != null && !product.getProductName().isEmpty()) {
                        name = product.getProductName();
                    }
                    imageUrl = product.getFirstImage();
                    price = product.getPrice();
                    originalPrice = product.getOriginalPrice();
                    carbonPoint = product.getCarbonSavingPoint();
                }
                if (price <= 0) {
                    price = itemDto.getPrice();
                }
                if (originalPrice <= 0) {
                    originalPrice = itemDto.getOriginalPrice();
                }
                if (originalPrice < price) {
                    originalPrice = price;
                }

                double selectedWeight = itemDto.getSelectedWeight() > 0 ? itemDto.getSelectedWeight() : 1.0;
                boolean hasWeightOptions = product != null
                        && product.getWeightOptions() != null
                        && !product.getWeightOptions().isEmpty();
                CartAdapter.CartItemUiModel uiModel = new CartAdapter.CartItemUiModel(
                        sku,
                        name,
                        hasWeightOptions ? formatWeight(selectedWeight) : "",
                        selectedWeight,
                        hasWeightOptions,
                        carbonPoint,
                        price,
                        originalPrice,
                        imageUrl,
                        itemDto.getQuantity(),
                        product
                );

                if (selectedSkuFilter != null) {
                    uiModel.isChecked = selectedSkuFilter.contains(uiModel.sku);
                } else if (checkStates.containsKey(uiModel.cartLineKey())) {
                    // Khôi phục trạng thái checkbox nếu SKU này đã có trước đó
                    uiModel.isChecked = Boolean.TRUE.equals(checkStates.get(uiModel.cartLineKey()));
                }

                cartItems.add(uiModel);
            }
        }
        
        cartAdapter.notifyDataSetChanged();
        syncAllCheckboxState();
        updateCartSummary();
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

    private void setupCartList() {
        layoutCartItemsContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(cartItems, this);
        layoutCartItemsContainer.setAdapter(cartAdapter);
    }

    @Override
    public void onItemCheckedChanged(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.get(position).isChecked = !cartItems.get(position).isChecked;
            cartAdapter.notifyItemChanged(position);
            syncAllCheckboxState();
            updateCartSummary();
        }
    }

    @Override
    public void onItemRemoved(int position) {
        if (position >= 0 && position < cartItems.size()) {
            CartAdapter.CartItemUiModel item = cartItems.get(position);
            VeggoDialog.show(
                    requireContext(),
                    R.drawable.ic_trash,
                    "Xác nhận xoá sản phẩm",
                    "Bạn có chắc chắn muốn xoá sản phẩm này khỏi giỏ hàng không?",
                    "Xoá",
                    "Hủy",
                    new VeggoDialog.DialogListener() {
                        @Override
                        public void onConfirm() {
                            viewModel.removeItem(customerId, item.sku, item.selectedWeightValue);
                        }
                    }
            );
        }
    }

    @Override
    public void onItemQuantityChanged(int position, int delta) {
        if (position >= 0 && position < cartItems.size()) {
            CartAdapter.CartItemUiModel item = cartItems.get(position);
            int newQty = item.quantity + delta;
            if (newQty >= MIN_QUANTITY) {
                viewModel.updateQuantity(customerId, item.sku, newQty, item.selectedWeightValue);
            }
        }
    }

    @Override
    public void onItemClicked(int position) {
        openProductDetail(position, false);
    }

    @Override
    public void onItemLongClicked(int position) {
        openProductDetail(position, true);
    }

    private void openProductDetail(int position, boolean openAddToCart) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }

        CartAdapter.CartItemUiModel item = cartItems.get(position);
        String productId = item.product != null ? item.product.getId() : null;
        if (productId == null || productId.trim().isEmpty()) {
            productId = item.sku;
        }
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
        intent.putExtra(ProductDetailActivity.EXTRA_OPEN_ADD_TO_CART, openAddToCart);
        startActivity(intent);
    }

    private void showPopupAboveAnchor(int layoutResId, View anchorView) {
        dismissActivePopup();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(layoutResId, null, false);
        if (layoutResId == R.layout.dialog_payment_detail) {
            setupPaymentDetailPopup(dialogView);
        }

        int popupWidth = rootView.getWidth();
        dialogView.measure(
                MeasureSpec.makeMeasureSpec(popupWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = dialogView.getMeasuredHeight();

        activePopup = new PopupWindow(
                dialogView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        activePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        activePopup.setOutsideTouchable(true);
        activePopup.setElevation(12f);
        activePopup.setOnDismissListener(() -> {
            activePopup = null;
            updatePaymentChevron(false);
            if (scrimView != null) {
                scrimView.setVisibility(View.GONE);
            }
        });

        int[] rootLocation = new int[2];
        int[] anchorLocation = new int[2];
        rootView.getLocationInWindow(rootLocation);
        anchorView.getLocationInWindow(anchorLocation);

        int x = rootLocation[0];
        int y = anchorLocation[1] - popupHeight;
        
        if (scrimView != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrimView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.TOP;
            scrimView.setLayoutParams(params);
            scrimView.setVisibility(View.VISIBLE);
        }
        updatePaymentChevron(layoutResId == R.layout.dialog_payment_detail);
        activePopup.showAtLocation(rootView, Gravity.TOP | Gravity.START, x, y);
    }

    private void setupPaymentDetailPopup(View dialogView) {
        OrderSummary summary = buildOrderSummary();
        setMoney(dialogView, R.id.tvPaymentSubtotal, summary.subtotal);
        setDiscountMoney(dialogView, R.id.tvPaymentVoucherDiscount, summary.voucherDiscount);
        setDiscountMoney(dialogView, R.id.tvPaymentProductDiscount, summary.productDiscount);
        setMoney(dialogView, R.id.tvPaymentShippingFee, summary.shippingFee);
        setDiscountMoney(dialogView, R.id.tvPaymentShippingDiscount, summary.shippingDiscount);
        setMoney(dialogView, R.id.tvPaymentTotal, summary.total);
    }

    private void showPaymentDetailDialog() {
        dismissActivePopup();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment_detail, null, false);
        setupPaymentDetailPopup(dialogView);

        activePopup = new PopupWindow(
                dialogView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        activePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        activePopup.setOutsideTouchable(true);
        activePopup.setElevation(12f);
        activePopup.setOnDismissListener(() -> {
            activePopup = null;
            updatePaymentChevron(false);
            if (scrimView != null) {
                scrimView.setVisibility(View.GONE);
            }
        });

        if (scrimView != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrimView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.TOP;
            scrimView.setLayoutParams(params);
            scrimView.setVisibility(View.VISIBLE);
        }
        updatePaymentChevron(true);
        activePopup.showAtLocation(rootView, Gravity.BOTTOM | Gravity.START, 0, 0);
    }

    private void setMoney(View root, int id, long amount) {
        TextView textView = root.findViewById(id);
        if (textView != null) {
            textView.setText(formatCurrency(amount));
        }
    }

    private void setDiscountMoney(View root, int id, long amount) {
        TextView textView = root.findViewById(id);
        if (textView != null) {
            textView.setText(amount > 0 ? "-" + formatCurrency(amount) : "-0đ");
        }
    }

    private void showVoucherDialog() {
        dismissActivePopup();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_voucher, null, false);
        setupVoucherPopup(dialogView);
        int popupHeight = rootView != null && rootView.getHeight() > 0
                ? rootView.getHeight() / 2
                : getResources().getDisplayMetrics().heightPixels / 2;
        dialogView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeight
        ));

        activePopup = new PopupWindow(
                dialogView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeight,
                true
        );
        activePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        activePopup.setOutsideTouchable(true);
        activePopup.setElevation(12f);
        activePopup.setOnDismissListener(() -> {
            activePopup = null;
            if (scrimView != null) {
                scrimView.setVisibility(View.GONE);
            }
        });

        if (scrimView != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrimView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.TOP;
            scrimView.setLayoutParams(params);
            scrimView.setVisibility(View.VISIBLE);
        }

        activePopup.showAtLocation(rootView, Gravity.BOTTOM | Gravity.START, 0, 0);
    }

    private void setupVoucherPopup(View dialogView) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvVoucherOptions);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvVoucherSelectedCount);
        TextView tvSelectedTitle = dialogView.findViewById(R.id.tvVoucherSelectedTitle);
        TextView btnApplyVoucher = dialogView.findViewById(R.id.btnApplyVoucher);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tvSelectedCount.setText("Đang tải khuyến mãi...");
        tvSelectedTitle.setText("");
        recyclerView.setAdapter(new VoucherOptionAdapter(new ArrayList<>(), RecyclerView.NO_POSITION, item -> {
        }));

        loadVoucherOptions(recyclerView, tvSelectedCount, tvSelectedTitle);

        btnApplyVoucher.setOnClickListener(v -> dismissActivePopup());
    }

    private void loadVoucherOptions(RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        List<PromotionDto> promotions = new ArrayList<>();
        List<PromotionTargetDto> targets = new ArrayList<>();
        List<PromotionUsageDto> usages = new ArrayList<>();
        AtomicInteger remainingCalls = new AtomicInteger(3);

        Runnable renderWhenReady = () -> {
            if (remainingCalls.decrementAndGet() == 0 && isAdded()) {
                requireActivity().runOnUiThread(() ->
                        renderVoucherOptions(promotions, targets, usages, recyclerView, selectedCountView, selectedTitleView)
                );
            }
        };

        promotionApi.getPromotions(customerId, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    promotions.addAll(response.body());
                }
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
            if (target.getPromotionId() != null) {
                targetByPromotion.put(target.getPromotionId(), target);
            }
        }

        Map<String, PromotionUsageDto> usageByPromotion = new HashMap<>();
        for (PromotionUsageDto usage : usages) {
            if (usage.getPromotionId() != null) {
                usageByPromotion.put(usage.getPromotionId(), usage);
            }
        }

        long subtotal = selectedSubtotal();
        for (PromotionDto promotion : promotions) {
            if (!isActivePromotion(promotion)) {
                continue;
            }
            if (promotion.getPromotionKind() != null && "FlashSale".equalsIgnoreCase(promotion.getPromotionKind())) {
                continue;
            }

            PromotionTargetDto target = targetByPromotion.get(promotion.getPromotionId());
            PromotionUsageDto usage = usageByPromotion.get(promotion.getPromotionId());
            String disabledReason = disabledReason(promotion, target, usage, subtotal);
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
        if (selectedPosition == RecyclerView.NO_POSITION && selectedVoucher != null) {
            selectedVoucher = null;
            updateSelectedVoucherUi();
            updateCartSummary();
        }
        updateVoucherSelectionText(selectedCountView, selectedTitleView);
        recyclerView.setAdapter(new VoucherOptionAdapter(voucherItems, selectedPosition, item -> {
            selectedVoucher = item;
            updateVoucherSelectionText(selectedCountView, selectedTitleView);
            updateSelectedVoucherUi();
            updateCartSummary();
        }));
    }

    private void updateSelectedVoucherUi() {
        if (tvSelectedVoucher == null) {
            return;
        }
        if (selectedVoucher != null) {
            tvSelectedVoucher.setText(selectedVoucher.title);
            tvSelectedVoucher.setTextColor(getResources().getColor(R.color.primary_main, requireContext().getTheme()));
        } else {
            tvSelectedVoucher.setText("Chọn hoặc nhập mã");
            tvSelectedVoucher.setTextColor(android.graphics.Color.parseColor("#616161"));
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
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private String disabledReason(PromotionDto promotion, PromotionTargetDto target,
                                  PromotionUsageDto usage, long subtotal) {
        double minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : 0;
        if (subtotal < minOrder) {
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
                if (customerId.equals(userId)) {
                    userUseCount++;
                }
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
        for (CartAdapter.CartItemUiModel cartItem : cartItems) {
            if (!cartItem.isChecked) {
                continue;
            }
            if ("Product".equalsIgnoreCase(targetType) && refs.contains(cartItem.sku)) {
                return true;
            }
            ProductDto product = cartItem.product;
            if (product == null) {
                continue;
            }
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
        long now = System.currentTimeMillis();
        Long start = parseDateMillis(promotion.getStartDate());
        Long end = parseDateMillis(promotion.getEndDate());
        return (start == null || now >= start) && (end == null || now <= end);
    }

    private Long parseDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
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
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String voucherTitle(PromotionDto promotion) {
        if (promotion.getName() != null && !promotion.getName().trim().isEmpty()) {
            return promotion.getName();
        }
        if (promotion.getCode() != null && !promotion.getCode().trim().isEmpty()) {
            return promotion.getCode();
        }
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
        if (promotion.getEndDate() == null || promotion.getEndDate().trim().isEmpty()) {
            return "Không giới hạn thời gian";
        }
        return "HSD: " + promotion.getEndDate().substring(0, Math.min(10, promotion.getEndDate().length()));
    }

    private boolean isShippingPromotion(PromotionDto promotion) {
        String haystack = ((promotion.getName() == null ? "" : promotion.getName()) + " "
                + (promotion.getDescription() == null ? "" : promotion.getDescription()) + " "
                + (promotion.getScope() == null ? "" : promotion.getScope()) + " "
                + (promotion.getCode() == null ? "" : promotion.getCode())).toLowerCase(Locale.US);
        return haystack.contains("ship") || haystack.contains("vận chuyển") || haystack.contains("shipping");
    }

    private long selectedSubtotal() {
        long total = 0;
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                total += item.getLineTotal();
            }
        }
        return total;
    }

    private long selectedShippingFee(long subtotal) {
        return subtotal <= 0 ? 0 : DEFAULT_SHIPPING_FEE;
    }

    private OrderSummary buildOrderSummary() {
        long subtotal = 0;
        long originalSubtotal = 0;
        int selectedCount = 0;
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                selectedCount++;
                subtotal += item.getLineTotal();
                originalSubtotal += item.getLineOriginalTotal();
            }
        }

        long productDiscount = Math.max(0, originalSubtotal - subtotal);
        long shippingFee = selectedShippingFee(subtotal);
        long voucherDiscount = 0;
        long shippingDiscount = subtotal >= FREE_SHIPPING_THRESHOLD && shippingFee > 0 ? shippingFee : 0;
        if (selectedVoucher != null && selectedVoucher.promotion != null) {
            if (isShippingPromotion(selectedVoucher.promotion)) {
                long voucherShippingDiscount = calculatePromotionDiscount(selectedVoucher.promotion, shippingFee);
                shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount, voucherShippingDiscount));
            } else {
                voucherDiscount = Math.min(subtotal, calculatePromotionDiscount(selectedVoucher.promotion, subtotal));
            }
        }

        long total = Math.max(0, subtotal + shippingFee - voucherDiscount - shippingDiscount);
        return new OrderSummary(selectedCount, subtotal, productDiscount, shippingFee, voucherDiscount, shippingDiscount, total);
    }

    private long calculatePromotionDiscount(PromotionDto promotion, long baseAmount) {
        if (promotion == null || baseAmount <= 0) {
            return 0;
        }
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

    private void toggleAllCheckbox(ImageView imageView) {
        isAllChecked = !isAllChecked;
        updateCheckboxIcon(imageView, isAllChecked);

        for (CartAdapter.CartItemUiModel item : cartItems) {
            item.isChecked = isAllChecked;
        }
        cartAdapter.notifyDataSetChanged();
        updateCartSummary();
    }

    private void syncAllCheckboxState() {
        boolean areAllChecked = !cartItems.isEmpty();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (!item.isChecked) {
                areAllChecked = false;
                break;
            }
        }
        isAllChecked = areAllChecked;
        if (imgCbAll != null) {
            updateCheckboxIcon(imgCbAll, isAllChecked);
        }
    }

    private void updateCartSummary() {
        OrderSummary summary = buildOrderSummary();

        if (tvCartTitle != null) {
            tvCartTitle.setText(getString(R.string.cart_title_format, cartItems.size()));
        }
        if (tvCartTotal != null) {
            tvCartTotal.setText(formatCurrency(summary.total));
        }
        if (btnCheckout != null) {
            btnCheckout.setText(getString(R.string.cart_checkout_format, summary.selectedCount));
        }
        if (tvCartCarbonQuote != null) {
            tvCartCarbonQuote.setText("Nhận " + formatCarbonPoints(selectedCarbonPoints()) + " điểm carbon cho đơn này");
        }
        boolean isCartEmpty = cartItems.isEmpty();
        if (layoutCartItemsContainer != null) {
            layoutCartItemsContainer.setVisibility(isCartEmpty ? View.GONE : View.VISIBLE);
        }
        if (layoutCartEmptyState != null) {
            layoutCartEmptyState.setVisibility(isCartEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private double selectedCarbonPoints() {
        double total = 0;
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                total += item.carbonSavingPoint * item.quantity;
            }
        }
        return total;
    }

    private ArrayList<String> selectedCartLineKeys() {
        ArrayList<String> selectedKeys = new ArrayList<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                selectedKeys.add(item.cartLineKey());
            }
        }
        return selectedKeys;
    }

    private String formatCarbonPoints(double points) {
        if (Math.abs(points - Math.round(points)) < 0.0001) {
            return String.format(Locale.US, "%.0f", points);
        }
        return String.format(Locale.US, "%.1f", points);
    }

    private void updateCheckboxIcon(ImageView imageView, boolean isChecked) {
        imageView.setImageResource(
                isChecked ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_uncheck
        );
    }

    private void updatePaymentChevron(boolean isOpen) {
        if (imgPaymentChevron != null) {
            imgPaymentChevron.setRotation(isOpen ? 180f : 0f);
        }
    }

    private void dismissActivePopup() {
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.dismiss();
        }
        // Đảm bảo ẩn scrim ngay lập tức khi yêu cầu đóng popup
        if (scrimView != null) {
            scrimView.setVisibility(View.GONE);
        }
        updatePaymentChevron(false);
    }

    private String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(requireContext());
        String resolvedCustomerId = appPreferences.getCustomerId();
        if (resolvedCustomerId == null || resolvedCustomerId.trim().isEmpty()) {
            resolvedCustomerId = new PreferencesManager(requireContext()).getUserId();
        }
        if (resolvedCustomerId == null || resolvedCustomerId.trim().isEmpty()) {
            resolvedCustomerId = new PendingCheckoutStore(requireContext()).guestId();
        }
        return resolvedCustomerId;
    }

    private static final class OrderSummary {
        final int selectedCount;
        final long subtotal;
        final long productDiscount;
        final long shippingFee;
        final long voucherDiscount;
        final long shippingDiscount;
        final long total;

        OrderSummary(int selectedCount, long subtotal, long productDiscount, long shippingFee,
                     long voucherDiscount, long shippingDiscount, long total) {
            this.selectedCount = selectedCount;
            this.subtotal = subtotal;
            this.productDiscount = productDiscount;
            this.shippingFee = shippingFee;
            this.voucherDiscount = voucherDiscount;
            this.shippingDiscount = shippingDiscount;
            this.total = total;
        }
    }
}
