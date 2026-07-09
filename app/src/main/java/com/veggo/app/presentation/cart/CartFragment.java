package com.veggo.app.presentation.cart;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.adapter.CartAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.ApiListResponseDto;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.auth.LoginActivity;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.checkout.CheckoutGuestActivity;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.presentation.product.ProductDetailActivity;
import com.veggo.app.presentation.promotion.PromotionVoucherHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private final List<VoucherOptionAdapter.VoucherItemUiModel> allVoucherItems = new ArrayList<>();
    private View rootView;
    private PopupWindow activePopup;
    private BottomSheetDialog activeVoucherDialog;
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
    private TextView tvVoucherLabel;
    private TextView tvCartCarbonQuote;
    private TextView cartEditButton;
    private TextView btnCartSaveFavorite;
    private TextView btnCartDeleteSelected;
    private View layoutVoucher;
    private View layoutCartEditActions;
    private View layoutPaymentDetail;
    private View layoutBottomSummaryRow;
    private boolean isEditMode;
    private FavoriteStore favoriteStore;
    private boolean isAllChecked = true;
    private CartViewModel viewModel;
    private String customerId;
    private VoucherOptionAdapter.VoucherItemUiModel selectedProductVoucher;
    private VoucherOptionAdapter.VoucherItemUiModel selectedShippingVoucher;
    private PromotionVoucherHelper.VoucherFilter activeVoucherFilter = PromotionVoucherHelper.VoucherFilter.PRODUCT;
    private Set<String> selectedSkuFilter;
    private SwipeRefreshLayout cartRefreshLayout;
    private boolean cartRefreshPending;

    private List<PromotionTargetDto> cachedTargets = new ArrayList<>();
    private List<PromotionUsageDto> cachedUsages = new ArrayList<>();

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
        this.layoutVoucher = layoutVoucher;
        this.layoutPaymentDetail = layoutPaymentDetail;
        this.layoutBottomSummaryRow = layoutBottomSummaryRow;
        layoutCartEditActions = rootView.findViewById(R.id.layoutCartEditActions);
        tvCartTitle = rootView.findViewById(R.id.tvCartTitle);
        tvCartTotal = rootView.findViewById(R.id.tvCartTotal);
        btnCheckout = rootView.findViewById(R.id.btnCheckout);
        tvSelectedVoucher = rootView.findViewById(R.id.tvSelectedVoucher);
        tvVoucherLabel = rootView.findViewById(R.id.tvVoucherLabel);
        tvCartCarbonQuote = rootView.findViewById(R.id.tvCartCarbonQuote);
        imgCbAll = rootView.findViewById(R.id.imgCbAll);
        imgPaymentChevron = rootView.findViewById(R.id.imgPaymentChevron);
        layoutCartEmptyState = rootView.findViewById(R.id.layoutCartEmptyState);
        cartEditButton = rootView.findViewById(R.id.cartEditButton);
        btnCartSaveFavorite = rootView.findViewById(R.id.btnCartSaveFavorite);
        btnCartDeleteSelected = rootView.findViewById(R.id.btnCartDeleteSelected);
        favoriteStore = new FavoriteStore(requireContext());

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
        setupPullToRefresh();
        setupEditModeAction();
        observeViewModel();
        prefetchVoucherData();
        
        viewModel.fetchCart(customerId);

        layoutVoucher.setOnClickListener(v -> showVoucherDialog());
        layoutPaymentDetail.setOnClickListener(v -> showPaymentDetailDialog());
        scrimView.setOnClickListener(v -> dismissActivePopup());
        imgCbAll.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        View layoutSelectAllRow = rootView.findViewById(R.id.layoutSelectAllRow);
        if (layoutSelectAllRow != null) {
            layoutSelectAllRow.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        }
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

    private void setupPullToRefresh() {
        cartRefreshLayout = PullToRefreshHelper.wrap(layoutCartItemsContainer, () -> {
            cartRefreshPending = true;
            viewModel.fetchCart(customerId);
        });
    }

    private void setupEditModeAction() {
        if (cartEditButton == null) {
            return;
        }
        cartEditButton.setOnClickListener(v -> toggleEditMode());
        if (btnCartSaveFavorite != null) {
            btnCartSaveFavorite.setOnClickListener(v -> saveSelectedToFavorites());
        }
        if (btnCartDeleteSelected != null) {
            btnCartDeleteSelected.setOnClickListener(v -> confirmDeleteSelectedItems());
        }
        applyEditModeUi();
    }

    private void toggleEditMode() {
        if (cartItems.isEmpty()) {
            return;
        }
        isEditMode = !isEditMode;
        dismissActivePopup();
        dismissActiveVoucherDialog();
        applyEditModeUi();
        if (cartAdapter != null) {
            cartAdapter.setEditMode(isEditMode);
        }
    }

    private void applyEditModeUi() {
        if (cartEditButton != null) {
            cartEditButton.setText(isEditMode
                    ? getString(R.string.cart_edit_done)
                    : getString(R.string.cart_edit));
            cartEditButton.setVisibility(cartItems.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (layoutVoucher != null) {
            layoutVoucher.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
        if (tvCartCarbonQuote != null) {
            tvCartCarbonQuote.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
        if (layoutPaymentDetail != null) {
            layoutPaymentDetail.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
        if (imgPaymentChevron != null) {
            imgPaymentChevron.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
        if (btnCheckout != null) {
            btnCheckout.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
        if (layoutCartEditActions != null) {
            layoutCartEditActions.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        if (layoutBottomSummaryRow != null) {
            layoutBottomSummaryRow.setClickable(!isEditMode);
            layoutBottomSummaryRow.setFocusable(!isEditMode);
        }
    }

    private List<CartAdapter.CartItemUiModel> selectedItems() {
        List<CartAdapter.CartItemUiModel> selected = new ArrayList<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                selected.add(item);
            }
        }
        return selected;
    }

    private void saveSelectedToFavorites() {
        List<CartAdapter.CartItemUiModel> selected = selectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_select_items_first, Toast.LENGTH_SHORT).show();
            return;
        }

        int savedCount = 0;
        for (CartAdapter.CartItemUiModel item : selected) {
            String productId = item.product != null ? item.product.getId() : item.sku;
            if (productId == null || productId.trim().isEmpty()) {
                continue;
            }
            if (!favoriteStore.isFavorite(FavoriteStore.TYPE_PRODUCT, productId)) {
                favoriteStore.add(new FavoriteStore.FavoriteItem(
                        FavoriteStore.TYPE_PRODUCT,
                        productId,
                        item.name,
                        favoriteProductSubtitle(item),
                        item.imageUrl
                ));
                savedCount++;
            }
        }
        if (savedCount == 0) {
            Toast.makeText(requireContext(), "Các sản phẩm đã chọn đã có trong yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(
                requireContext(),
                getString(R.string.cart_saved_to_favorites_format, savedCount),
                Toast.LENGTH_SHORT
        ).show();
    }

    private String favoriteProductSubtitle(CartAdapter.CartItemUiModel item) {
        String subtitle = CurrencyFormatter.formatVnd(item.getLineTotal() / Math.max(1, item.quantity));
        if (item.carbonSavingPoint > 0) {
            subtitle += " • " + formatCarbonPoints(item.carbonSavingPoint) + " điểm carbon";
        }
        return subtitle;
    }

    private void confirmDeleteSelectedItems() {
        List<CartAdapter.CartItemUiModel> selected = selectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_select_items_first, Toast.LENGTH_SHORT).show();
            return;
        }

        VeggoDialog.show(
                requireContext(),
                R.drawable.ic_trash,
                getString(R.string.cart_delete_selected_confirm_title),
                getString(R.string.cart_delete_selected_confirm_message, selected.size()),
                getString(R.string.cart_delete_selected),
                "Hủy",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        deleteSelectedItems(selected);
                    }
                }
        );
    }

    private void deleteSelectedItems(List<CartAdapter.CartItemUiModel> selected) {
        for (CartAdapter.CartItemUiModel item : selected) {
            viewModel.removeItem(customerId, item.sku, item.selectedWeightValue);
        }
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
        putSelectedVoucherExtras(intent);
        startActivity(intent);
    }

    private void showCheckoutRoleDialog(ArrayList<String> selectedCartLineKeys) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkout_type, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);
        dialogView.findViewById(R.id.btnOpenCheckout).setOnClickListener(v -> {
            new PendingCheckoutStore(requireContext()).saveCartCheckout(
                    selectedCartLineKeys,
                    selectedProductVoucher == null ? null : selectedProductVoucher.promotionId,
                    selectedProductVoucher == null ? null : selectedProductVoucher.title,
                    selectedShippingVoucher == null ? null : selectedShippingVoucher.promotionId,
                    selectedShippingVoucher == null ? null : selectedShippingVoucher.title
            );
            dialog.dismiss();
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });
        dialogView.findViewById(R.id.btnOpenCheckoutGuest).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CheckoutGuestActivity.class);
            intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_LINE_KEYS, selectedCartLineKeys);
            putSelectedVoucherExtras(intent);
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
            if (cartRefreshPending && !Boolean.TRUE.equals(isLoading)) {
                cartRefreshPending = false;
                PullToRefreshHelper.finish(cartRefreshLayout);
            }
        });
    }

    private void mapDtoToUiModel(CartDto cartDto) {
        java.util.Map<String, Boolean> checkStates = new java.util.HashMap<>();
        List<String> existingOrder = new ArrayList<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            checkStates.put(item.cartLineKey(), item.isChecked);
            existingOrder.add(item.cartLineKey());
        }

        java.util.Map<String, CartDto.CartItemDto> dtoByKey = new java.util.LinkedHashMap<>();
        if (cartDto.getItems() != null) {
            for (CartDto.CartItemDto itemDto : cartDto.getItems()) {
                dtoByKey.put(cartLineKey(itemDto), itemDto);
            }
        }

        cartItems.clear();

        for (String key : existingOrder) {
            CartDto.CartItemDto itemDto = dtoByKey.remove(key);
            if (itemDto != null) {
                cartItems.add(createUiModelFromDto(itemDto, checkStates));
            }
        }

        for (CartDto.CartItemDto itemDto : dtoByKey.values()) {
            cartItems.add(createUiModelFromDto(itemDto, checkStates));
        }

        cartAdapter.notifyDataSetChanged();
        syncAllCheckboxState();
        validateAppliedVoucher();
        updateCartSummary();

        triggerCartPriceAlerts();
    }

    private CartAdapter.CartItemUiModel createUiModelFromDto(
            CartDto.CartItemDto itemDto,
            java.util.Map<String, Boolean> checkStates
    ) {
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
            uiModel.isChecked = Boolean.TRUE.equals(checkStates.get(uiModel.cartLineKey()));
        }

        return uiModel;
    }

    private String cartLineKey(CartDto.CartItemDto itemDto) {
        if (itemDto == null) {
            return "";
        }
        double selectedWeight = itemDto.getSelectedWeight() > 0 ? itemDto.getSelectedWeight() : 1.0;
        return itemDto.getSku() + "#" + trimTrailingZeros(selectedWeight);
    }

    private void triggerCartPriceAlerts() {
        if (cartItems == null || cartItems.isEmpty()) return;

        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.product != null && item.product.getId() != null) {
                com.veggo.app.presentation.dialog.PriceAlertHelper.checkAndShowPriceAlert(
                        requireContext(),
                        item.product.getId(),
                        item.name,
                        item.imageUrl,
                        item.price,
                        productId -> triggerCheckoutForProduct(productId)
                );
            }
        }
    }

    private void triggerCheckoutForProduct(String productId) {
        if (cartItems == null || productId == null) return;

        CartAdapter.CartItemUiModel foundItem = null;
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.product != null && productId.equals(item.product.getId())) {
                foundItem = item;
                break;
            }
        }

        if (foundItem != null) {
            ArrayList<String> keys = new ArrayList<>();
            keys.add(foundItem.cartLineKey());

            if (isLoggedIn()) {
                startAccountCheckout(keys);
            } else {
                showCheckoutRoleDialog(keys);
            }
        }
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
            validateAppliedVoucher();
            updateCartSummary();
        }
    }

    @Override
    public void onItemRemoved(int position) {
        confirmRemoveItem(position);
    }

    private void confirmRemoveItem(int position) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }
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

    @Override
    public void onItemQuantityChanged(int position, int delta) {
        if (position >= 0 && position < cartItems.size()) {
            CartAdapter.CartItemUiModel item = cartItems.get(position);
            int newQty = item.quantity + delta;
            if (newQty < MIN_QUANTITY) {
                if (delta < 0 && item.quantity == MIN_QUANTITY) {
                    confirmRemoveItem(position);
                }
                return;
            }
            viewModel.updateQuantity(customerId, item.sku, newQty, item.selectedWeightValue);
        }
    }

    @Override
    public void onItemClicked(int position) {
        openProductDetail(position);
    }

    @Override
    public void onItemLongClicked(int position) {
        showEditCartItemDialog(position);
    }

    @Override
    public void onItemVariantClicked(int position) {
        showEditCartItemDialog(position);
    }

    private void showEditCartItemDialog(int position) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }

        CartAdapter.CartItemUiModel item = cartItems.get(position);
        ProductDto productDto = item.product;
        if (productDto == null) {
            Toast.makeText(requireContext(), "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_add_to_cart_bottom_sheet, null, false);
        dialog.setContentView(view);

        ImageView ivThumb = view.findViewById(R.id.ivProductThumb);
        TextView tvName = view.findViewById(R.id.tvProductNamePopup);
        TextView tvPrice = view.findViewById(R.id.tvPricePopup);
        TextView tvOriginalPrice = view.findViewById(R.id.tvOriginalPricePopup);
        TextView tvPriceUnit = view.findViewById(R.id.tvPriceUnitPopup);
        TextView tvQuantity = view.findViewById(R.id.tvQuantityPopup);
        TextView tvTotal = view.findViewById(R.id.tvTotalPopup);
        View btnDecrease = view.findViewById(R.id.tvDecrease);
        View btnIncrease = view.findViewById(R.id.tvIncrease);
        TextView btnConfirm = view.findViewById(R.id.btnConfirmAddToCart);
        View btnClose = view.findViewById(R.id.btnClose);
        TextView tvWeightTitle = view.findViewById(R.id.tvWeightTitle);
        ChipGroup weightGroup = view.findViewById(R.id.cgWeight);

        long displayPrice = item.price;
        long displayOriginalPrice = item.oldPrice > item.price ? item.oldPrice : item.price;
        Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_vegetable).into(ivThumb);
        tvName.setText(item.name);
        tvPrice.setText(CurrencyFormatter.formatVnd(displayPrice));
        bindEditDialogOriginalPrice(tvOriginalPrice, displayOriginalPrice, displayPrice);
        btnConfirm.setText("Lưu thay đổi");

        final int[] quantity = {Math.max(MIN_QUANTITY, item.quantity)};
        final double[] selectedWeight = {item.selectedWeightValue};
        final double previousWeight = item.selectedWeightValue;
        boolean hasWeightOptions = item.hasWeightOptions;
        if (tvPriceUnit != null) {
            tvPriceUnit.setVisibility(hasWeightOptions ? View.VISIBLE : View.GONE);
            tvPriceUnit.setText("/ kg");
        }

        tvQuantity.setText(String.valueOf(quantity[0]));
        bindEditWeightOptions(
                tvWeightTitle,
                weightGroup,
                productDto.getWeightOptions(),
                selectedWeight,
                previousWeight,
                () -> updateEditDialogTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions)
        );
        updateEditDialogTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);

        btnDecrease.setOnClickListener(v -> {
            if (quantity[0] > MIN_QUANTITY) {
                quantity[0]--;
                tvQuantity.setText(String.valueOf(quantity[0]));
                updateEditDialogTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
            }
        });
        btnIncrease.setOnClickListener(v -> {
            quantity[0]++;
            tvQuantity.setText(String.valueOf(quantity[0]));
            updateEditDialogTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            viewModel.replaceCartItem(customerId, item.sku, previousWeight, quantity[0], selectedWeight[0]);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void bindEditDialogOriginalPrice(TextView originalPriceView, long originalPrice, long salePrice) {
        if (originalPriceView == null) {
            return;
        }
        if (originalPrice > salePrice && salePrice > 0) {
            originalPriceView.setVisibility(View.VISIBLE);
            originalPriceView.setText(CurrencyFormatter.formatVnd(originalPrice));
            originalPriceView.setPaintFlags(originalPriceView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            originalPriceView.setVisibility(View.GONE);
        }
    }

    private void updateEditDialogTotal(TextView totalView, long unitPrice, int quantity, double selectedWeight, boolean hasWeightOptions) {
        if (totalView == null) {
            return;
        }
        double multiplier = hasWeightOptions ? selectedWeight : 1.0;
        long total = Math.round(unitPrice * multiplier * quantity);
        totalView.setText(CurrencyFormatter.formatVnd(total));
    }

    private void bindEditWeightOptions(TextView titleView, ChipGroup chipGroup, List<Double> weightOptions,
                                       double[] selectedWeight, double currentWeight, Runnable onSelectionChanged) {
        if (chipGroup == null) {
            return;
        }

        chipGroup.removeAllViews();
        List<Double> resolvedOptions = new ArrayList<>();
        if (weightOptions != null) {
            for (Double option : weightOptions) {
                if (option != null && option > 0) {
                    resolvedOptions.add(option);
                }
            }
        }

        if (resolvedOptions.isEmpty()) {
            selectedWeight[0] = currentWeight > 0 ? currentWeight : 1.0;
            chipGroup.setVisibility(View.GONE);
            if (titleView != null) {
                titleView.setVisibility(View.GONE);
            }
            return;
        }

        chipGroup.setVisibility(View.VISIBLE);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
        }

        int initialIndex = 0;
        for (int i = 0; i < resolvedOptions.size(); i++) {
            if (weightsEqual(resolvedOptions.get(i), currentWeight)) {
                initialIndex = i;
                break;
            }
        }

        for (int i = 0; i < resolvedOptions.size(); i++) {
            double option = resolvedOptions.get(i);
            Chip chip = new Chip(requireContext());
            chip.setCheckable(true);
            chip.setText(formatWeight(option));
            chip.setChipBackgroundColorResource(R.color.chip_choice_background_color);
            chip.setChipStrokeColorResource(R.color.chip_choice_stroke_color);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_choice_text_color));
            chip.setChecked(i == initialIndex);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedWeight[0] = option;
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
                }
            });
            chipGroup.addView(chip);
        }

        selectedWeight[0] = resolvedOptions.get(initialIndex);
    }

    private boolean weightsEqual(double left, double right) {
        return Math.abs(left - right) < 0.0005d;
    }

    private void openProductDetail(int position) {
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
        dismissActiveVoucherDialog();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_voucher, null, false);
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

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
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
                    updateCartSummary();
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

        activeVoucherDialog = createHalfHeightBottomSheetDialog(dialogView);
        activeVoucherDialog.setOnDismissListener(dialog -> activeVoucherDialog = null);
        btnApplyVoucher.setOnClickListener(v -> dismissActiveVoucherDialog());
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
        activeVoucherDialog.show();
    }

    private BottomSheetDialog createHalfHeightBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
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

    private void dismissActiveVoucherDialog() {
        if (activeVoucherDialog != null && activeVoucherDialog.isShowing()) {
            activeVoucherDialog.dismiss();
        }
        activeVoucherDialog = null;
    }

    private boolean isSearchingVoucher = false;

    private void updateVoucherFilterTags(TextView productTag, TextView shippingTag) {
        boolean productSelected = activeVoucherFilter == PromotionVoucherHelper.VoucherFilter.PRODUCT;
        productTag.setBackground(null);
        shippingTag.setBackground(null);
        productTag.setTextColor(ContextCompat.getColor(
                requireContext(),
                productSelected ? R.color.primary_main : R.color.neutral_60
        ));
        shippingTag.setTextColor(ContextCompat.getColor(
                requireContext(),
                productSelected ? R.color.neutral_60 : R.color.primary_main
        ));
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
            VoucherOptionAdapter.VoucherItemUiModel resolved =
                    findVoucherItem(selectedProductVoucher.promotionId, false);
            if (resolved == null || !resolved.enabled) {
                selectedProductVoucher = null;
            } else {
                selectedProductVoucher = resolved;
            }
        }
        if (selectedShippingVoucher != null) {
            VoucherOptionAdapter.VoucherItemUiModel resolved =
                    findVoucherItem(selectedShippingVoucher.promotionId, true);
            if (resolved == null || !resolved.enabled) {
                selectedShippingVoucher = null;
            } else {
                selectedShippingVoucher = resolved;
            }
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

    private void putSelectedVoucherExtras(Intent intent) {
        if (selectedProductVoucher != null) {
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_ID, selectedProductVoucher.promotionId);
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_TITLE, selectedProductVoucher.title);
            intent.putExtra("extra_selected_product_voucher_dto", selectedProductVoucher.promotion);
        }
        if (selectedShippingVoucher != null) {
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_ID, selectedShippingVoucher.promotionId);
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_TITLE, selectedShippingVoucher.title);
            intent.putExtra("extra_selected_shipping_voucher_dto", selectedShippingVoucher.promotion);
        }
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

    private void searchVoucher(String code, RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        
        // Xóa list cũ và hiện loading ngay lập tức
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

        String resolvedCustomerId = resolveCustomerId();
        promotionApi.getPromotions(resolvedCustomerId, code, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (!isSearchingVoucher) return; // Bỏ qua nếu đã chuyển sang mode khác
                
                if (response.isSuccessful() && response.body() != null) {
                    List<PromotionDto> foundPromotions = response.body();
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (foundPromotions.isEmpty()) {
                                showVoucherEmptyState("Không tìm thấy mã '" + code + "'.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                tvListTitle.setVisibility(View.VISIBLE);
                                tvListTitle.setText("Kết quả tìm kiếm cho '" + code + "'");
                                recyclerView.setVisibility(View.VISIBLE);
                                renderVoucherOptions(foundPromotions, cachedTargets, cachedUsages, recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                            }
                        });
                    }
                } else {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showVoucherEmptyState("Mã '" + code + "' không hợp lệ.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                        });
                    }
                }
            }
            @Override public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
                if (!isSearchingVoucher || !isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showVoucherEmptyState("Lỗi kết nối. Vui lòng thử lại.", recyclerView, selectedCountView, selectedTitleView, tvEmptyState, tvListTitle);
                });
            }
        });
    }

    private void loadVoucherOptions(RecyclerView recyclerView, TextView selectedCountView, TextView selectedTitleView, TextView tvEmptyState, TextView tvListTitle) {
        PromotionApi promotionApi = AppModule.providePromotionApi();
        List<PromotionDto> promotions = new ArrayList<>();
        cachedTargets.clear();
        cachedUsages.clear();
        AtomicInteger remainingCalls = new AtomicInteger(3);

        String resolvedCustomerId = resolveCustomerId();

        Runnable renderWhenReady = () -> {
            if (isSearchingVoucher) return; // Nếu đang search thì không ghi đề kết quả list mặc định

            if (remainingCalls.decrementAndGet() == 0 && isAdded()) {
                requireActivity().runOnUiThread(() -> {
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

        promotionApi.getPromotions(resolvedCustomerId, null, null).enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    promotions.addAll(response.body());
                }
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
        if (!isAdded()) {
            return;
        }
        allVoucherItems.clear();

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
            if (promotion.getPromotionKind() != null && "FlashSale".equalsIgnoreCase(promotion.getPromotionKind())) {
                continue;
            }
            if (!isSearchingVoucher && PromotionVoucherHelper.isPromotionExpired(promotion)) {
                continue;
            }

            PromotionTargetDto target = targetByPromotion.get(promotion.getPromotionId());
            PromotionUsageDto usage = usageByPromotion.get(promotion.getPromotionId());
            String disabledReason = disabledReason(promotion, target, usage, subtotal);
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
        updateCartSummary();
    }

    private void updateSelectedVoucherUi() {
        if (tvSelectedVoucher == null) {
            return;
        }
        int selectedCount = countSelectedVouchers();
        PromotionVoucherHelper.bindVoucherSelectionRow(
                tvVoucherLabel,
                tvSelectedVoucher,
                selectedCount > 0 ? buildSelectedVoucherSummary() : null,
                getResources().getColor(R.color.primary_main, requireContext().getTheme()),
                android.graphics.Color.parseColor("#616161")
        );
        if (activeVoucherDialog != null && activeVoucherDialog.isShowing()) {
            TextView tvSelectedCount = activeVoucherDialog.findViewById(R.id.tvVoucherSelectedCount);
            TextView tvSelectedTitle = activeVoucherDialog.findViewById(R.id.tvVoucherSelectedTitle);
            if (tvSelectedCount != null && tvSelectedTitle != null) {
                updateVoucherSelectionText(tvSelectedCount, tvSelectedTitle);
            }
            RecyclerView recyclerView = activeVoucherDialog.findViewById(R.id.rvVoucherOptions);
            if (recyclerView != null && recyclerView.getAdapter() instanceof VoucherOptionAdapter) {
                VoucherOptionAdapter adapter = (VoucherOptionAdapter) recyclerView.getAdapter();
                adapter.setSelectedPromotionIds(
                        selectedProductVoucher == null ? null : selectedProductVoucher.promotionId,
                        selectedShippingVoucher == null ? null : selectedShippingVoucher.promotionId
                );
            }
        }
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

    private String disabledReason(PromotionDto promotion, PromotionTargetDto target,
                                  PromotionUsageDto usage, long subtotal) {
        return PromotionVoucherHelper.evaluateDisabledReason(
                promotion,
                target,
                usage,
                subtotal,
                resolveCustomerId(),
                PromotionVoucherHelper.matchesPromotionTarget(
                        promotion,
                        target,
                        buildVoucherCartLines()
                )
        );
    }

    private List<PromotionVoucherHelper.VoucherCartLine> buildVoucherCartLines() {
        List<PromotionVoucherHelper.VoucherCartLine> lines = new ArrayList<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                lines.add(new PromotionVoucherHelper.VoucherCartLine(item.sku, item.product));
            }
        }
        return lines;
    }

    private boolean isActivePromotion(PromotionDto promotion) {
        if (promotion == null || !Boolean.TRUE.equals(promotion.getActive())) {
            return false;
        }
        return !PromotionVoucherHelper.isPromotionExpired(promotion)
                && !PromotionVoucherHelper.isPromotionNotStarted(promotion);
    }

    private String formatCurrency(long amount) {
        return PromotionVoucherHelper.formatCurrency(amount);
    }

    private boolean isShippingPromotion(PromotionDto promotion) {
        return VoucherOptionAdapter.isShippingPromotion(promotion);
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
        if (selectedProductVoucher != null && selectedProductVoucher.promotion != null) {
            voucherDiscount = Math.min(subtotal,
                    calculatePromotionDiscount(selectedProductVoucher.promotion, subtotal));
        }
        if (selectedShippingVoucher != null && selectedShippingVoucher.promotion != null) {
            long voucherShippingDiscount = calculatePromotionDiscount(
                    selectedShippingVoucher.promotion,
                    shippingFee
            );
            shippingDiscount = Math.min(shippingFee, Math.max(shippingDiscount, voucherShippingDiscount));
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
        validateAppliedVoucher();
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
        if (isCartEmpty && isEditMode) {
            isEditMode = false;
            if (cartAdapter != null) {
                cartAdapter.setEditMode(false);
            }
        }
        applyEditModeUi();
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

    private PromotionTargetDto findTargetForPromotion(String promotionId) {
        if (promotionId == null) return null;
        for (PromotionTargetDto target : cachedTargets) {
            if (promotionId.equals(target.getPromotionId())) {
                return target;
            }
        }
        return null;
    }

    private PromotionUsageDto findUsageForPromotion(String promotionId) {
        if (promotionId == null) return null;
        for (PromotionUsageDto usage : cachedUsages) {
            if (promotionId.equals(usage.getPromotionId())) {
                return usage;
            }
        }
        return null;
    }

    private void validateAppliedVoucher() {
        long subtotal = selectedSubtotal();
        if (subtotal <= 0) {
            return;
        }
        boolean hadProductVoucher = selectedProductVoucher != null;
        boolean hadShippingVoucher = selectedShippingVoucher != null;
        boolean changed = false;

        if (selectedProductVoucher != null && selectedProductVoucher.promotion != null) {
            PromotionTargetDto target = findTargetForPromotion(selectedProductVoucher.promotionId);
            PromotionUsageDto usage = findUsageForPromotion(selectedProductVoucher.promotionId);
            String reason = disabledReason(selectedProductVoucher.promotion, target, usage, subtotal);
            if (reason != null) {
                selectedProductVoucher = null;
                changed = true;
            }
        }

        if (selectedShippingVoucher != null && selectedShippingVoucher.promotion != null) {
            PromotionTargetDto target = findTargetForPromotion(selectedShippingVoucher.promotionId);
            PromotionUsageDto usage = findUsageForPromotion(selectedShippingVoucher.promotionId);
            String reason = disabledReason(selectedShippingVoucher.promotion, target, usage, subtotal);
            if (reason != null) {
                selectedShippingVoucher = null;
                changed = true;
            }
        }

        if (changed) {
            clearSelectedVoucherUi();
            updatePaymentSummary();
            refreshCartUI();
            if (hadProductVoucher || hadShippingVoucher) {
                Toast.makeText(requireContext(), "Voucher đã được hủy vì giỏ hàng không còn đáp ứng điều kiện áp dụng.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void clearSelectedVoucherUi() {
        updateSelectedVoucherUi();
    }

    private void updatePaymentSummary() {
        updateCartSummary();
    }

    private void refreshCartUI() {
        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged();
        }
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
