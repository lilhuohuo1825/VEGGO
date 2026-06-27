package com.veggo.app.presentation.order;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.OrderApi;
import com.veggo.app.presentation.common.AssetScreenData;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class ReturnRequestActivity extends BaseActivity {

    private String orderId;
    private RadioGroup reasonGroup;
    private EditText descriptionInput;
    private TextView submitButton;
    private ViewGroup evidenceButton;
    private TextView evidenceCount;
    private LinearLayout evidenceContainer;
    private final List<ReturnEvidence> evidenceItems = new ArrayList<>();
    private ActivityResultLauncher<String> galleryPicker;
    private ActivityResultLauncher<Void> cameraPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_request);

        orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
        reasonGroup = findViewById(R.id.returnReasonGroup);
        descriptionInput = findViewById(R.id.returnDescriptionInput);
        submitButton = findViewById(R.id.returnSubmitButton);
        evidenceButton = findViewById(R.id.returnEvidenceButton);
        evidenceCount = findViewById(R.id.returnEvidenceCount);
        evidenceContainer = findViewById(R.id.returnEvidenceContainer);

        setupEvidencePickers();
        findViewById(R.id.returnRequestBackButton).setOnClickListener(v -> finish());
        submitButton.setOnClickListener(v -> submitReturnRequest());
        evidenceButton.setOnClickListener(v -> showEvidenceSourceDialog());
        updateEvidenceButton();
        bindOrderSummary();
    }

    private void setupEvidencePickers() {
        galleryPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onEvidenceSelected);
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onCameraEvidenceSelected);
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
                AssetScreenData.setText(findViewById(android.R.id.content), R.id.returnRequestOrderCode,
                        "Giao hàng tận nơi · " + finalOrder.orderId);
                AssetScreenData.bindProductBlock(this, findViewById(R.id.returnRequestProductCard), detail, finalOrder);
            });
        }).start();
    }

    private void submitReturnRequest() {
        int checkedId = reasonGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Vui lòng chọn lý do trả hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton checked = findViewById(checkedId);
        String reason = checked.getText().toString();
        String detail = descriptionInput.getText() == null ? "" : descriptionInput.getText().toString().trim();
        if (detail.isEmpty()) {
            descriptionInput.requestFocus();
            Toast.makeText(this, "Vui lòng nhập mô tả chi tiết vấn đề", Toast.LENGTH_SHORT).show();
            return;
        }
        if (evidenceItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ảnh/video minh chứng", Toast.LENGTH_SHORT).show();
            return;
        }
        String returnReason = reason + " - " + detail;

        submitButton.setEnabled(false);
        submitButton.setText("Đang gửi...");
        new Thread(() -> {
            boolean success = false;
            String errorMessage = "Không thể gửi yêu cầu trả hàng/hoàn tiền";
            try {
                OrderApi orderApi = ApiClient.createService(OrderApi.class);
                Map<String, String> body = new HashMap<>();
                body.put("status", "processing_return");
                body.put("returnReason", returnReason);
                body.put("returnEvidenceUrls", String.join(",", evidencePayload()));
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
                submitButton.setText("Gửi yêu cầu");
                if (finalSuccess) {
                    Toast.makeText(this, "Đã gửi yêu cầu trả hàng/hoàn tiền", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ReturnsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, finalErrorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showEvidenceSourceDialog() {
        if (evidenceItems.size() >= 5) {
            Toast.makeText(this, "Bạn chỉ có thể thêm tối đa 5 minh chứng", Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            galleryPicker.launch("*/*");
        });

        dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            cameraPicker.launch(null);
        });

        dialog.show();
    }

    private void onEvidenceSelected(Uri uri) {
        if (uri == null || evidenceItems.size() >= 5) {
            return;
        }
        String mimeType = getContentResolver().getType(uri);
        evidenceItems.add(new ReturnEvidence(uri, mimeType == null ? "" : mimeType));
        renderEvidence();
    }

    private void onCameraEvidenceSelected(Bitmap bitmap) {
        if (bitmap == null || evidenceItems.size() >= 5) {
            return;
        }
        Uri uri = saveBitmapToCache(bitmap);
        if (uri != null) {
            evidenceItems.add(new ReturnEvidence(uri, "image/jpeg"));
            renderEvidence();
        }
    }

    private void updateEvidenceButton() {
        if (evidenceButton == null) {
            return;
        }
        int count = evidenceItems.size();
        if (evidenceCount != null) {
            evidenceCount.setText(count + "/5");
        }
        evidenceButton.setAlpha(count >= 5 ? 0.55f : 1f);
    }

    private List<String> evidencePayload() {
        List<String> values = new ArrayList<>();
        for (ReturnEvidence item : evidenceItems) {
            values.add(item.uri.toString());
        }
        return values;
    }

    private void renderEvidence() {
        if (evidenceContainer == null) {
            updateEvidenceButton();
            return;
        }
        while (evidenceContainer.getChildCount() > 1) {
            evidenceContainer.removeViewAt(0);
        }
        int insertIndex = 0;
        for (ReturnEvidence item : evidenceItems) {
            evidenceContainer.addView(createEvidenceThumb(item), insertIndex++);
        }
        updateEvidenceButton();
    }

    private ViewGroup createEvidenceThumb(ReturnEvidence item) {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMarginEnd(dp(10));
        frame.setLayoutParams(params);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.bg_order_product_thumb);
        frame.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        if (item.isVideo()) {
            Bitmap thumbnail = getVideoThumbnail(item.uri);
            if (thumbnail != null) {
                image.setImageBitmap(thumbnail);
            } else {
                image.setImageResource(R.drawable.ic_camera);
            }
            frame.addView(overlayLabel("Video"));
        } else {
            image.setImageURI(item.uri);
        }

        TextView removeButton = new TextView(this);
        removeButton.setText("×");
        removeButton.setTextColor(getColor(R.color.background_main));
        removeButton.setTextSize(18);
        removeButton.setGravity(android.view.Gravity.CENTER);
        removeButton.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        removeButton.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(dp(24), dp(24));
        removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        frame.addView(removeButton, removeParams);
        removeButton.setOnClickListener(v -> {
            evidenceItems.remove(item);
            renderEvidence();
        });

        return frame;
    }

    private TextView overlayLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.background_main));
        label.setTextSize(12);
        label.setGravity(android.view.Gravity.CENTER);
        label.setBackgroundColor(0x66000000);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return label;
    }

    private Bitmap getVideoThumbnail(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            return retriever.getFrameAtTime(0);
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "return-evidence-" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream);
            }
            return Uri.fromFile(file);
        } catch (Exception exception) {
            Toast.makeText(this, "Không thể xử lý ảnh đã chụp", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class ReturnEvidence {
        final Uri uri;
        final String mimeType;

        ReturnEvidence(Uri uri, String mimeType) {
            this.uri = uri;
            this.mimeType = mimeType;
        }

        boolean isVideo() {
            return mimeType != null && mimeType.startsWith("video/");
        }
    }
}
