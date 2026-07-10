package com.veggo.app.presentation.order;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

public class CancelOrderActivity extends BaseActivity {

    private String orderId;
    private RadioGroup reasonGroup;
    private EditText descriptionInput;
    private TextView submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel_order);

        orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
        reasonGroup = findViewById(R.id.cancelReasonGroup);
        descriptionInput = findViewById(R.id.cancelDescriptionInput);
        submitButton = findViewById(R.id.cancelOrderSubmitButton);

        tintRequiredAsterisk(findViewById(R.id.cancelReasonLabel));

        findViewById(R.id.cancelOrderBackButton).setOnClickListener(v -> finish());
        submitButton.setOnClickListener(v -> submitCancelRequest());
        bindOrderSummary();
    }

    private void tintRequiredAsterisk(TextView label) {
        if (label == null) {
            return;
        }
        CharSequence text = label.getText();
        if (text == null) {
            return;
        }
        int starIndex = text.toString().indexOf('*');
        if (starIndex < 0) {
            return;
        }
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.danger_main)),
                starIndex,
                starIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        label.setText(spannable);
    }

    private void bindOrderSummary() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            AssetModels.Order order = null;
            for (AssetModels.Order item : snapshot.orders) {
                if (item.orderId != null && item.orderId.equals(orderId)) {
                    order = item;
                    break;
                }
            }
            AssetModels.Order finalOrder = order;
            AssetModels.OrderDetail detail = order == null ? null : snapshot.detailByOrderId.get(order.orderId);
            runOnUiThread(() -> {
                if (finalOrder == null || detail == null) {
                    Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.cancelOrderOrderCode,
                        "Giao hàng tận nơi · " + finalOrder.orderId);
                AssetScreenData.bindProductBlock(this, findViewById(R.id.cancelOrderProductCard), detail, finalOrder);
            });
        }).start();
    }

    private void submitCancelRequest() {
        int checkedId = reasonGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Vui lòng chọn lý do hủy đơn", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton checked = findViewById(checkedId);
        String reason = checked.getText().toString();
        String detail = descriptionInput.getText() == null ? "" : descriptionInput.getText().toString().trim();
        String cancelReason = detail.isEmpty() ? reason : reason + " - " + detail;

        submitButton.setEnabled(false);
        submitButton.setText("Đang hủy...");
        new Thread(() -> {
            boolean success = false;
            String errorMessage = "Không thể hủy đơn hàng";
            try {
                OrderApi orderApi = ApiClient.createService(OrderApi.class);
                Map<String, String> body = new HashMap<>();
                body.put("status", "cancelled");
                body.put("cancelReason", cancelReason);
                Response<Map<String, Object>> response = orderApi.updateOrderStatus(orderId, body).execute();
                success = response.isSuccessful();
                if (!success && response.errorBody() != null) {
                    errorMessage = response.errorBody().string();
                }
            } catch (Exception exception) {
                errorMessage = exception.getMessage() == null ? errorMessage : exception.getMessage();
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                submitButton.setEnabled(true);
                submitButton.setText(R.string.orders_cancel);
                if (finalSuccess) {
                    Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, finalErrorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
