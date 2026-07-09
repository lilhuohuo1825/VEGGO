package com.veggo.app.presentation.order;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.utils.UserAvatarHelper;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.utils.CameraCaptureHelper;
import com.veggo.app.core.utils.ProductCatalogImageResolver;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.core.utils.ReviewMediaHelper;
import com.veggo.app.data.remote.api.ReviewApi;
import com.veggo.app.data.remote.dto.ReviewMediaUploadResponseDto;
import com.veggo.app.presentation.common.AssetScreenData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class ReviewOrderActivity extends BaseActivity {

    private final List<ProductReviewForm> forms = new ArrayList<>();
    private LinearLayout formsContainer;
    private TextView submitButton;
    private TextView hintText;
    private String orderId;
    private boolean canEditExisting;
    private boolean hasExistingReviews;
    private ProductReviewForm activeMediaForm;
    private boolean activeMediaIsVideo;
    private ActivityResultLauncher<String> mediaPicker;
    private ActivityResultLauncher<PickVisualMediaRequest> multiImagePicker;
    private CameraCaptureHelper imageCameraHelper;
    private ActivityResultLauncher<Intent> videoCameraPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);

        orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
        formsContainer = findViewById(R.id.reviewOrderProductForms);
        submitButton = findViewById(R.id.reviewOrderSubmitButton);
        hintText = findViewById(R.id.reviewOrderHintText);

        setupMediaPickers();
        findViewById(R.id.reviewOrderBackButton).setOnClickListener(v -> finish());
        submitButton.setOnClickListener(v -> submitReviews());

        loadOrder();
    }

    private void setupMediaPickers() {
        multiImagePicker = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(3), uris -> {
            if (uris == null || uris.isEmpty() || activeMediaForm == null) {
                return;
            }
            int remaining = Math.max(0, 3 - activeMediaForm.imageCount());
            if (remaining == 0) {
                Toast.makeText(this, "Chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
                return;
            }
            int added = 0;
            for (Uri uri : uris) {
                if (added >= remaining) {
                    break;
                }
                activeMediaForm.media.add(new ReviewMedia(
                        uri,
                        "review-image.jpg",
                        getContentResolver().getType(uri) != null ? getContentResolver().getType(uri) : "image/jpeg"
                ));
                added++;
            }
            activeMediaForm.renderMedia(this);
            if (uris.size() > remaining) {
                Toast.makeText(this, "Chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
            }
        });
        mediaPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && activeMediaForm != null) {
                if (activeMediaIsVideo && activeMediaForm.videoCount() >= 1) {
                    Toast.makeText(this, "Chỉ được chọn tối đa 1 video", Toast.LENGTH_SHORT).show();
                    return;
                }
                activeMediaForm.media.add(new ReviewMedia(
                        uri,
                        activeMediaIsVideo ? "review-video.mp4" : "review-image.jpg",
                        getContentResolver().getType(uri) != null ? getContentResolver().getType(uri) : (activeMediaIsVideo ? "video/mp4" : "image/jpeg")
                ));
                activeMediaForm.renderMedia(this);
            }
        });
        imageCameraHelper = new CameraCaptureHelper(this);
        videoCameraPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null
                    && result.getData().getData() != null && activeMediaForm != null) {
                if (activeMediaForm.videoCount() >= 1) {
                    Toast.makeText(this, "Chỉ được chọn tối đa 1 video", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri uri = result.getData().getData();
                activeMediaForm.media.add(new ReviewMedia(uri, "review-video.mp4", "video/mp4"));
                activeMediaForm.renderMedia(this);
            }
        });
    }

    private void loadOrder() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(orderId);
            String customerId = new AppPreferences(this).getCustomerId();
            Map<String, Object> reviewsBySku = new HashMap<>();
            boolean editable = false;
            try {
                if (customerId != null && !customerId.trim().isEmpty()) {
                    ReviewApi reviewApi = ApiClient.createService(ReviewApi.class);
                    Response<Map<String, Object>> response = reviewApi.getOrderReviews(orderId, customerId).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> body = response.body();
                        Object canEditValue = body.get("canEdit");
                        if (canEditValue instanceof Boolean) {
                            editable = (Boolean) canEditValue;
                        }
                        Object reviewsValue = body.get("reviewsBySku");
                        if (reviewsValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = (Map<String, Object>) reviewsValue;
                            reviewsBySku = parsed;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fall back to blank forms when review lookup fails.
            }

            String orderStatus = "";
            if (snapshot.orders != null) {
                for (AssetModels.Order order : snapshot.orders) {
                    if (orderId != null && orderId.equals(order.orderId)) {
                        orderStatus = order.status == null ? "" : order.status;
                        break;
                    }
                }
            }

            final boolean finalEditable = editable;
            final Map<String, Object> finalReviewsBySku = reviewsBySku;
            final String finalOrderStatus = orderStatus;
            runOnUiThread(() -> bindForms(detail, finalReviewsBySku, finalEditable, finalOrderStatus));
        }).start();
    }

    private void bindForms(
            AssetModels.OrderDetail detail,
            Map<String, Object> reviewsBySku,
            boolean canEdit,
            String orderStatus
    ) {
        forms.clear();
        formsContainer.removeAllViews();

        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm trong đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        hasExistingReviews = reviewsBySku != null && !reviewsBySku.isEmpty();
        canEditExisting = canEdit;
        if ("reviewed".equals(orderStatus) && !hasExistingReviews) {
            Toast.makeText(this, "Không tìm thấy đánh giá của đơn hàng này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (hasExistingReviews && !canEditExisting) {
            hintText.setText(R.string.review_order_readonly_hint);
        } else if (hasExistingReviews) {
            hintText.setText(R.string.review_order_edit_hint);
        } else {
            hintText.setText(R.string.review_order_carbon_hint);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.OrderDetailItem item : detail.items) {
            View formView = inflater.inflate(R.layout.item_review_product_form, formsContainer, false);
            bindProductHeader(formView, item);
            ProductReviewForm form = new ProductReviewForm(
                    item,
                    formView.findViewById(R.id.reviewProductRating),
                    formView.findViewById(R.id.reviewProductContent),
                    formView.findViewById(R.id.reviewProductCharCount),
                    formView.findViewById(R.id.reviewMediaContainer),
                    formView.findViewById(R.id.reviewPhotoContainer),
                    formView.findViewById(R.id.reviewPhotoPlaceholder),
                    formView.findViewById(R.id.reviewVideoContainer),
                    formView.findViewById(R.id.reviewVideoPlaceholder)
            );
            Map<String, Object> existing = skuReviewMap(reviewsBySku, item.sku);
            boolean formEditable = !hasExistingReviews || (existing == null ? canEditExisting : canEditExisting);
            if (existing != null) {
                applyExistingToForm(form, existing, formEditable);
            } else {
                form.photoContainer.setOnClickListener(v -> showMediaSourceDialog(form, false));
                form.videoContainer.setOnClickListener(v -> showMediaSourceDialog(form, true));
                form.renderMedia(this, formEditable);
                form.attachCharacterCounter();
            }
            if (!formEditable) {
                form.setReadOnly();
            }
            forms.add(form);
            formsContainer.addView(formView);
        }

        if (hasExistingReviews && !canEditExisting) {
            submitButton.setVisibility(View.GONE);
        } else {
            submitButton.setVisibility(View.VISIBLE);
            submitButton.setText(hasExistingReviews
                    ? getString(R.string.review_order_update)
                    : "Gửi đánh giá");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> skuReviewMap(Map<String, Object> reviewsBySku, String sku) {
        if (reviewsBySku == null || sku == null || sku.trim().isEmpty()) {
            return null;
        }
        Object value = reviewsBySku.get(sku.trim());
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private void applyExistingToForm(ProductReviewForm form, Map<String, Object> data, boolean editable) {
        Object rating = data.get("rating");
        if (rating instanceof Number) {
            form.ratingBar.setRating(((Number) rating).floatValue());
        }
        Object content = data.get("content");
        if (content != null) {
            form.contentInput.setText(String.valueOf(content));
        }
        Object imagesObj = data.get("images");
        if (imagesObj instanceof List) {
            for (Object imageObj : (List<?>) imagesObj) {
                String url = UserAvatarHelper.resolveUrl(String.valueOf(imageObj));
                if (url == null || url.trim().isEmpty()) {
                    continue;
                }
                String lower = url.toLowerCase();
                String mimeType = lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm")
                        ? "video/mp4" : "image/jpeg";
                form.media.add(new ReviewMedia(Uri.parse(url), "remote", mimeType, url));
            }
        }
        form.photoContainer.setOnClickListener(v -> showMediaSourceDialog(form, false));
        form.videoContainer.setOnClickListener(v -> showMediaSourceDialog(form, true));
        form.renderMedia(this, editable);
        form.attachCharacterCounter();
    }

    private void showMediaSourceDialog(ProductReviewForm form, boolean isVideo) {
        if (hasExistingReviews && !canEditExisting) {
            return;
        }
        activeMediaForm = form;
        activeMediaIsVideo = isVideo;
        if (!isVideo && form.imageCount() >= 3) {
            Toast.makeText(this, "Chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isVideo && form.videoCount() >= 1) {
            Toast.makeText(this, "Chỉ được chọn tối đa 1 video", Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView title = dialog.findViewById(R.id.dialogSourceTitle);
        TextView galleryLabel = dialog.findViewById(R.id.dialogOptionGalleryLabel);
        TextView cameraLabel = dialog.findViewById(R.id.dialogOptionCameraLabel);
        if (isVideo) {
            if (title != null) title.setText("Nguồn video");
            if (galleryLabel != null) galleryLabel.setText("Chọn video từ thư viện");
            if (cameraLabel != null) cameraLabel.setText("Quay video bằng camera");
        } else {
            if (title != null) title.setText("Nguồn ảnh");
            if (galleryLabel != null) galleryLabel.setText("Chọn từ thư viện");
            if (cameraLabel != null) cameraLabel.setText("Chụp ảnh bằng camera");
        }

        dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            if (isVideo) {
                mediaPicker.launch("video/*");
            } else {
                multiImagePicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });

        dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            if (isVideo) {
                videoCameraPicker.launch(new Intent(MediaStore.ACTION_VIDEO_CAPTURE));
            } else {
                imageCameraHelper.openCamera(new CameraCaptureHelper.Listener() {
                    @Override
                    public void onImageCaptured(@NonNull Uri imageUri) {
                        if (activeMediaForm == null) {
                            return;
                        }
                        if (activeMediaForm.imageCount() >= 3) {
                            Toast.makeText(ReviewOrderActivity.this, "Chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        activeMediaForm.media.add(new ReviewMedia(imageUri, "review-camera.jpg", "image/jpeg"));
                        activeMediaForm.renderMedia(ReviewOrderActivity.this);
                    }

                    @Override
                    public void onPermissionDenied() {
                        Toast.makeText(ReviewOrderActivity.this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void bindProductHeader(View view, AssetModels.OrderDetailItem item) {
        TextView name = view.findViewById(R.id.reviewProductName);
        TextView variant = view.findViewById(R.id.reviewProductVariant);
        ImageView image = view.findViewById(R.id.reviewProductImage);

        name.setText(item.productName == null ? "" : item.productName);
        variant.setText((item.unit == null ? "" : item.unit) + "  x" + Math.max(1, item.quantity));
        String imageUrl = ProductCatalogImageResolver.resolveOrderItemImage(this, item.image, item.sku);
        ProductImageUtils.loadInto(this, image, imageUrl, R.drawable.ic_vegetable, R.drawable.ic_vegetable);
    }

    private void submitReviews() {
        if (forms.isEmpty()) {
            return;
        }

        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ProductReviewForm form : forms) {
            if (form.ratingBar.getRating() < 1f) {
                Toast.makeText(this, "Vui lòng chọn số sao cho tất cả sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        submitButton.setEnabled(false);
        submitButton.setText("Đang gửi...");

        String fullName = new AppPreferences(this).getFullName();
        new Thread(() -> {
            boolean success = true;
            String error = "Không thể gửi đánh giá";
            int carbonEarned = 0;
            int sessionCarbon = 0;
            try {
                ReviewApi reviewApi = ApiClient.createService(ReviewApi.class);
                for (ProductReviewForm form : forms) {
                    if (form.item.sku == null || form.item.sku.trim().isEmpty()) {
                        success = false;
                        error = "Không tìm thấy mã SKU cho sản phẩm: "
                                + (form.item.productName == null ? "" : form.item.productName);
                        break;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("sku", form.item.sku.trim());
                    body.put("customer_id", customerId);
                    body.put("order_id", orderId);
                    body.put("fullname", fullName == null || fullName.trim().isEmpty() ? "Khách hàng VEGGO" : fullName);
                    body.put("rating", Math.round(form.ratingBar.getRating()));
                    body.put("content", form.contentInput.getText().toString().trim());
                    body.put("images", uploadReviewMedia(reviewApi, form.media));

                    Response<Map<String, Object>> response = reviewApi.submitReview(body).execute();
                    if (!response.isSuccessful()) {
                        success = false;
                        if (response.errorBody() != null) {
                            error = parseApiError(response.errorBody().string());
                        }
                        break;
                    }
                    Map<String, Object> responseBody = response.body();
                    if (responseBody != null) {
                        Object singlePoints = responseBody.get("carbonPoints");
                        if (singlePoints instanceof Number) {
                            sessionCarbon += ((Number) singlePoints).intValue();
                        }
                        Object orderPoints = responseBody.get("reviewCarbonPointEarned");
                        if (orderPoints instanceof Number && ((Number) orderPoints).intValue() > 0) {
                            carbonEarned = ((Number) orderPoints).intValue();
                        }
                    }
                }
                if (carbonEarned == 0) {
                    carbonEarned = sessionCarbon;
                }
            } catch (Exception exception) {
                success = false;
                error = exception.getMessage() == null ? error : exception.getMessage();
            }

            boolean finalSuccess = success;
            String finalError = error;
            int finalCarbonEarned = carbonEarned;
            runOnUiThread(() -> {
                submitButton.setEnabled(true);
                submitButton.setText(hasExistingReviews
                        ? getString(R.string.review_order_update)
                        : "Gửi đánh giá");
                if (finalSuccess) {
                    String message;
                    if (hasExistingReviews) {
                        message = getString(R.string.review_update_success);
                    } else if (finalCarbonEarned > 0) {
                        message = "Đã đánh giá thành công và nhận được " + finalCarbonEarned + " điểm carbon";
                    } else {
                        message = "Đã gửi đánh giá thành công";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String parseApiError(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Không thể gửi đánh giá";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.contains("\"message\"")) {
            int start = trimmed.indexOf("\"message\"");
            int colon = trimmed.indexOf(':', start);
            int quoteStart = trimmed.indexOf('"', colon + 1);
            int quoteEnd = trimmed.indexOf('"', quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                return trimmed.substring(quoteStart + 1, quoteEnd);
            }
        }
        return trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed;
    }

    private List<String> uploadReviewMedia(ReviewApi reviewApi, List<ReviewMedia> media) throws Exception {
        List<String> urls = new ArrayList<>();
        if (media.isEmpty()) {
            return urls;
        }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (ReviewMedia item : media) {
            if (item.remoteUrl != null && !item.remoteUrl.trim().isEmpty()) {
                urls.add(item.remoteUrl);
                continue;
            }
            byte[] bytes = readBytes(item.uri);
            RequestBody requestBody = RequestBody.create(MediaType.parse(item.mimeType), bytes);
            parts.add(MultipartBody.Part.createFormData("media", item.fileName, requestBody));
        }

        Response<ReviewMediaUploadResponseDto> response = reviewApi.uploadReviewMedia(parts).execute();
        if (response.isSuccessful() && response.body() != null && response.body().getMedia() != null) {
            urls.addAll(response.body().getMedia());
        }
        return urls;
    }

    private byte[] readBytes(Uri uri) throws Exception {
        InputStream inputStream;
        if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
            inputStream = new FileInputStream(uri.getPath());
        } else {
            inputStream = getContentResolver().openInputStream(uri);
        }
        if (inputStream == null) {
            return new byte[0];
        }
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static class ProductReviewForm {
        final AssetModels.OrderDetailItem item;
        final RatingBar ratingBar;
        final EditText contentInput;
        final TextView charCountView;
        final LinearLayout mediaContainer;
        final View photoContainer;
        final TextView photoLabel;
        final View videoContainer;
        final TextView videoLabel;
        final List<ReviewMedia> media = new ArrayList<>();
        boolean mediaEditable = true;

        ProductReviewForm(
                AssetModels.OrderDetailItem item,
                RatingBar ratingBar,
                EditText contentInput,
                TextView charCountView,
                LinearLayout mediaContainer,
                View photoContainer,
                TextView photoLabel,
                View videoContainer,
                TextView videoLabel
        ) {
            this.item = item;
            this.ratingBar = ratingBar;
            this.contentInput = contentInput;
            this.charCountView = charCountView;
            this.mediaContainer = mediaContainer;
            this.photoContainer = photoContainer;
            this.photoLabel = photoLabel;
            this.videoContainer = videoContainer;
            this.videoLabel = videoLabel;
        }

        void setReadOnly() {
            mediaEditable = false;
            ratingBar.setIsIndicator(true);
            contentInput.setEnabled(false);
            contentInput.setFocusable(false);
            photoContainer.setEnabled(false);
            photoContainer.setClickable(false);
            videoContainer.setEnabled(false);
            videoContainer.setClickable(false);
        }

        void attachCharacterCounter() {
            updateCharacterCounter();
            contentInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateCharacterCounter();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        private void updateCharacterCounter() {
            int count = contentInput.getText() == null ? 0 : contentInput.getText().length();
            charCountView.setText(count + " ký tự");
        }

        int imageCount() {
            int count = 0;
            for (ReviewMedia item : media) {
                if (!item.isVideo()) count++;
            }
            return count;
        }

        int videoCount() {
            int count = 0;
            for (ReviewMedia item : media) {
                if (item.isVideo()) count++;
            }
            return count;
        }

        void renderMedia(Context context) {
            renderMedia(context, mediaEditable);
        }

        void renderMedia(Context context, boolean editable) {
            mediaEditable = editable;
            while (mediaContainer.getChildCount() > 2) {
                mediaContainer.removeViewAt(0);
            }
            int insertIndex = 0;
            for (ReviewMedia item : media) {
                mediaContainer.addView(createMediaThumb(context, item, editable), insertIndex++);
            }
            int imageCount = imageCount();
            int videoCount = videoCount();
            photoContainer.setVisibility(View.VISIBLE);
            videoContainer.setVisibility(View.VISIBLE);
            photoContainer.setAlpha(imageCount >= 3 ? 0.45f : 1f);
            videoContainer.setAlpha(videoCount >= 1 ? 0.45f : 1f);
            photoLabel.setText(imageCount + "/3");
            videoLabel.setText(videoCount + "/1");
        }

        private View createMediaThumb(Context context, ReviewMedia mediaItem, boolean editable) {
            FrameLayout frame = new FrameLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 96), LinearLayout.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(dp(context, 10));
            frame.setLayoutParams(params);

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundResource(R.drawable.bg_order_product_thumb);
            frame.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            if (mediaItem.isVideo()) {
                if (mediaItem.remoteUrl != null && !mediaItem.remoteUrl.trim().isEmpty()) {
                    ReviewMediaHelper.loadThumbnail(context, image, mediaItem.remoteUrl);
                } else {
                    Bitmap thumbnail = getVideoThumbnail(context, mediaItem.uri);
                    if (thumbnail != null) {
                        image.setImageBitmap(thumbnail);
                    } else {
                        image.setImageResource(R.drawable.ic_camera);
                    }
                }
                frame.addView(playOverlay(context));
            } else if (mediaItem.remoteUrl != null && !mediaItem.remoteUrl.trim().isEmpty()) {
                ReviewMediaHelper.loadThumbnail(context, image, mediaItem.remoteUrl);
            } else {
                image.setImageURI(mediaItem.uri);
            }

            if (!editable) {
                frame.setOnClickListener(v -> openMediaItem(context, mediaItem));
            }

            if (editable) {
                TextView removeButton = new TextView(context);
                removeButton.setText("×");
                removeButton.setTextColor(context.getColor(R.color.background_main));
                removeButton.setTextSize(18);
                removeButton.setGravity(android.view.Gravity.CENTER);
                removeButton.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                removeButton.setBackgroundColor(0x99000000);
                FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(dp(context, 24), dp(context, 24));
                removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                frame.addView(removeButton, removeParams);
                removeButton.setOnClickListener(v -> {
                    media.remove(mediaItem);
                    renderMedia(context, editable);
                });
            }

            return frame;
        }

        private Bitmap getVideoThumbnail(Context context, Uri uri) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
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

        private ImageView playOverlay(Context context) {
            ImageView play = new ImageView(context);
            play.setImageResource(R.drawable.ic_play_overlay);
            int size = dp(context, 36);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = android.view.Gravity.CENTER;
            play.setLayoutParams(params);
            return play;
        }

        private void openMediaItem(Context context, ReviewMedia mediaItem) {
            if (mediaItem.remoteUrl != null && !mediaItem.remoteUrl.trim().isEmpty()) {
                ReviewMediaHelper.openMedia(context, mediaItem.remoteUrl);
                return;
            }
            if (mediaItem.uri == null) {
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(mediaItem.uri, mediaItem.mimeType);
                context.startActivity(Intent.createChooser(intent, "Xem media"));
            } catch (Exception exception) {
                Toast.makeText(context, "Không thể mở video/ảnh đánh giá", Toast.LENGTH_SHORT).show();
            }
        }

        private int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }

    private static class ReviewMedia {
        final Uri uri;
        final String fileName;
        final String mimeType;
        final String remoteUrl;

        ReviewMedia(Uri uri, String fileName, String mimeType) {
            this(uri, fileName, mimeType, null);
        }

        ReviewMedia(Uri uri, String fileName, String mimeType, String remoteUrl) {
            this.uri = uri;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.remoteUrl = remoteUrl;
        }

        boolean isVideo() {
            return mimeType != null && mimeType.startsWith("video/");
        }
    }
}
