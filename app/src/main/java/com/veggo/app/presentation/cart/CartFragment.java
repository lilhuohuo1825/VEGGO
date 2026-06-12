package com.veggo.app.presentation.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.drawable.ColorDrawable;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.View.MeasureSpec;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.checkout.CheckoutGuestActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartFragment extends BaseFragment {
    private static final int MIN_QUANTITY = 1;

    private final List<CartItemHolder> cartItems = new ArrayList<>();
    private View rootView;
    private PopupWindow activePopup;
    private View scrimView;
    private ImageView imgPaymentChevron;
    private ImageView imgCbAll;
    private LinearLayout layoutCartItemsContainer;
    private boolean isAllChecked = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cart, container, false);
        scrimView = rootView.findViewById(R.id.viewScrim);
        layoutCartItemsContainer = rootView.findViewById(R.id.layoutCartItemsContainer);

        View layoutVoucher = rootView.findViewById(R.id.layoutVoucher);
        View layoutPaymentDetail = rootView.findViewById(R.id.layoutPaymentDetail);
        View layoutBottomSummaryRow = rootView.findViewById(R.id.layoutBottomSummaryRow);
        View btnCheckout = rootView.findViewById(R.id.btnCheckout);
        imgCbAll = rootView.findViewById(R.id.imgCbAll);
        imgPaymentChevron = rootView.findViewById(R.id.imgPaymentChevron);

        bindCartItem(rootView.findViewById(R.id.itemCart1));
        bindCartItem(rootView.findViewById(R.id.itemCart2));
        bindCartItem(rootView.findViewById(R.id.itemCart3));
        bindCartItem(rootView.findViewById(R.id.itemCart4));

        layoutVoucher.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_voucher, layoutVoucher));
        layoutPaymentDetail.setOnClickListener(v -> showPopupAboveAnchor(R.layout.dialog_payment_detail, layoutBottomSummaryRow));
        scrimView.setOnClickListener(v -> dismissActivePopup());
        imgCbAll.setOnClickListener(v -> toggleAllCheckbox(imgCbAll));
        btnCheckout.setOnClickListener(v -> showCheckoutTypeDialog());

        return rootView;
    }

    private void showPopupAboveAnchor(int layoutResId, View anchorView) {
        dismissActivePopup();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(layoutResId, null, false);

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

    private void toggleAllCheckbox(ImageView imageView) {
        isAllChecked = !isAllChecked;
        updateCheckboxIcon(imageView, isAllChecked);

        for (CartItemHolder item : cartItems) {
            item.isChecked = isAllChecked;
            updateCheckboxIcon(item.checkboxView, item.isChecked);
        }
    }

    private void bindCartItem(View itemView) {
        if (itemView == null) {
            return;
        }

        CartItemHolder holder = new CartItemHolder(itemView);
        cartItems.add(holder);

        holder.checkboxView.setOnClickListener(v -> {
            holder.isChecked = !holder.isChecked;
            updateCheckboxIcon(holder.checkboxView, holder.isChecked);
            syncAllCheckboxState();
        });

        holder.deleteView.setOnClickListener(v -> removeCartItem(holder));
        holder.decreaseView.setOnClickListener(v -> changeQuantity(holder, -1));
        holder.increaseView.setOnClickListener(v -> changeQuantity(holder, 1));

        updateCheckboxIcon(holder.checkboxView, holder.isChecked);
        holder.quantityView.setText(String.valueOf(holder.quantity));
    }

    private void removeCartItem(CartItemHolder holder) {
        if (layoutCartItemsContainer != null) {
            layoutCartItemsContainer.removeView(holder.rootView);
        }
        Iterator<CartItemHolder> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == holder) {
                iterator.remove();
                break;
            }
        }
        syncAllCheckboxState();
    }

    private void changeQuantity(CartItemHolder holder, int delta) {
        int updatedQuantity = holder.quantity + delta;
        if (updatedQuantity < MIN_QUANTITY) {
            updatedQuantity = MIN_QUANTITY;
        }
        holder.quantity = updatedQuantity;
        holder.quantityView.setText(String.valueOf(holder.quantity));
    }

    private void syncAllCheckboxState() {
        boolean areAllChecked = !cartItems.isEmpty();
        for (CartItemHolder item : cartItems) {
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

    @Override
    public void onDestroyView() {
        dismissActivePopup();
        imgPaymentChevron = null;
        imgCbAll = null;
        layoutCartItemsContainer = null;
        cartItems.clear();
        scrimView = null;
        rootView = null;
        super.onDestroyView();
    }

    private static final class CartItemHolder {
        private final View rootView;
        private final ImageView checkboxView;
        private final ImageView deleteView;
        private final TextView decreaseView;
        private final TextView quantityView;
        private final TextView increaseView;
        private boolean isChecked = true;
        private int quantity = 1;

        private CartItemHolder(View rootView) {
            this.rootView = rootView;
            checkboxView = rootView.findViewById(R.id.imgItemCheckbox);
            deleteView = rootView.findViewById(R.id.imgDeleteCartItem);
            decreaseView = rootView.findViewById(R.id.tvDecreaseQuantity);
            quantityView = rootView.findViewById(R.id.tvQuantity);
            increaseView = rootView.findViewById(R.id.tvIncreaseQuantity);
        }
    }
}
