package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        loadOrderDetail();
    }

    private void loadOrderDetail() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            String orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
            AssetModels.Order order = findOrder(snapshot, orderId);
            AssetModels.OrderDetail detail = order == null ? null : snapshot.detailByOrderId.get(order.orderId);
            
            java.util.Map<String, String> skuToIdMap = new java.util.HashMap<>();
            if (detail != null && detail.items != null) {
                com.veggo.app.data.local.dao.ProductDao dao = com.veggo.app.core.database.VeggoDatabase.getInstance(this).productDao();
                for (AssetModels.OrderDetailItem item : detail.items) {
                    if (item.sku != null && !skuToIdMap.containsKey(item.sku)) {
                        String pId = dao.getProductIdBySku(item.sku);
                        if (pId != null) {
                            skuToIdMap.put(item.sku, pId);
                        }
                    }
                }
            }
            
            runOnUiThread(() -> bindDetail(snapshot, order, detail, skuToIdMap));
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
            AssetModels.OrderDetail detail,
            java.util.Map<String, String> skuToIdMap
    ) {
        if (order == null) {
            return;
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.orderDetailCode, order.orderId);
        TextView tvStatus = findViewById(R.id.orderDetailStatus);
        if (tvStatus != null) {
            tvStatus.setText(AssetScreenData.statusLabel(order.status));
            String status = order.status != null ? order.status : "";
            int bgRes = R.drawable.bg_order_delivered_chip;
            int textColorRes = R.color.primary_main;
            switch (status) {
                case "pending":
                    bgRes = R.drawable.bg_order_pending_chip;
                    textColorRes = R.color.order_status_pending;
                    break;
                case "shipping":
                    bgRes = R.drawable.bg_order_shipping_chip;
                    textColorRes = R.color.order_status_shipping;
                    break;
                case "delivered":
                case "completed":
                case "returned":
                    bgRes = R.drawable.bg_order_delivered_chip;
                    textColorRes = R.color.order_status_delivered;
                    break;
                case "cancelled":
                    bgRes = R.drawable.bg_order_cancelled_chip;
                    textColorRes = R.color.order_status_cancelled;
                    break;
            }
            tvStatus.setBackgroundResource(bgRes);
            tvStatus.setTextColor(getColor(textColorRes));
        }
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
            bindProducts(detail, skuToIdMap);
        }

        // Bind bottom action buttons dynamically based on status
        View bottomActions = findViewById(R.id.orderDetailBottomActions);
        View btnCancel = findViewById(R.id.orderDetailCancelButton);
        View btnReturn = findViewById(R.id.orderDetailReturnButton);
        View btnReceived = findViewById(R.id.orderDetailReceivedButton);
        View btnBuyAgain = findViewById(R.id.orderDetailBuyAgainButton);

        if (bottomActions != null && btnCancel != null && btnReturn != null && btnReceived != null && btnBuyAgain != null) {
            // Hide all buttons by default
            bottomActions.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
            btnReturn.setVisibility(View.GONE);
            btnReceived.setVisibility(View.GONE);
            btnBuyAgain.setVisibility(View.GONE);

            String status = order.status != null ? order.status : "";
            switch (status) {
                case "pending":
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(v -> {
                        com.veggo.app.presentation.dialog.VeggoDialog.show(
                                this,
                                null,
                                "Xác nhận hủy đơn",
                                "Bạn có chắc chắn muốn hủy đơn hàng này không?",
                                "Đồng ý",
                                "Hủy bỏ",
                                new com.veggo.app.presentation.dialog.VeggoDialog.DialogListener() {
                                    @Override
                                    public void onConfirm() {
                                        updateOrderStatus(order, "cancelled", "Đã huỷ đơn hàng thành công");
                                    }
                                }
                        );
                    });
                    break;

                case "shipping":
                    bottomActions.setVisibility(View.GONE);
                    break;

                case "delivered":
                case "completed":
                case "returned":
                    btnReturn.setVisibility(View.VISIBLE);
                    btnReceived.setVisibility(View.VISIBLE);

                    btnReturn.setOnClickListener(v ->
                            startActivity(new Intent(this, ReturnsActivity.class))
                    );

                    btnReceived.setOnClickListener(v -> {
                        com.veggo.app.presentation.dialog.VeggoDialog.show(
                                this,
                                null,
                                "Xác nhận đã nhận hàng",
                                "Bạn đã nhận được đơn hàng và hài lòng với sản phẩm?",
                                "Xác nhận",
                                "Đóng",
                                new com.veggo.app.presentation.dialog.VeggoDialog.DialogListener() {
                                    @Override
                                    public void onConfirm() {
                                        updateOrderStatus(order, "completed", "Cảm ơn bạn đã nhận hàng!");
                                    }
                                }
                        );
                    });
                    break;

                case "cancelled":
                    btnBuyAgain.setVisibility(View.VISIBLE);
                    btnBuyAgain.setOnClickListener(v ->
                            startActivity(new Intent(this, CheckoutActivity.class))
                    );
                    break;

                default:
                    bottomActions.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void updateOrderStatus(AssetModels.Order order, String newStatus, String toastMsg) {
        order.status = newStatus;
        new Thread(() -> {
            String documentId = order.orderId;
            if (order.objectId != null && order.objectId.oid != null && !order.objectId.oid.trim().isEmpty()) {
                documentId = order.objectId.oid;
            }
            new com.veggo.app.core.database.AssetRepository(this).upsert(
                    com.veggo.app.assets.AssetFiles.COLLECTION_ORDERS,
                    documentId,
                    order
            );
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, toastMsg, android.widget.Toast.LENGTH_SHORT).show();
                loadOrderDetail(); // Reload order details to refresh UI
            });
        }).start();
    }

    private void bindProducts(AssetModels.OrderDetail detail, java.util.Map<String, String> skuToIdMap) {
        LinearLayout container = findViewById(R.id.orderDetailProducts);
        container.removeAllViews();
        if (detail.items == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        int topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        for (int index = 0; index < detail.items.size(); index++) {
            AssetModels.OrderDetailItem item = detail.items.get(index);
            View row = inflater.inflate(R.layout.item_order_detail_product_kale, container, false);
            if (index > 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = topMargin;
                row.setLayoutParams(params);
            }
            AssetScreenData.bindDetailProduct(this, row, item);
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.veggo.app.presentation.product.ProductDetailActivity.class);
                String productId = item.sku != null ? skuToIdMap.get(item.sku) : null;
                if (productId == null && item.objectId != null) {
                    productId = item.objectId.oid; // Fallback
                }
                if (productId == null) {
                    productId = item.sku; // Fallback
                }
                intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
                startActivity(intent);
            });
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
