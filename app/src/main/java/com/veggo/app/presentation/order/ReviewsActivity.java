package com.veggo.app.presentation.order;

import android.os.Bundle;
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
    private View waitingIndicator;
    private View doneIndicator;
    private View waitingList;
    private View doneList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        findViewById(R.id.reviewsBackButton).setOnClickListener(v -> finish());
        waitingTabText = findViewById(R.id.reviewWaitingTabText);
        doneTabText = findViewById(R.id.reviewDoneTabText);
        waitingIndicator = findViewById(R.id.reviewWaitingIndicator);
        doneIndicator = findViewById(R.id.reviewDoneIndicator);
        waitingList = findViewById(R.id.reviewWaitingList);
        doneList = findViewById(R.id.reviewDoneList);

        findViewById(R.id.reviewWaitingTab).setOnClickListener(v -> showWaitingReviews());
        findViewById(R.id.reviewDoneTab).setOnClickListener(v -> showDoneReviews());
        loadReviews();
    }

    private void showWaitingReviews() {
        setActive(waitingTabText, true);
        setActive(doneTabText, false);
        waitingIndicator.setVisibility(View.VISIBLE);
        doneIndicator.setVisibility(View.INVISIBLE);
        waitingList.setVisibility(View.VISIBLE);
        doneList.setVisibility(View.GONE);
    }

    private void showDoneReviews() {
        setActive(waitingTabText, false);
        setActive(doneTabText, true);
        waitingIndicator.setVisibility(View.INVISIBLE);
        doneIndicator.setVisibility(View.VISIBLE);
        waitingList.setVisibility(View.GONE);
        doneList.setVisibility(View.VISIBLE);
    }

    private void setActive(TextView textView, boolean active) {
        int colorRes = active ? R.color.primary_main : R.color.neutral_60;
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private void loadReviews() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> {
                bindList((LinearLayout) waitingList, AssetScreenData.filterReviewOrders(snapshot, false), R.layout.item_review_waiting, snapshot);
                bindList((LinearLayout) doneList, AssetScreenData.filterReviewOrders(snapshot, true), R.layout.item_review_done, snapshot);
            });
        }).start();
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
