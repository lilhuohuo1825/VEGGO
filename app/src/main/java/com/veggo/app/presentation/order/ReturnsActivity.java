package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.CurvedTabIndicatorHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReturnsActivity extends BaseActivity {
    private TextView pendingTab;
    private TextView processingTab;
    private TextView completedTab;
    private TextView rejectedTab;
    private TextView pendingBadge;
    private TextView processingBadge;
    private TextView completedBadge;
    private TextView rejectedBadge;
    private View pendingIndicator;
    private View processingIndicator;
    private View completedIndicator;
    private View rejectedIndicator;
    private CurvedTabIndicatorHelper tabIndicator;
    private View pendingList;
    private View processingList;
    private View completedList;
    private View rejectedList;
    private View returnsEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView returnListScroll;
    private List<AssetModels.Order> pendingOrders;
    private List<AssetModels.Order> processingOrders;
    private List<AssetModels.Order> completedOrders;
    private List<AssetModels.Order> rejectedOrders;
    private android.widget.HorizontalScrollView returnsStatusScroll;
    private AssetScreenData.Snapshot snapshot;
    private String searchQuery = "";
    private String selectedBucket = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_returns);

        findViewById(R.id.returnsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.returnsMenuButton).setOnClickListener(v -> AssetScreenData.showOrderOptions(this));
        pendingTab = findViewById(R.id.returnPendingTabText);
        processingTab = findViewById(R.id.returnProcessingTabText);
        completedTab = findViewById(R.id.returnCompletedTabText);
        rejectedTab = findViewById(R.id.returnRejectedTabText);
        pendingBadge = findViewById(R.id.returnPendingTabBadge);
        processingBadge = findViewById(R.id.returnProcessingTabBadge);
        completedBadge = findViewById(R.id.returnCompletedTabBadge);
        rejectedBadge = findViewById(R.id.returnRejectedTabBadge);
        pendingIndicator = findViewById(R.id.returnPendingIndicator);
        processingIndicator = findViewById(R.id.returnProcessingIndicator);
        completedIndicator = findViewById(R.id.returnCompletedIndicator);
        rejectedIndicator = findViewById(R.id.returnRejectedIndicator);
        returnsStatusScroll = findViewById(R.id.returnsStatusScroll);
        tabIndicator = CurvedTabIndicatorHelper.attach(
                returnsStatusScroll,
                new CurvedTabIndicatorHelper.TabItem(findViewById(R.id.returnPendingTab), pendingIndicator),
                new CurvedTabIndicatorHelper.TabItem(findViewById(R.id.returnProcessingTab), processingIndicator),
                new CurvedTabIndicatorHelper.TabItem(findViewById(R.id.returnCompletedTab), completedIndicator),
                new CurvedTabIndicatorHelper.TabItem(findViewById(R.id.returnRejectedTab), rejectedIndicator)
        );
        pendingList = findViewById(R.id.returnPendingList);
        processingList = findViewById(R.id.returnProcessingList);
        completedList = findViewById(R.id.returnCompletedList);
        rejectedList = findViewById(R.id.returnRejectedList);
        returnsEmptyState = findViewById(R.id.returnsEmptyState);
        returnListScroll = findViewById(R.id.returnListScroll);
        swipeRefreshLayout = findViewById(R.id.returnsSwipeRefresh);
        if (swipeRefreshLayout != null) {
            PullToRefreshHelper.bind(swipeRefreshLayout, returnListScroll, () -> loadReturns(true));
        }

        findViewById(R.id.returnPendingTab).setOnClickListener(v -> showReturnState("pending"));
        findViewById(R.id.returnProcessingTab).setOnClickListener(v -> showReturnState("processing"));
        findViewById(R.id.returnCompletedTab).setOnClickListener(v -> showReturnState("completed"));
        findViewById(R.id.returnRejectedTab).setOnClickListener(v -> showReturnState("rejected"));
        EditText searchInput = findViewById(R.id.returnSearchInput);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim();
                    if (snapshot != null) {
                        bindReturnLists(snapshot);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        loadReturns();
    }

    private void showReturnState(String bucket) {
        selectedBucket = bucket;
        int index = 0;
        TextView activeTab = pendingTab;
        View activeList = pendingList;
        if ("processing".equals(bucket)) {
            index = 1;
            activeTab = processingTab;
            activeList = processingList;
        } else if ("completed".equals(bucket)) {
            index = 2;
            activeTab = completedTab;
            activeList = completedList;
        } else if ("rejected".equals(bucket)) {
            index = 3;
            activeTab = rejectedTab;
            activeList = rejectedList;
        }
        showReturnState(index, activeTab, activeList);
    }

    private void showReturnState(int index, TextView activeTab, View activeList) {
        setActive(pendingTab, pendingBadge, pendingTab == activeTab);
        setActive(processingTab, processingBadge, processingTab == activeTab);
        setActive(completedTab, completedBadge, completedTab == activeTab);
        setActive(rejectedTab, rejectedBadge, rejectedTab == activeTab);
        if (tabIndicator != null) {
            tabIndicator.selectTab(index);
        }

        boolean isEmpty = false;
        if (activeList == pendingList) {
            isEmpty = pendingOrders == null || pendingOrders.isEmpty();
        } else if (activeList == processingList) {
            isEmpty = processingOrders == null || processingOrders.isEmpty();
        } else if (activeList == completedList) {
            isEmpty = completedOrders == null || completedOrders.isEmpty();
        } else if (activeList == rejectedList) {
            isEmpty = rejectedOrders == null || rejectedOrders.isEmpty();
        }

        pendingList.setVisibility(pendingList == activeList && !isEmpty ? View.VISIBLE : View.GONE);
        processingList.setVisibility(processingList == activeList && !isEmpty ? View.VISIBLE : View.GONE);
        completedList.setVisibility(completedList == activeList && !isEmpty ? View.VISIBLE : View.GONE);
        rejectedList.setVisibility(rejectedList == activeList && !isEmpty ? View.VISIBLE : View.GONE);

        if (returnsEmptyState != null) {
            returnsEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (returnsStatusScroll != null) {
            int tabId = R.id.returnPendingTab;
            if (activeTab == processingTab) tabId = R.id.returnProcessingTab;
            else if (activeTab == completedTab) tabId = R.id.returnCompletedTab;
            else if (activeTab == rejectedTab) tabId = R.id.returnRejectedTab;
            View tab = findViewById(tabId);
            if (tab != null) {
                returnsStatusScroll.post(() -> {
                    int scrollX = tab.getLeft() - (returnsStatusScroll.getWidth() - tab.getWidth()) / 2;
                    returnsStatusScroll.smoothScrollTo(scrollX, 0);
                });
            }
        }
    }


    private void setActive(TextView textView, TextView badge, boolean active) {
        int colorRes = active ? R.color.primary_main : R.color.neutral_60;
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
        textView.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        if (badge != null) {
            BadgeUiHelper.styleTabBadge(badge, active);
        }
    }

    private void loadReturns() {
        loadReturns(false);
    }

    private void loadReturns(boolean fromSwipeRefresh) {
        new Thread(() -> {
            try {
                AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
                runOnUiThread(() -> {
                    bindReturnLists(snapshot);
                    if (fromSwipeRefresh && swipeRefreshLayout != null) {
                        PullToRefreshHelper.finish(swipeRefreshLayout);
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (fromSwipeRefresh && swipeRefreshLayout != null) {
                        PullToRefreshHelper.finish(swipeRefreshLayout);
                    }
                    Toast.makeText(this, "Không thể tải danh sách đổi trả", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void bindReturnLists(AssetScreenData.Snapshot snapshot) {
        this.snapshot = snapshot;
        pendingOrders = filterOrdersByQuery(AssetScreenData.filterReturnOrders(snapshot, "pending"));
        processingOrders = filterOrdersByQuery(AssetScreenData.filterReturnOrders(snapshot, "processing"));
        completedOrders = filterOrdersByQuery(AssetScreenData.filterReturnOrders(snapshot, "completed"));
        rejectedOrders = filterOrdersByQuery(AssetScreenData.filterReturnOrders(snapshot, "rejected"));
        setBadgeCount(pendingBadge, pendingOrders.size());
        setBadgeCount(processingBadge, processingOrders.size());
        setBadgeCount(completedBadge, completedOrders.size());
        setBadgeCount(rejectedBadge, rejectedOrders.size());
        bindList((LinearLayout) pendingList, pendingOrders, R.layout.item_return_pending, snapshot);
        bindList((LinearLayout) processingList, processingOrders, R.layout.item_return_processing, snapshot);
        bindList((LinearLayout) completedList, completedOrders, R.layout.item_return_completed, snapshot);
        bindList((LinearLayout) rejectedList, rejectedOrders, R.layout.item_return_rejected, snapshot);
        showReturnState(selectedBucket);
    }

    private List<AssetModels.Order> filterOrdersByQuery(List<AssetModels.Order> source) {
        String normalizedQuery = searchQuery == null
                ? ""
                : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty() || snapshot == null) {
            return source;
        }

        List<AssetModels.Order> filtered = new ArrayList<>();
        for (AssetModels.Order order : source) {
            if (matchesSearch(order, normalizedQuery)) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    private boolean matchesSearch(AssetModels.Order order, String normalizedQuery) {
        StringBuilder searchable = new StringBuilder();
        appendSearchable(searchable, order.orderId);
        appendSearchable(searchable, AssetScreenData.statusLabel(order.status));
        appendSearchable(searchable, AssetScreenData.date(order.createdAt));

        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
        if (detail != null && detail.items != null) {
            for (AssetModels.OrderDetailItem item : detail.items) {
                appendSearchable(searchable, item.productName);
                appendSearchable(searchable, item.sku);
            }
        }
        return searchable.toString().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void appendSearchable(StringBuilder builder, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(' ').append(value);
        }
    }

    private void setBadgeCount(TextView badge, int count) {
        if (badge == null) {
            return;
        }
        badge.setText(String.valueOf(count));
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void bindList(
            LinearLayout container,
            List<AssetModels.Order> orders,
            int layout,
            AssetScreenData.Snapshot snapshot
    ) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.Order order : orders) {
            View item = inflater.inflate(layout, container, false);
            AssetScreenData.bindOrderCard(this, item, snapshot, order);
            bindRejectedReason(item, order);
            bindActions(item, order, layout);
            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            container.addView(item);
        }
    }

    private void bindRejectedReason(View item, AssetModels.Order order) {
        TextView rejectReasonText = item.findViewById(R.id.returnRejectReasonText);
        if (rejectReasonText == null) {
            return;
        }
        String reason = order.rejectReason == null ? "" : order.rejectReason.trim();
        rejectReasonText.setText(reason.isEmpty()
                ? getString(R.string.returns_rejected_reason)
                : reason);
    }

    private void bindActions(View item, AssetModels.Order order, int layout) {
        TextView cancelRequestButton = item.findViewById(R.id.returnCancelRequestButton);
        if (cancelRequestButton != null && layout == R.layout.item_return_pending) {
            cancelRequestButton.setOnClickListener(v -> confirmCancelReturnRequest(order));
        }

        TextView buyAgainButton = item.findViewById(R.id.returnBuyAgainButton);
        if (buyAgainButton != null && (layout == R.layout.item_return_completed || layout == R.layout.item_return_rejected)) {
            buyAgainButton.setOnClickListener(v -> {
                v.setEnabled(false);
                buyAgainOrder(order, v);
            });
        }
    }

    private void confirmCancelReturnRequest(AssetModels.Order order) {
        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                "Hủy yêu cầu đổi/trả",
                "Bạn có chắc chắn muốn hủy yêu cầu đổi/trả cho đơn hàng này?",
                "Xác nhận",
                "Đóng",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        updateOrderStatus(order, "delivered", "Đã hủy yêu cầu đổi/trả", "pending");
                    }
                }
        );
    }

    private void confirmReturnCompleted(AssetModels.Order order) {
        // Deprecated: confirmation moved to admin side.
    }

    private void updateOrderStatus(AssetModels.Order order, String newStatus) {
        updateOrderStatus(order, newStatus, "Đã xác nhận hoàn/trả thành công", "completed");
    }

    private void updateOrderStatus(AssetModels.Order order, String newStatus, String toastMessage, String nextBucket) {
        new Thread(() -> {
            boolean success = false;
            String errorMessage = "Không thể cập nhật trạng thái đơn hàng";
            try {
                OrderApi orderApi = ApiClient.createService(OrderApi.class);
                Map<String, String> body = new HashMap<>();
                body.put("status", newStatus);
                retrofit2.Response<Map<String, Object>> response =
                        orderApi.updateOrderStatus(order.orderId, body).execute();
                success = response.isSuccessful();
                if (!success && response.errorBody() != null) {
                    errorMessage = response.errorBody().string();
                }
            } catch (Exception exception) {
                errorMessage = exception.getMessage() == null ? errorMessage : exception.getMessage();
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    android.widget.Toast.makeText(this, toastMessage, android.widget.Toast.LENGTH_SHORT).show();
                    selectedBucket = nextBucket;
                    loadReturns();
                } else {
                    android.widget.Toast.makeText(this, finalErrorMessage, android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void openOrderDetail(String orderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
    }

    private void buyAgainOrder(AssetModels.Order order, View sourceButton) {
        AssetModels.OrderDetail detail = snapshot == null ? null : snapshot.detailByOrderId.get(order.orderId);
        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            if (sourceButton != null) sourceButton.setEnabled(true);
            android.widget.Toast.makeText(this, "Không có sản phẩm nào để mua lại", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            if (sourceButton != null) sourceButton.setEnabled(true);
            android.widget.Toast.makeText(this, "Không tìm thấy thông tin người dùng", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            boolean success = true;
            String errorMessage = "Không thể thêm sản phẩm vào giỏ";
            try {
                CartApi cartApi = ApiClient.createService(CartApi.class);
                for (AssetModels.OrderDetailItem item : detail.items) {
                    String sku = item.sku == null ? "" : item.sku.trim();
                    if (sku.isEmpty()) continue;
                    retrofit2.Response<?> response = cartApi.addItem(
                            customerId,
                            new CartItemRequestDto(sku, Math.max(1, item.quantity), 1.0)
                    ).execute();
                    if (!response.isSuccessful()) {
                        success = false;
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                        break;
                    }
                }
            } catch (Exception exception) {
                success = false;
                errorMessage = exception.getMessage() == null ? errorMessage : exception.getMessage();
            }
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                if (sourceButton != null) sourceButton.setEnabled(true);
                if (finalSuccess) {
                    openCart(detail);
                } else {
                    android.widget.Toast.makeText(this, finalErrorMessage, android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void openCart(AssetModels.OrderDetail detail) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_cart);
        ArrayList<String> selectedSkus = new ArrayList<>();
        if (detail.items != null) {
            for (AssetModels.OrderDetailItem item : detail.items) {
                if (item.sku != null && !item.sku.trim().isEmpty()) {
                    selectedSkus.add(item.sku.trim());
                }
            }
        }
        intent.putStringArrayListExtra(CartFragment.ARG_SELECTED_SKUS, selectedSkus);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
