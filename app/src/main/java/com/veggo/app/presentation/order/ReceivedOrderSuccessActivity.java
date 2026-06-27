package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class ReceivedOrderSuccessActivity extends BaseActivity {

    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_order_success);

        orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);

        TextView btnContinueShopping = findViewById(R.id.btnContinueShopping);
        TextView btnAddToFridge = findViewById(R.id.btnAddToFridge);

        btnContinueShopping.setOnClickListener(v -> openHomeProducts());
        btnAddToFridge.setOnClickListener(v -> openOrderDetailForFridge());
    }

    private void openHomeProducts() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_home);
        intent.putExtra(MainActivity.EXTRA_SCROLL_HOME_PRODUCTS, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openOrderDetailForFridge() {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
        finish();
    }
}
