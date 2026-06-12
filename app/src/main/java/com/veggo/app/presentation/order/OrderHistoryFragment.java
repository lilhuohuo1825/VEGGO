package com.veggo.app.presentation.order;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private AssetScreenData.Snapshot snapshot;
    private LinearLayout orderListContainer;
    private ScrollView orderListScroll;
    private View orderEmptyState;
    private String initialStatus;

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

        view.findViewById(R.id.orderHistoryBackButton).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        view.findViewById(R.id.orderHistoryMenuButton).setOnClickListener(v -> showOrderOptions());
        orderListContainer = view.findViewById(R.id.orderListContainer);
        orderListScroll = view.findViewById(R.id.orderListScroll);
        orderEmptyState = view.findViewById(R.id.orderEmptyState);
        view.findViewById(R.id.orderEmptyShopButton).setOnClickListener(v -> openShopping());
        view.findViewById(R.id.orderTabAll).setOnClickListener(v -> showOrders(null));
        view.findViewById(R.id.orderTabPending).setOnClickListener(v -> showOrders("pending"));
        view.findViewById(R.id.orderTabShipping).setOnClickListener(v -> showOrders("shipping"));
        view.findViewById(R.id.orderTabDelivered).setOnClickListener(v -> showOrders("delivered"));
        view.findViewById(R.id.orderTabCancelled).setOnClickListener(v -> showOrders("cancelled"));
        initialStatus = requireActivity().getIntent()
                .getStringExtra(OrderHistoryFragmentExtras.EXTRA_INITIAL_STATUS);
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
                showOrders(initialStatus);
            });
        }).start();
    }

    private void showOrders(String status) {
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
                R.id.orderTabAllIndicator,
                status == null
        );
        setTabSelected(
                R.id.orderTabPendingText,
                R.id.orderTabPendingIndicator,
                "pending".equals(status)
        );
        setTabSelected(
                R.id.orderTabShippingText,
                R.id.orderTabShippingIndicator,
                "shipping".equals(status)
        );
        setTabSelected(
                R.id.orderTabDeliveredText,
                R.id.orderTabDeliveredIndicator,
                "delivered".equals(status)
        );
        setTabSelected(
                R.id.orderTabCancelledText,
                R.id.orderTabCancelledIndicator,
                "cancelled".equals(status)
        );
    }

    private void setTabSelected(int textId, int indicatorId, boolean selected) {
        View root = getView();
        if (root == null) {
            return;
        }
        TextView text = root.findViewById(textId);
        View indicator = root.findViewById(indicatorId);
        if (text != null) {
            text.setTextColor(requireContext().getColor(selected ? R.color.primary_main : R.color.neutral_60));
            text.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (indicator != null) {
            indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
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

    private void showOrderOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_order_history_options, null, false);
        sheet.findViewById(R.id.orderOptionRecurring).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), RecurringOrdersActivity.class));
        });
        sheet.findViewById(R.id.orderOptionReviews).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), ReviewsActivity.class));
        });
        sheet.findViewById(R.id.orderOptionReturns).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), ReturnsActivity.class));
        });
        dialog.setContentView(sheet);
        dialog.show();
    }
}
