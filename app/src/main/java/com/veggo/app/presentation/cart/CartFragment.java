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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.CartAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.checkout.CheckoutActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends BaseFragment implements CartAdapter.CartItemActionListener {
    private static final int MIN_QUANTITY = 1;

    private final List<CartAdapter.CartItemUiModel> cartItems = new ArrayList<>();
    private View rootView;
    private PopupWindow activePopup;
    private View scrimView;
    private ImageView imgPaymentChevron;
    private ImageView imgCbAll;
    private RecyclerView layoutCartItemsContainer;
    private CartAdapter cartAdapter;
    private TextView tvCartTitle;
    private TextView tvCartTotal;
    private TextView btnCheckout;
    private boolean isAllChecked = true;
    private CartViewModel viewModel;
    private String customerId;

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
        imgCbAll = rootView.findViewById(R.id.imgCbAll);
        imgPaymentChevron = rootView.findViewById(R.id.imgPaymentChevron);

        customerId = resolveCustomerId();
        
        android.util.Log.d("VEGGO_DEBUG", "CartFragment is using CustomerId: " + customerId);
        
        setupViewModel();
        setupCartList();
        observeViewModel();
        
        viewModel.fetchCart(customerId);

        layoutVoucher.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_voucher, layoutVoucher));
        layoutPaymentDetail.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_payment_detail, layoutBottomSummaryRow));
        scrimView.setOnClickListener(v -> dismissActivePopup());
        imgCbAll.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            startActivity(intent);
        });

        return rootView;
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
        // Lưu lại trạng thái checkbox hiện tại theo SKU
        java.util.Map<String, Boolean> checkStates = new java.util.HashMap<>();
        for (CartAdapter.CartItemUiModel item : cartItems) {
            checkStates.put(item.sku, item.isChecked);
        }

        cartItems.clear();
        if (cartDto.getItems() != null) {
            for (CartDto.CartItemDto itemDto : cartDto.getItems()) {
                ProductDto product = itemDto.getProduct();
                String sku = itemDto.getSku();

                String name = "Sản phẩm VEGGO";
                String imageUrl = "";
                long price = 0;
                double carbonPoint = 0;

                if (product != null) {
                    if (product.getProductName() != null && !product.getProductName().isEmpty()) {
                        name = product.getProductName();
                    }
                    imageUrl = product.getFirstImage();
                    price = product.getPrice();
                    carbonPoint = product.getCarbonSavingPoint();
                }

                CartAdapter.CartItemUiModel uiModel = new CartAdapter.CartItemUiModel(
                        sku,
                        name,
                        formatWeight(itemDto.getSelectedWeight()),
                        carbonPoint,
                        price,
                        price,
                        imageUrl,
                        itemDto.getQuantity()
                );

                // Khôi phục trạng thái checkbox nếu SKU này đã có trước đó
                if (checkStates.containsKey(sku)) {
                    uiModel.isChecked = Boolean.TRUE.equals(checkStates.get(sku));
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
            return String.format(Locale.US, "%.0fkg", weight);
        } else {
            return String.format(Locale.US, "%.0fg", weight * 1000);
        }
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
            String sku = cartItems.get(position).sku;
            viewModel.removeItem(customerId, sku);
        }
    }

    @Override
    public void onItemQuantityChanged(int position, int delta) {
        if (position >= 0 && position < cartItems.size()) {
            CartAdapter.CartItemUiModel item = cartItems.get(position);
            int newQty = item.quantity + delta;
            if (newQty >= MIN_QUANTITY) {
                viewModel.updateQuantity(customerId, item.sku, newQty);
            }
        }
    }

    private void showPopupAboveAnchor(int layoutResId, View anchorView) {
        dismissActivePopup();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(layoutResId, null, false);
        if (layoutResId == R.layout.dialog_voucher) {
            setupVoucherPopup(dialogView);
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
            int scrimHeight = Math.max(0, anchorLocation[1] - rootLocation[1]);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrimView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = scrimHeight;
            params.gravity = Gravity.TOP;
            scrimView.setLayoutParams(params);
            scrimView.setVisibility(View.VISIBLE);
        }
        updatePaymentChevron(layoutResId == R.layout.dialog_payment_detail);
        activePopup.showAtLocation(rootView, Gravity.TOP | Gravity.START, x, y);
    }

    private void setupVoucherPopup(View dialogView) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvVoucherOptions);
        TextView tvSelectedCount = dialogView.findViewById(R.id.tvVoucherSelectedCount);
        TextView tvSelectedTitle = dialogView.findViewById(R.id.tvVoucherSelectedTitle);
        TextView btnApplyVoucher = dialogView.findViewById(R.id.btnApplyVoucher);

        // Dummy vouchers for UI
        List<VoucherOptionAdapter.VoucherItemUiModel> vouchers = new ArrayList<>();
        vouchers.add(new VoucherOptionAdapter.VoucherItemUiModel("V1", "Giảm 10k cho đơn từ 100k", "HSD: 30/06/2024", R.drawable.ic_voucher));
        vouchers.add(new VoucherOptionAdapter.VoucherItemUiModel("V2", "Miễn phí vận chuyển", "HSD: 15/07/2024", R.drawable.ic_voucher));

        tvSelectedCount.setText("1 mã đã được chọn");
        tvSelectedTitle.setText(vouchers.get(0).title);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new VoucherOptionAdapter(vouchers, 0, item -> {
            tvSelectedCount.setText("1 mã đã được chọn");
            tvSelectedTitle.setText(item.title);
        }));

        btnApplyVoucher.setOnClickListener(v -> dismissActivePopup());
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
        int selectedCount = 0;
        long total = 0;
        for (CartAdapter.CartItemUiModel item : cartItems) {
            if (item.isChecked) {
                selectedCount++;
                total += item.getLineTotal();
            }
        }

        if (tvCartTitle != null) {
            tvCartTitle.setText(getString(R.string.cart_title_format, cartItems.size()));
        }
        if (tvCartTotal != null) {
            tvCartTotal.setText(formatCurrency(total));
        }
        if (btnCheckout != null) {
            btnCheckout.setText(getString(R.string.cart_checkout_format, selectedCount));
        }
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
            resolvedCustomerId = "CUS900025";
        }
        return resolvedCustomerId;
    }
}
