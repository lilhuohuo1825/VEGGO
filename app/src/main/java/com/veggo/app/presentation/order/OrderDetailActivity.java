package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class OrderDetailActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        findViewById(R.id.orderDetailBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.orderDetailReturnButton).setOnClickListener(v ->
                startActivity(new Intent(this, ReturnsActivity.class))
        );
        findViewById(R.id.orderDetailBuyAgainButton).setOnClickListener(v ->
                startActivity(new Intent(this, CheckoutActivity.class))
        );
        loadOrderDetail();
    }

    private void loadOrderDetail() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            String orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
            AssetModels.Order order = findOrder(snapshot, orderId);
            AssetModels.OrderDetail detail = order == null ? null : snapshot.detailByOrderId.get(order.orderId);
            runOnUiThread(() -> bindDetail(snapshot, order, detail));
        }).start();
    }

    private AssetModels.Order findOrder(AssetScreenData.Snapshot snapshot, String orderId) {
        for (AssetModels.Order order : snapshot.orders) {
            if (order.orderId.equals(orderId)) {
                return order;
            }
        }
        return snapshot.orders.isEmpty() ? null : snapshot.orders.get(0);
    }

    private void bindDetail(
            AssetScreenData.Snapshot snapshot,
            AssetModels.Order order,
            AssetModels.OrderDetail detail
    ) {
        if (order == null) {
            return;
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailCode, order.orderId);
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailStatus, AssetScreenData.statusLabel(order.status));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailTime, AssetScreenData.date(order.createdAt));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailPayment, AssetScreenData.paymentLabel(order.paymentMethod));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailSubtotalValue, AssetScreenData.money(order.subtotal));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailProductDiscountValue, "-" + AssetScreenData.money(productDiscount(detail)));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailDiscountValue, "-" + AssetScreenData.money(Math.round(order.discount)));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailShippingFeeValue, AssetScreenData.money(Math.max(0, order.shippingFee - order.shippingDiscount)));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailTotalValue, AssetScreenData.money(Math.round(order.totalAmount)));

        if (detail != null && detail.shippingInfo != null) {
            AssetModels.Warehouse warehouse = snapshot.warehouseById.get(detail.shippingInfo.warehouseId);
            String warehouseText = warehouse == null ? detail.shippingInfo.warehouseId : warehouse.warehouseName;
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailWarehouse, warehouseText);
            bindProducts(detail);
        }
    }

    private void bindProducts(AssetModels.OrderDetail detail) {
        LinearLayout container = findViewById(R.id.orderDetailProducts);
        container.removeAllViews();
        if (detail.items == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        int topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        for (int index = 0; index < detail.items.size(); index++) {
            View row = inflater.inflate(R.layout.item_order_detail_product_kale, container, false);
            if (index > 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = topMargin;
                row.setLayoutParams(params);
            }
            AssetScreenData.bindDetailProduct(this, row, detail.items.get(index));
            container.addView(row);
        }
    }

    private long productDiscount(AssetModels.OrderDetail detail) {
        if (detail == null || detail.items == null) {
            return 0;
        }
        long discount = 0;
        for (AssetModels.OrderDetailItem item : detail.items) {
            discount += Math.max(0, item.originalPrice - item.price) * item.quantity;
        }
        return discount;
    }
}
