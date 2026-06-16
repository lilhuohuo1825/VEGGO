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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.adapter.CartAdapter;
import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.checkout.CheckoutGuestActivity;

import java.util.ArrayList;
import java.util.List;

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

        setupCartList();
        seedCartItems();

        layoutVoucher.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_voucher, layoutVoucher));
        layoutPaymentDetail.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_payment_detail, layoutBottomSummaryRow));
        scrimView.setOnClickListener(v -> dismissActivePopup());
        imgCbAll.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        btnCheckout.setOnClickListener(v -> showCheckoutTypeDialog());

        return rootView;
    }

    private void setupCartList() {
        layoutCartItemsContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(cartItems, this);
        layoutCartItemsContainer.setAdapter(cartAdapter);
    }

    private void seedCartItems() {
        if (!cartItems.isEmpty()) {
            return;
        }

        cartItems.add(new CartAdapter.CartItemUiModel(
                getString(R.string.orders_sample_product_name),
                "200gr",
                45000,
                59000,
                R.drawable.ic_vegetable,
                1
        ));
        cartItems.add(new CartAdapter.CartItemUiModel(
                "Ca chua bi huu co",
                "500gr",
                32000,
                39000,
                R.drawable.ic_fruit,
                2
        ));
        cartItems.add(new CartAdapter.CartItemUiModel(
                "Ca chua bi huu co",
                "500gr",
                32000,
                39000,
                R.drawable.ic_fruit,
                2
        ));
        cartItems.add(new CartAdapter.CartItemUiModel(
                getString(R.string.orders_sample_product_name),
                "200gr",
                45000,
                59000,
                R.drawable.ic_vegetable,
                1
        ));

        cartAdapter.notifyDataSetChanged();
        syncAllCheckboxState();
        updateCartSummary();
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
        if (y < rootLocation[1]) {
            y = rootLocation[1];
        }

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

        List<VoucherOptionAdapter.VoucherItemUiModel> vouchers = buildVoucherItems();
        VoucherOptionAdapter.VoucherItemUiModel initialVoucher = vouchers.get(0);
        tvSelectedCount.setText("1 ma da duoc chon");
        tvSelectedTitle.setText(initialVoucher.title);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new VoucherOptionAdapter(vouchers, 0, item -> {
            tvSelectedCount.setText("1 ma da duoc chon");
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
        cartAdapter.notifyItemRangeChanged(0, cartItems.size());
        updateCartSummary();
    }

    private void changeQuantity(int position, int delta) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }
        CartAdapter.CartItemUiModel item = cartItems.get(position);
        int updatedQuantity = item.quantity + delta;
        if (updatedQuantity < MIN_QUANTITY) {
            updatedQuantity = MIN_QUANTITY;
        }
        if (updatedQuantity == item.quantity) {
            return;
        }
        item.quantity = updatedQuantity;
        cartAdapter.notifyItemChanged(position);
        updateCartSummary();
    }

    private void removeCartItem(int position) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }
        cartItems.remove(position);
        cartAdapter.notifyItemRemoved(position);
        syncAllCheckboxState();
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
        int total = 0;
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

    private void showCheckoutTypeDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_checkout_type, null, false);
        TextView btnOpenCheckout = dialogView.findViewById(R.id.btnOpenCheckout);
        TextView btnOpenCheckoutGuest = dialogView.findViewById(R.id.btnOpenCheckoutGuest);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                bottomSheet.setPadding(0, 0, 0, 0);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setGestureInsetBottomIgnored(true);
            }
        });

        btnOpenCheckout.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), CheckoutActivity.class));
        });

        btnOpenCheckoutGuest.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), CheckoutGuestActivity.class));
        });

        dialog.show();
    }

    private void dismissActivePopup() {
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.dismiss();
        } else {
            updatePaymentChevron(false);
            if (scrimView != null) {
                scrimView.setVisibility(View.GONE);
            }
        }
    }

    private void updatePaymentChevron(boolean isPopupShowing) {
        if (imgPaymentChevron != null) {
            imgPaymentChevron.setImageResource(
                    isPopupShowing ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up
            );
        }
    }

    private String formatCurrency(int amount) {
        return String.format(java.util.Locale.US, "%,d", amount).replace(',', '.') + "\u0111";
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

    @Override
    public void onItemCheckedChanged(int position) {
        if (position < 0 || position >= cartItems.size()) {
            return;
        }
        CartAdapter.CartItemUiModel item = cartItems.get(position);
        item.isChecked = !item.isChecked;
        cartAdapter.notifyItemChanged(position);
        syncAllCheckboxState();
        updateCartSummary();
    }

    @Override
    public void onItemRemoved(int position) {
        removeCartItem(position);
    }

    @Override
    public void onItemQuantityChanged(int position, int delta) {
        changeQuantity(position, delta);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setBottomNavVisible(false);
        }
    }

    @Override
    public void onPause() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setBottomNavVisible(true);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        dismissActivePopup();
        imgPaymentChevron = null;
        imgCbAll = null;
        layoutCartItemsContainer = null;
        cartAdapter = null;
        scrimView = null;
        tvCartTitle = null;
        tvCartTotal = null;
        btnCheckout = null;
        rootView = null;
        super.onDestroyView();
    }
}
