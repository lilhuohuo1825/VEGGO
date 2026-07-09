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

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewsActivity extends BaseActivity {
    public static final String EXTRA_SHOW_DONE_REVIEWS = "extra_show_done_reviews";

    private TextView waitingTabText;
    private TextView doneTabText;
    private TextView waitingTabBadge;
    private TextView doneTabBadge;
    private View waitingIndicator;
    private View doneIndicator;
    private View waitingList;
    private View doneList;
    private View reviewsEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView reviewListScroll;
    private List<AssetModels.Order> waitingOrders;
    private List<AssetModels.Order> doneOrders;
    private android.widget.HorizontalScrollView reviewsStatusScroll;
    private AssetScreenData.Snapshot snapshot;
    private String searchQuery = "";
    private boolean showingDoneReviews = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        findViewById(R.id.reviewsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.reviewsMenuButton).setOnClickListener(v -> AssetScreenData.showOrderOptions(this));
        waitingTabText = findViewById(R.id.reviewWaitingTabText);
        doneTabText = findViewById(R.id.reviewDoneTabText);
        waitingTabBadge = findViewById(R.id.reviewWaitingTabBadge);
        doneTabBadge = findViewById(R.id.reviewDoneTabBadge);
        waitingIndicator = findViewById(R.id.reviewWaitingIndicator);
        doneIndicator = findViewById(R.id.reviewDoneIndicator);
        waitingList = findViewById(R.id.reviewWaitingList);
        doneList = findViewById(R.id.reviewDoneList);
        reviewsEmptyState = findViewById(R.id.reviewsEmptyState);
        reviewsStatusScroll = findViewById(R.id.reviewsStatusScroll);
        reviewListScroll = findViewById(R.id.reviewListScroll);
        swipeRefreshLayout = findViewById(R.id.reviewsSwipeRefresh);
        if (swipeRefreshLayout != null) {
            PullToRefreshHelper.bind(swipeRefreshLayout, reviewListScroll, () -> loadReviews(true));
        }

        findViewById(R.id.reviewWaitingTab).setOnClickListener(v -> showWaitingReviews());
        findViewById(R.id.reviewDoneTab).setOnClickListener(v -> showDoneReviews());
        EditText searchInput = findViewById(R.id.reviewSearchInput);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim();
                    if (snapshot != null) {
                        bindReviews(snapshot);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
        if (getIntent().getBooleanExtra(EXTRA_SHOW_DONE_REVIEWS, false)) {
            getIntent().removeExtra(EXTRA_SHOW_DONE_REVIEWS);
            showDoneReviews();
        }
    }

    private void showWaitingReviews() {
        showingDoneReviews = false;
        setActive(waitingTabText, waitingTabBadge, true);
        setActive(doneTabText, doneTabBadge, false);
        waitingIndicator.setVisibility(View.VISIBLE);
        doneIndicator.setVisibility(View.INVISIBLE);
        boolean isEmpty = waitingOrders == null || waitingOrders.isEmpty();
        waitingList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        doneList.setVisibility(View.GONE);
        if (reviewsEmptyState != null) {
            reviewsEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        centerTab(findViewById(R.id.reviewWaitingTab));
    }

    private void showDoneReviews() {
        showingDoneReviews = true;
        setActive(waitingTabText, waitingTabBadge, false);
        setActive(doneTabText, doneTabBadge, true);
        waitingIndicator.setVisibility(View.INVISIBLE);
        doneIndicator.setVisibility(View.VISIBLE);
        waitingList.setVisibility(View.GONE);
        boolean isEmpty = doneOrders == null || doneOrders.isEmpty();
        doneList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (reviewsEmptyState != null) {
            reviewsEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        centerTab(findViewById(R.id.reviewDoneTab));
    }

    private void centerTab(View tab) {
        if (tab != null && reviewsStatusScroll != null) {
            reviewsStatusScroll.post(() -> {
                int scrollX = tab.getLeft() - (reviewsStatusScroll.getWidth() - tab.getWidth()) / 2;
                reviewsStatusScroll.smoothScrollTo(scrollX, 0);
            });
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

    private void loadReviews() {
        loadReviews(false);
    }

    private void loadReviews(boolean fromSwipeRefresh) {
        new Thread(() -> {
            AssetScreenData.Snapshot loaded = AssetScreenData.load(this);
            runOnUiThread(() -> {
                bindReviews(loaded);
                if (fromSwipeRefresh && swipeRefreshLayout != null) {
                    PullToRefreshHelper.finish(swipeRefreshLayout);
                }
            });
        }).start();
    }

    private void bindReviews(AssetScreenData.Snapshot snapshot) {
        this.snapshot = snapshot;
        waitingOrders = filterOrdersByQuery(AssetScreenData.filterReviewOrders(snapshot, false));
        doneOrders = filterOrdersByQuery(AssetScreenData.filterReviewOrders(snapshot, true));
        setBadgeCount(waitingTabBadge, waitingOrders.size());
        setBadgeCount(doneTabBadge, doneOrders.size());
        bindList((LinearLayout) waitingList, waitingOrders, R.layout.item_review_waiting, snapshot);
        bindList((LinearLayout) doneList, doneOrders, R.layout.item_review_done, snapshot);
        if (showingDoneReviews) {
            showDoneReviews();
        } else {
            showWaitingReviews();
        }
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
            bindActions(item, order, layout == R.layout.item_review_waiting);
            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            container.addView(item);
        }
    }

    private void bindActions(View item, AssetModels.Order order, boolean waitingReview) {
        View buyAgainButton = item.findViewById(R.id.reviewBuyAgainButton);
        if (buyAgainButton != null) {
            buyAgainButton.setOnClickListener(v -> {
                v.setEnabled(false);
                buyAgainOrder(order, v);
            });
        }

        View reviewButton = item.findViewById(R.id.reviewActionButton);
        if (reviewButton != null) {
            reviewButton.setVisibility(waitingReview ? View.VISIBLE : View.GONE);
            if (waitingReview) {
                reviewButton.setOnClickListener(v -> openReviewForm(order.orderId));
            }
        }

        View viewReviewButton = item.findViewById(R.id.reviewViewButton);
        if (viewReviewButton != null) {
            viewReviewButton.setVisibility(waitingReview ? View.GONE : View.VISIBLE);
            if (!waitingReview) {
                viewReviewButton.setOnClickListener(v -> openReviewForm(order.orderId));
            }
        }
    }

    private void openReviewForm(String orderId) {
        Intent intent = new Intent(this, ReviewOrderActivity.class);
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
