package com.veggo.app.presentation.profile;

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

public class CarbonHistoryActivity extends BaseActivity {
    private TextView allText;
    private TextView receivedText;
    private TextView redeemedText;
    private View allIndicator;
    private View receivedIndicator;
    private View redeemedIndicator;
    private View allList;
    private View receivedList;
    private View redeemedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carbon_history);
        findViewById(R.id.carbonHistoryBackButton).setOnClickListener(v -> finish());

        allText = findViewById(R.id.carbonTabAllText);
        receivedText = findViewById(R.id.carbonTabReceivedText);
        redeemedText = findViewById(R.id.carbonTabRedeemedText);
        allIndicator = findViewById(R.id.carbonTabAllIndicator);
        receivedIndicator = findViewById(R.id.carbonTabReceivedIndicator);
        redeemedIndicator = findViewById(R.id.carbonTabRedeemedIndicator);
        allList = findViewById(R.id.carbonHistoryAllList);
        receivedList = findViewById(R.id.carbonHistoryReceivedList);
        redeemedList = findViewById(R.id.carbonHistoryRedeemedList);

        findViewById(R.id.carbonTabAll).setOnClickListener(v -> showState(allText, allIndicator, allList));
        findViewById(R.id.carbonTabReceived).setOnClickListener(v -> showState(receivedText, receivedIndicator, receivedList));
        findViewById(R.id.carbonTabRedeemed).setOnClickListener(v -> showState(redeemedText, redeemedIndicator, redeemedList));
        loadHistory();
    }

    private void showState(TextView activeText, View activeIndicator, View activeList) {
        setActive(allText, allIndicator, allText == activeText);
        setActive(receivedText, receivedIndicator, receivedText == activeText);
        setActive(redeemedText, redeemedIndicator, redeemedText == activeText);

        allList.setVisibility(allList == activeList ? View.VISIBLE : View.GONE);
        receivedList.setVisibility(receivedList == activeList ? View.VISIBLE : View.GONE);
        redeemedList.setVisibility(redeemedList == activeList ? View.VISIBLE : View.GONE);
    }

    private void setActive(TextView textView, View indicator, boolean active) {
        textView.setTextColor(ContextCompat.getColor(this, active ? R.color.primary_main : R.color.neutral_60));
        indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadHistory() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindHistory(snapshot));
        }).start();
    }

    private void bindHistory(AssetScreenData.Snapshot snapshot) {
        bindEarnedList((LinearLayout) allList, snapshot);
        bindEarnedList((LinearLayout) receivedList, snapshot);
        ((LinearLayout) redeemedList).removeAllViews();
    }

    private void bindEarnedList(LinearLayout container, AssetScreenData.Snapshot snapshot) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.Order order : snapshot.orders) {
            AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
            if (detail == null || detail.carbonPointEarned <= 0) {
                continue;
            }
            View item = inflater.inflate(R.layout.item_carbon_history_earned_order, container, false);
            AssetScreenData.setText(item, R.id.carbonHistoryTitle, "Mua hàng xanh\n#" + order.orderId);
            AssetScreenData.setText(item, R.id.carbonHistoryDate, AssetScreenData.date(order.createdAt));
            AssetScreenData.setText(item, R.id.carbonHistoryPoints, "+" + detail.carbonPointEarned + " C");
            container.addView(item);
        }
    }
}
