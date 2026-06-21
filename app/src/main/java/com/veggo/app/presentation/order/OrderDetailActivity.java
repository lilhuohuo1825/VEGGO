package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.dto.FridgeBatchRequestDto;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class OrderDetailActivity extends BaseActivity {

    private static final SimpleDateFormat ISO_DATE =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    static {
        ISO_DATE.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** key = index trong danh sách sản phẩm, value = item */
    private final Map<Integer, AssetModels.OrderDetailItem> selectedItems = new HashMap<>();
    private AssetModels.OrderDetail currentDetail;
    private boolean isDeliveredOrder = false;

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

            // Lấy danh sách fridge items
            List<com.veggo.app.data.remote.dto.FridgeItemDto> fridgeItems = new java.util.ArrayList<>();
            try {
                String customerId = new AppPreferences(this).getCustomerId();
                if (customerId != null && !customerId.isEmpty()) {
                    FridgeApi api = ApiClient.createService(FridgeApi.class);
                    retrofit2.Response<List<com.veggo.app.data.remote.dto.FridgeItemDto>> response = api.getFridgeItems(customerId).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        fridgeItems = response.body();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final List<com.veggo.app.data.remote.dto.FridgeItemDto> finalFridgeItems = fridgeItems;

            runOnUiThread(() -> bindDetail(snapshot, order, detail, skuToIdMap, finalFridgeItems));
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
            java.util.Map<String, String> skuToIdMap,
            List<com.veggo.app.data.remote.dto.FridgeItemDto> fridgeItems
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
        }

        // Xác định loại đơn hàng để hiện/ẩn fridge feature
        // Chỉ cho phép tính năng Fridge đối với đơn hàng có trạng thái "completed"
        String status = order.status != null ? order.status : "";
        isDeliveredOrder = "completed".equals(status);
        currentDetail = detail;

        // Bind products (với checkbox nếu là đơn đã giao)
        if (detail != null) {
            bindProducts(detail, skuToIdMap, fridgeItems, order.orderId);
        }

        // Bind bottom action buttons dynamically based on status
        View bottomActions = findViewById(R.id.orderDetailBottomActions);
        View btnCancel = findViewById(R.id.orderDetailCancelButton);
        View btnReturn = findViewById(R.id.orderDetailReturnButton);
        View btnReceived = findViewById(R.id.orderDetailReceivedButton);
        View btnBuyAgain = findViewById(R.id.orderDetailBuyAgainButton);
        View btnAddToFridge = findViewById(R.id.orderDetailAddToFridgeButton);

        if (bottomActions != null && btnCancel != null && btnReturn != null
                && btnReceived != null && btnBuyAgain != null) {
            bottomActions.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
            btnReturn.setVisibility(View.GONE);
            btnReceived.setVisibility(View.GONE);
            btnBuyAgain.setVisibility(View.GONE);
            if (btnAddToFridge != null) btnAddToFridge.setVisibility(View.GONE);

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

                    // Hiện button thêm vào tủ lạnh nếu trạng thái là completed
                    if ("completed".equals(status) && btnAddToFridge != null) {
                        btnAddToFridge.setVisibility(View.VISIBLE);
                        btnAddToFridge.setOnClickListener(v -> onAddToFridgeClicked());
                    }

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
            // Cập nhật backend API
            try {
                com.veggo.app.data.remote.api.OrderApi orderApi =
                        ApiClient.createService(com.veggo.app.data.remote.api.OrderApi.class);
                java.util.Map<String, String> body = new java.util.HashMap<>();
                body.put("status", newStatus);
                // Dùng orderId (ORD...) để gọi PATCH API
                String apiOrderId = order.orderId;
                orderApi.updateOrderStatus(apiOrderId, body).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                loadOrderDetail(); // Reload order details to refresh UI
            });
        }).start();
    }

    private void bindProducts(AssetModels.OrderDetail detail, java.util.Map<String, String> skuToIdMap, List<com.veggo.app.data.remote.dto.FridgeItemDto> fridgeItems, String orderId) {
        LinearLayout container = findViewById(R.id.orderDetailProducts);
        container.removeAllViews();
        selectedItems.clear();

        if (detail.items == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        int topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_md);

        int totalAddable = 0;

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

            boolean alreadyAdded = false;
            for (com.veggo.app.data.remote.dto.FridgeItemDto fItem : fridgeItems) {
                if (orderId != null && orderId.equals(fItem.getOrderId()) && item.sku != null && item.sku.equals(fItem.getSku())) {
                    alreadyAdded = true;
                    break;
                }
            }

            // Checkbox cho tủ lạnh
            CheckBox checkBox = row.findViewById(R.id.orderDetailProductCheckBox);
            TextView addedBadge = row.findViewById(R.id.orderDetailProductAddedBadge);

            if (isDeliveredOrder) {
                if (alreadyAdded) {
                    if (checkBox != null) checkBox.setVisibility(View.GONE);
                    if (addedBadge != null) addedBadge.setVisibility(View.VISIBLE);
                    row.setAlpha(0.6f);
                } else {
                    totalAddable++;
                    if (addedBadge != null) addedBadge.setVisibility(View.GONE);
                    if (checkBox != null) {
                        checkBox.setVisibility(View.VISIBLE);
                        final int itemIndex = index;
                        // Khi bấm vào cả row thì toggle checkbox
                        row.setOnClickListener(v -> {
                            checkBox.setChecked(!checkBox.isChecked());
                            if (checkBox.isChecked()) {
                                selectedItems.put(itemIndex, item);
                            } else {
                                selectedItems.remove(itemIndex);
                            }
                            updateSelectAllCheckboxState();
                        });
                        checkBox.setOnClickListener(v -> {
                            if (checkBox.isChecked()) {
                                selectedItems.put(itemIndex, item);
                            } else {
                                selectedItems.remove(itemIndex);
                            }
                            updateSelectAllCheckboxState();
                        });
                    }
                }
            } else {
                // Đơn chưa giao: click vào để xem chi tiết sản phẩm
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, com.veggo.app.presentation.product.ProductDetailActivity.class);
                    String productId = item.sku != null ? skuToIdMap.get(item.sku) : null;
                    if (productId == null && item.objectId != null) {
                        productId = item.objectId.oid;
                    }
                    if (productId == null) {
                        productId = item.sku;
                    }
                    intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
                    startActivity(intent);
                });
            }

            container.addView(row);
        }

        CheckBox selectAllBox = findViewById(R.id.orderDetailSelectAllCheckBox);
        if (selectAllBox != null) {
            if (isDeliveredOrder && totalAddable > 0) {
                selectAllBox.setVisibility(View.VISIBLE);
                selectAllBox.setOnCheckedChangeListener(null);
                selectAllBox.setChecked(false);
                selectAllBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View row = container.getChildAt(i);
                        CheckBox cb = row.findViewById(R.id.orderDetailProductCheckBox);
                        if (cb != null && cb.getVisibility() == View.VISIBLE) {
                            cb.setChecked(isChecked);
                            if (isChecked) {
                                selectedItems.put(i, detail.items.get(i));
                            } else {
                                selectedItems.remove(i);
                            }
                        }
                    }
                });
            } else {
                selectAllBox.setVisibility(View.GONE);
            }
        }
    }

    private void updateSelectAllCheckboxState() {
        LinearLayout container = findViewById(R.id.orderDetailProducts);
        if (container == null) return;
        int totalAddable = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            CheckBox cb = row.findViewById(R.id.orderDetailProductCheckBox);
            if (cb != null && cb.getVisibility() == View.VISIBLE) {
                totalAddable++;
            }
        }

        CheckBox selectAllBox = findViewById(R.id.orderDetailSelectAllCheckBox);
        if (selectAllBox != null && totalAddable > 0) {
            selectAllBox.setOnCheckedChangeListener(null);
            selectAllBox.setChecked(selectedItems.size() == totalAddable);
            selectAllBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                for (int i = 0; i < container.getChildCount(); i++) {
                    View row = container.getChildAt(i);
                    CheckBox cb = row.findViewById(R.id.orderDetailProductCheckBox);
                    if (cb != null && cb.getVisibility() == View.VISIBLE) {
                        cb.setChecked(isChecked);
                        if (isChecked) {
                            selectedItems.put(i, currentDetail.items.get(i));
                        } else {
                            selectedItems.remove(i);
                        }
                    }
                }
            });
        }
    }

    private void onAddToFridgeClicked() {
        if (currentDetail == null || currentDetail.items == null || currentDetail.items.isEmpty()) {
            Toast.makeText(this, "Không có nguyên liệu nào trong đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 nguyên liệu để thêm", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AssetModels.OrderDetailItem> toAdd = new ArrayList<>(selectedItems.values());

        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        List<FridgeBatchRequestDto.FridgeItemRequestDto> dtoItems = new ArrayList<>();
        String purchaseDate = ISO_DATE.format(new Date());
        // Hạn sử dụng mặc định: 7 ngày sau ngày mua
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.DAY_OF_YEAR, 7);
        String expiryDate = ISO_DATE.format(cal.getTime());

        for (AssetModels.OrderDetailItem item : toAdd) {
            dtoItems.add(new FridgeBatchRequestDto.FridgeItemRequestDto(
                    item.productName != null ? item.productName : "",
                    item.quantity,
                    purchaseDate,
                    expiryDate,
                    item.sku != null ? item.sku : "",
                    currentDetail.orderId != null ? currentDetail.orderId : "",
                    item.image != null ? item.image : "",
                    null,
                    item.unit != null ? item.unit : "kg",
                    "history",
                    null
            ));
        }

        FridgeBatchRequestDto requestDto = new FridgeBatchRequestDto(dtoItems);

        // Vô hiệu hóa button để tránh bấm nhiều lần
        View btnAddToFridge = findViewById(R.id.orderDetailAddToFridgeButton);
        if (btnAddToFridge != null) btnAddToFridge.setEnabled(false);

        final String userId = customerId;
        new Thread(() -> {
            try {
                FridgeApi fridgeApi = ApiClient.createService(FridgeApi.class);
                retrofit2.Response<?> response = fridgeApi.addBatchItems(userId, requestDto).execute();
                runOnUiThread(() -> {
                    if (btnAddToFridge != null) btnAddToFridge.setEnabled(true);
                    if (response.isSuccessful()) {
                        // Reset selection
                        selectedItems.clear();
                        resetCheckboxes();
                        Toast.makeText(this,
                                "Đã thêm " + dtoItems.size() + " nguyên liệu vào tủ lạnh!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Không thể thêm vào tủ lạnh. Vui lòng thử lại.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (btnAddToFridge != null) btnAddToFridge.setEnabled(true);
                    Toast.makeText(this,
                            "Lỗi kết nối. Vui lòng kiểm tra mạng.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void resetCheckboxes() {
        LinearLayout container = findViewById(R.id.orderDetailProducts);
        if (container == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            CheckBox cb = row.findViewById(R.id.orderDetailProductCheckBox);
            if (cb != null) cb.setChecked(false);
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
