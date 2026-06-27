package com.veggo.app.presentation.order;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.data.remote.dto.OrderDto;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.profile.OrderHistoryFragmentExtras;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryFragment extends BaseFragment {
    private static final String ARG_SHOW_BACK_BUTTON = "arg_show_back_button";

    private AssetScreenData.Snapshot snapshot;
    private LinearLayout orderListContainer;
    private ScrollView orderListScroll;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HorizontalScrollView orderStatusScroll;
    private View orderEmptyState;
    private String initialStatus;
    private String currentStatus;
    private String searchQuery = "";
    private String pendingStatusAfterLoad;

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
        swipeRefreshLayout = view.findViewById(R.id.orderHistorySwipeRefresh);
        orderStatusScroll = view.findViewById(R.id.orderStatusScroll);
        orderEmptyState = view.findViewById(R.id.orderEmptyState);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.primary_main, R.color.primary_hover);
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) ->
                    orderListScroll != null && orderListScroll.canScrollVertically(-1));
            swipeRefreshLayout.setOnRefreshListener(() -> loadOrders(true));
        }
        EditText orderSearchInput = view.findViewById(R.id.orderSearchInput);
        if (orderSearchInput != null) {
            orderSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim();
                    showOrders(currentStatus);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            orderSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && !new AppPreferences(requireContext()).isLoggedIn()) {
                    showGuestOrderPhoneDialog(orderSearchInput.getText().toString().trim());
                    return true;
                }
                return false;
            });
        }
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
        loadOrders(false);
    }

    private void loadOrders() {
        loadOrders(false);
    }

    private void loadOrders(boolean fromSwipeRefresh) {
        new Thread(() -> {
            AssetScreenData.Snapshot loaded = AssetScreenData.load(requireContext());
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                snapshot = loaded;
                String statusToShow = pendingStatusAfterLoad != null ? pendingStatusAfterLoad : currentStatus;
                pendingStatusAfterLoad = null;
                showOrders(statusToShow);
                if (fromSwipeRefresh && swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }).start();
    }

    private void showOrders(String status) {
        currentStatus = status;
        if (snapshot == null || orderListContainer == null) {
            return;
        }
        updateSelectedTab(status);
        List<AssetModels.Order> orders = filterOrdersByQuery(
                AssetScreenData.filterOrders(snapshot, status)
        );
        orderListContainer.removeAllViews();
        if (orders.isEmpty()) {
            showEmptyState(true);
            return;
        }
        showEmptyState(false);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int itemSpacing = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        for (int index = 0; index < orders.size(); index++) {
            AssetModels.Order order = orders.get(index);
            View card = inflater.inflate(layoutForStatus(order.status), orderListContainer, false);
            AssetScreenData.bindOrderCard(requireContext(), card, snapshot, order);
            bindCommonCardActions(card, order);
            bindDeliveredCardActions(card, order);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = index == 0 ? 0 : itemSpacing;
            orderListContainer.addView(card, params);
        }
    }

    private void showGuestOrderPhoneDialog(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập mã đơn guest", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText phoneInput = new EditText(requireContext());
        phoneInput.setHint("Nhập số điện thoại đặt hàng");
        phoneInput.setSingleLine(true);
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        phoneInput.setBackgroundResource(R.drawable.bg_normal);
        phoneInput.setPadding(dp(16), 0, dp(16), 0);
        phoneInput.setTextColor(0xFF1E1E1E);
        phoneInput.setHintTextColor(0xFF777777);
        phoneInput.setTextSize(14);
        phoneInput.setMinHeight(dp(48));
        phoneInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String phone = phoneInput.getText().toString().trim();
                if (!phone.matches("^0\\d{9}$")) {
                    phoneInput.setError("Số điện thoại không hợp lệ");
                    return true;
                }
                phoneInput.setError("Bấm nút Tra cứu để xác nhận");
                return true;
            }
            return false;
        });

        VeggoDialog.showWithContent(
                requireContext(),
                R.drawable.ic_order_green,
                "Tra cứu đơn guest",
                "Nhập số điện thoại đã dùng khi đặt đơn " + orderId,
                phoneInput,
                "Tra cứu",
                "Huỷ",
                new VeggoDialog.CustomDialogListener() {
                    @Override
                    public void onConfirm(android.app.Dialog dialog) {
                        String phone = phoneInput.getText().toString().trim();
                        if (!phone.matches("^0\\d{9}$")) {
                            phoneInput.setError("Số điện thoại không hợp lệ");
                            phoneInput.requestFocus();
                            return;
                        }
                        dialog.dismiss();
                        searchGuestOrder(orderId, phone);
                    }
                }
        );
    }

    private void searchGuestOrder(String orderId, String phone) {
        if (!phone.matches("^0\\d{9}$")) {
            Toast.makeText(requireContext(), "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiClient.createService(OrderApi.class).searchGuestOrder(orderId, phone).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Không tìm thấy đơn guest", Toast.LENGTH_SHORT).show();
                    return;
                }
                AssetModels.Order order = AssetScreenData.cacheGuestOrder(response.body());
                pendingStatusAfterLoad = tabStatusFor(order.status);
                searchQuery = order.orderId;
                loadOrders(false);
                Toast.makeText(requireContext(), "Đã tìm thấy đơn " + order.orderId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Toast.makeText(requireContext(), "Không thể tra cứu đơn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String tabStatusFor(String status) {
        String cleanStatus = status == null ? "" : status.trim();
        if ("pending".equals(cleanStatus)
                || "shipping".equals(cleanStatus)
                || "cancelled".equals(cleanStatus)) {
            return cleanStatus;
        }
        if ("delivered".equals(cleanStatus)
                || "completed".equals(cleanStatus)
                || "unreview".equals(cleanStatus)
                || "reviewed".equals(cleanStatus)
                || "rejected".equals(cleanStatus)) {
            return "delivered";
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        if ("delivered".equals(status) || "completed".equals(status)
                || "unreview".equals(status) || "reviewed".equals(status)
                || "returned".equals(status) || "rejected".equals(status)) {
            return R.layout.item_order_delivered;
        }
        if ("processing_return".equals(status) || "returning".equals(status)) {
            return R.layout.item_order_shipping;
        }
        return R.layout.item_order_cancelled;
    }

    private void bindDeliveredCardActions(View card, AssetModels.Order order) {
        TextView leftButton = card.findViewById(R.id.orderReturnRefundButton);
        TextView rightButton = card.findViewById(R.id.orderReceivedButton);
        if (leftButton == null || rightButton == null) {
            return;
        }

        String status = order.status == null ? "" : order.status;
        if ("unreview".equals(status) || "reviewed".equals(status)
                || "completed".equals(status) || "rejected".equals(status)) {
            leftButton.setText("Mua lại");
            rightButton.setText("Thêm vào tủ");
            leftButton.setOnClickListener(v -> {
                v.setEnabled(false);
                buyAgainOrder(order, v);
            });
            rightButton.setOnClickListener(v -> openOrderDetail(order.orderId));
        } else {
            leftButton.setText(R.string.orders_return_refund);
            rightButton.setText(R.string.orders_received);
            leftButton.setOnClickListener(v -> openReturnRequest(order.orderId));
            rightButton.setOnClickListener(v -> confirmReceived(order));
        }
    }

    private void bindCommonCardActions(View card, AssetModels.Order order) {
        TextView cancelButton = card.findViewById(R.id.orderCancelButton);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> confirmCancel(order));
        }

        TextView buyAgainButton = card.findViewById(R.id.orderBuyAgainButton);
        if (buyAgainButton != null) {
            buyAgainButton.setOnClickListener(v -> {
                v.setEnabled(false);
                buyAgainOrder(order, v);
            });
        }
    }

    private void confirmCancel(AssetModels.Order order) {
        VeggoDialog.show(
                requireContext(),
                R.drawable.ic_order_cancel_dialog,
                "Xác nhận hủy đơn",
                "Bạn có chắc chắn muốn hủy đơn hàng này không?",
                "Đồng ý",
                "Hủy bỏ",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        updateOrderStatus(order, "cancelled");
                    }
                }
        );
    }

    private void confirmReceived(AssetModels.Order order) {
        VeggoDialog.show(
                requireContext(),
                R.drawable.ic_order_delivered_box,
                "Xác nhận đã nhận hàng",
                "Bạn đã nhận được đơn hàng và hài lòng với sản phẩm?",
                "Xác nhận",
                "Đóng",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        updateOrderStatus(order, "unreview");
                    }
                }
        );
    }

    private void updateOrderStatus(AssetModels.Order order, String newStatus) {
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
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (finalSuccess) {
                    android.widget.Toast.makeText(requireContext(), "Đã xác nhận nhận hàng", android.widget.Toast.LENGTH_SHORT).show();
                    loadOrders(false);
                } else {
                    android.widget.Toast.makeText(requireContext(), finalErrorMessage, android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void openReturnRequest(String orderId) {
        Intent intent = new Intent(requireContext(), ReturnRequestActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
    }

    private void openOrderDetail(String orderId) {
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
    }

    private void buyAgainOrder(AssetModels.Order order, View sourceButton) {
        AssetModels.OrderDetail detail = snapshot == null ? null : snapshot.detailByOrderId.get(order.orderId);
        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            if (sourceButton != null) sourceButton.setEnabled(true);
            android.widget.Toast.makeText(requireContext(), "Không có sản phẩm nào để mua lại", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String customerId = new AppPreferences(requireContext()).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            if (sourceButton != null) sourceButton.setEnabled(true);
            android.widget.Toast.makeText(requireContext(), "Không tìm thấy thông tin người dùng", android.widget.Toast.LENGTH_SHORT).show();
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
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (sourceButton != null) sourceButton.setEnabled(true);
                if (finalSuccess) {
                    openCart(detail);
                } else {
                    android.widget.Toast.makeText(requireContext(), finalErrorMessage, android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void openCart(AssetModels.OrderDetail detail) {
        Intent intent = new Intent(requireContext(), MainActivity.class);
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
