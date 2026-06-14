package com.veggo.app.presentation.order;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

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
    private View pendingList;
    private View processingList;
    private View completedList;
    private View rejectedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_returns);

        findViewById(R.id.returnsBackButton).setOnClickListener(v -> finish());
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
        pendingList = findViewById(R.id.returnPendingList);
        processingList = findViewById(R.id.returnProcessingList);
        completedList = findViewById(R.id.returnCompletedList);
        rejectedList = findViewById(R.id.returnRejectedList);

        findViewById(R.id.returnPendingTab).setOnClickListener(v -> showReturnState(pendingTab, pendingList));
        findViewById(R.id.returnProcessingTab).setOnClickListener(v -> showReturnState(processingTab, processingList));
        findViewById(R.id.returnCompletedTab).setOnClickListener(v -> showReturnState(completedTab, completedList));
        findViewById(R.id.returnRejectedTab).setOnClickListener(v -> showReturnState(rejectedTab, rejectedList));
        loadReturns();
    }

    private void showReturnState(TextView activeTab, View activeList) {
        setActive(pendingTab, pendingBadge, pendingTab == activeTab);
        setActive(processingTab, processingBadge, processingTab == activeTab);
        setActive(completedTab, completedBadge, completedTab == activeTab);
        setActive(rejectedTab, rejectedBadge, rejectedTab == activeTab);
        pendingIndicator.setVisibility(pendingTab == activeTab ? View.VISIBLE : View.INVISIBLE);
        processingIndicator.setVisibility(processingTab == activeTab ? View.VISIBLE : View.INVISIBLE);
        completedIndicator.setVisibility(completedTab == activeTab ? View.VISIBLE : View.INVISIBLE);
        rejectedIndicator.setVisibility(rejectedTab == activeTab ? View.VISIBLE : View.INVISIBLE);

        pendingList.setVisibility(pendingList == activeList ? View.VISIBLE : View.GONE);
        processingList.setVisibility(processingList == activeList ? View.VISIBLE : View.GONE);
        completedList.setVisibility(completedList == activeList ? View.VISIBLE : View.GONE);
        rejectedList.setVisibility(rejectedList == activeList ? View.VISIBLE : View.GONE);
    }

    private void setActive(TextView textView, TextView badge, boolean active) {
        int colorRes = active ? R.color.primary_main : R.color.neutral_60;
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
        textView.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        if (badge != null) {
            badge.setBackgroundResource(active
                    ? R.drawable.bg_notification_badge_alert
                    : R.drawable.bg_notification_badge_dark);
        }
    }

    private void loadReturns() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindReturnLists(snapshot));
        }).start();
    }

    private void bindReturnLists(AssetScreenData.Snapshot snapshot) {
        List<AssetModels.Order> pendingOrders = AssetScreenData.filterReturnOrders(snapshot, "pending");
        List<AssetModels.Order> processingOrders = AssetScreenData.filterReturnOrders(snapshot, "processing");
        List<AssetModels.Order> completedOrders = AssetScreenData.filterReturnOrders(snapshot, "completed");
        List<AssetModels.Order> rejectedOrders = AssetScreenData.filterReturnOrders(snapshot, "rejected");
        setBadgeCount(pendingBadge, pendingOrders.size());
        setBadgeCount(processingBadge, processingOrders.size());
        setBadgeCount(completedBadge, completedOrders.size());
        setBadgeCount(rejectedBadge, rejectedOrders.size());
        bindList((LinearLayout) pendingList, pendingOrders, R.layout.item_return_pending, snapshot);
        bindList((LinearLayout) processingList, processingOrders, R.layout.item_return_processing, snapshot);
        bindList((LinearLayout) completedList, completedOrders, R.layout.item_return_completed, snapshot);
        bindList((LinearLayout) rejectedList, rejectedOrders, R.layout.item_return_rejected, snapshot);
        showReturnState(pendingTab, pendingList);
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
            item.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            container.addView(item);
        }
    }
}
