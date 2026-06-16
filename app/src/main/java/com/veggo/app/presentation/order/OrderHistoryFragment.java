package com.veggo.app.presentation.order;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.profile.OrderHistoryFragmentExtras;

import java.util.List;

public class OrderHistoryFragment extends BaseFragment {
    private static final String ARG_SHOW_BACK_BUTTON = "arg_show_back_button";

    private AssetScreenData.Snapshot snapshot;
    private LinearLayout orderListContainer;
    private ScrollView orderListScroll;
    private HorizontalScrollView orderStatusScroll;
    private View orderEmptyState;
    private String initialStatus;
    private String currentStatus;

    public static OrderHistoryFragment newInstance(boolean showBackButton) {
        OrderHistoryFragment fragment = new OrderHistoryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_BACK_BUTTON, showBackButton);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_order_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backButton = view.findViewById(R.id.orderHistoryBackButton);
        boolean showBackButton = getArguments() == null
                || getArguments().getBoolean(ARG_SHOW_BACK_BUTTON, true);
        applyNavbarTopInsetIfNeeded(view, showBackButton);
        backButton.setVisibility(showBackButton ? View.VISIBLE : View.INVISIBLE);
        backButton.setEnabled(showBackButton);
        backButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        view.findViewById(R.id.orderHistoryMenuButton).setOnClickListener(v -> AssetScreenData.showOrderOptions(requireContext()));
        orderListContainer = view.findViewById(R.id.orderListContainer);
        orderListScroll = view.findViewById(R.id.orderListScroll);
        orderStatusScroll = view.findViewById(R.id.orderStatusScroll);
        orderEmptyState = view.findViewById(R.id.orderEmptyState);
        view.findViewById(R.id.orderEmptyShopButton).setOnClickListener(v -> openShopping());
        view.findViewById(R.id.orderTabAll).setOnClickListener(v -> showOrders(null));
        view.findViewById(R.id.orderTabPending).setOnClickListener(v -> showOrders("pending"));
        view.findViewById(R.id.orderTabShipping).setOnClickListener(v -> showOrders("shipping"));
        view.findViewById(R.id.orderTabDelivered).setOnClickListener(v -> showOrders("delivered"));
        view.findViewById(R.id.orderTabCancelled).setOnClickListener(v -> showOrders("cancelled"));
        initialStatus = requireActivity().getIntent()
                .getStringExtra(OrderHistoryFragmentExtras.EXTRA_INITIAL_STATUS);
        currentStatus = initialStatus;
    }

    private void applyNavbarTopInsetIfNeeded(@NonNull View root, boolean showBackButton) {
        if (showBackButton) {
            return;
        }
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    topInset = Math.max(topInset, cutout.getSafeInsetTop());
                }
            }
            view.setPadding(initialLeft, initialTop + topInset, initialRight, initialBottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        new Thread(() -> {
            AssetScreenData.Snapshot loaded = AssetScreenData.load(requireContext());
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                snapshot = loaded;
                showOrders(currentStatus);
            });
        }).start();
    }

    private void showOrders(String status) {
        currentStatus = status;
        if (snapshot == null || orderListContainer == null) {
            return;
        }
        updateSelectedTab(status);
        List<AssetModels.Order> orders = AssetScreenData.filterOrders(snapshot, status);
        orderListContainer.removeAllViews();
        if (orders.isEmpty()) {
            showEmptyState(true);
            return;
        }
        showEmptyState(false);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AssetModels.Order order : orders) {
            View card = inflater.inflate(layoutForStatus(order.status), orderListContainer, false);
            AssetScreenData.bindOrderCard(requireContext(), card, snapshot, order);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            orderListContainer.addView(card);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (orderListScroll != null) {
            orderListScroll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (orderEmptyState != null) {
            orderEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSelectedTab(@Nullable String status) {
        setTabSelected(
                R.id.orderTabAllText,
                R.id.orderTabAllBadge,
                R.id.orderTabAllIndicator,
                status == null,
                AssetScreenData.filterOrders(snapshot, null).size(),
                R.id.orderTabAll
        );
        setTabSelected(
                R.id.orderTabPendingText,
                R.id.orderTabPendingBadge,
                R.id.orderTabPendingIndicator,
                "pending".equals(status),
                AssetScreenData.filterOrders(snapshot, "pending").size(),
                R.id.orderTabPending
        );
        setTabSelected(
                R.id.orderTabShippingText,
                R.id.orderTabShippingBadge,
                R.id.orderTabShippingIndicator,
                "shipping".equals(status),
                AssetScreenData.filterOrders(snapshot, "shipping").size(),
                R.id.orderTabShipping
        );
        setTabSelected(
                R.id.orderTabDeliveredText,
                R.id.orderTabDeliveredBadge,
                R.id.orderTabDeliveredIndicator,
                "delivered".equals(status),
                AssetScreenData.filterOrders(snapshot, "delivered").size(),
                R.id.orderTabDelivered
        );
        setTabSelected(
                R.id.orderTabCancelledText,
                R.id.orderTabCancelledBadge,
                R.id.orderTabCancelledIndicator,
                "cancelled".equals(status),
                AssetScreenData.filterOrders(snapshot, "cancelled").size(),
                R.id.orderTabCancelled
        );
    }

    private void setTabSelected(int textId, int badgeId, int indicatorId, boolean selected, int count, int tabId) {
        View root = getView();
        if (root == null) {
            return;
        }
        TextView text = root.findViewById(textId);
        TextView badge = root.findViewById(badgeId);
        View indicator = root.findViewById(indicatorId);
        View tab = root.findViewById(tabId);

        if (text != null) {
            text.setTextColor(requireContext().getColor(selected ? R.color.primary_main : R.color.neutral_60));
            text.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (badge != null) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            badge.setBackgroundResource(selected
                    ? R.drawable.bg_notification_badge_alert
                    : R.drawable.bg_notification_badge_dark);
        }
        if (indicator != null) {
            indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }

        if (selected && tab != null && orderStatusScroll != null) {
            orderStatusScroll.post(() -> {
                int scrollX = tab.getLeft() - (orderStatusScroll.getWidth() - tab.getWidth()) / 2;
                orderStatusScroll.smoothScrollTo(scrollX, 0);
            });
        }
    }

    private void openShopping() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_home);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private int layoutForStatus(String status) {
        if ("pending".equals(status)) {
            return R.layout.item_order_pending;
        }
        if ("shipping".equals(status)) {
            return R.layout.item_order_shipping;
        }
        if ("delivered".equals(status) || "completed".equals(status) || "returned".equals(status)) {
            return R.layout.item_order_delivered;
        }
        return R.layout.item_order_cancelled;
    }


}
