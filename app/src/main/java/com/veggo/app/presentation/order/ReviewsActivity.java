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

public class ReviewsActivity extends BaseActivity {
    private TextView waitingTabText;
    private TextView doneTabText;
    private TextView waitingTabBadge;
    private TextView doneTabBadge;
    private View waitingIndicator;
    private View doneIndicator;
    private View waitingList;
    private View doneList;
    private View reviewsEmptyState;
    private List<AssetModels.Order> waitingOrders;
    private List<AssetModels.Order> doneOrders;
    private android.widget.HorizontalScrollView reviewsStatusScroll;

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

        findViewById(R.id.reviewWaitingTab).setOnClickListener(v -> showWaitingReviews());
        findViewById(R.id.reviewDoneTab).setOnClickListener(v -> showDoneReviews());
        loadReviews();
    }

    private void showWaitingReviews() {
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
            badge.setBackgroundResource(active
                    ? R.drawable.bg_notification_badge_alert
                    : R.drawable.bg_notification_badge_dark);
        }
    }

    private void loadReviews() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> {
                waitingOrders = AssetScreenData.filterReviewOrders(snapshot, false);
                doneOrders = AssetScreenData.filterReviewOrders(snapshot, true);
                setBadgeCount(waitingTabBadge, waitingOrders.size());
                setBadgeCount(doneTabBadge, doneOrders.size());
                bindList((LinearLayout) waitingList, waitingOrders, R.layout.item_review_waiting, snapshot);
                bindList((LinearLayout) doneList, doneOrders, R.layout.item_review_done, snapshot);
                showWaitingReviews();
            });
        }).start();
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
