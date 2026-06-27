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

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
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
    private String orderId;
    private ProductReviewForm activeMediaForm;
    private boolean activeMediaIsVideo;
    private ActivityResultLauncher<String> mediaPicker;
    private ActivityResultLauncher<PickVisualMediaRequest> multiImagePicker;
    private ActivityResultLauncher<Void> imageCameraPicker;
    private ActivityResultLauncher<Intent> videoCameraPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);

        orderId = getIntent().getStringExtra(AssetScreenData.EXTRA_ORDER_ID);
        formsContainer = findViewById(R.id.reviewOrderProductForms);
        submitButton = findViewById(R.id.reviewOrderSubmitButton);

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
        imageCameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null && activeMediaForm != null) {
                if (activeMediaForm.imageCount() >= 3) {
                    Toast.makeText(this, "Chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri uri = saveBitmapToCache(bitmap);
                if (uri != null) {
                    activeMediaForm.media.add(new ReviewMedia(uri, "review-camera.jpg", "image/jpeg"));
                    activeMediaForm.renderMedia(this);
                }
            }
        });
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
            runOnUiThread(() -> bindForms(detail));
        }).start();
    }

    private void bindForms(AssetModels.OrderDetail detail) {
        forms.clear();
        formsContainer.removeAllViews();

        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm trong đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
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
            form.photoContainer.setOnClickListener(v -> showMediaSourceDialog(form, false));
            form.videoContainer.setOnClickListener(v -> showMediaSourceDialog(form, true));
            form.renderMedia(this);
            form.attachCharacterCounter();
            forms.add(form);
            formsContainer.addView(formView);
        }
    }

    private void showMediaSourceDialog(ProductReviewForm form, boolean isVideo) {
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
                imageCameraPicker.launch(null);
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
        if (item.image != null && !item.image.trim().isEmpty()) {
            Glide.with(this).load(item.image).placeholder(R.drawable.ic_vegetable).into(image);
        }
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
            try {
                ReviewApi reviewApi = ApiClient.createService(ReviewApi.class);
                for (ProductReviewForm form : forms) {
                    Map<String, Object> body = new HashMap<>();
                    body.put("sku", form.item.sku);
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
                            error = response.errorBody().string();
                        }
                        break;
                    }
                }
            } catch (Exception exception) {
                success = false;
                error = exception.getMessage() == null ? error : exception.getMessage();
            }

            boolean finalSuccess = success;
            String finalError = error;
            runOnUiThread(() -> {
                submitButton.setEnabled(true);
                submitButton.setText("Gửi đánh giá");
                if (finalSuccess) {
                    Toast.makeText(this,
                            "Đã đánh giá thành công và nhận được " + (forms.size() * 2) + " điểm carbon",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private List<String> uploadReviewMedia(ReviewApi reviewApi, List<ReviewMedia> media) throws Exception {
        List<String> urls = new ArrayList<>();
        if (media.isEmpty()) {
            return urls;
        }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (ReviewMedia item : media) {
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

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "review-camera-" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream);
            }
            return Uri.fromFile(file);
        } catch (Exception exception) {
            Toast.makeText(this, "Không thể xử lý ảnh đã chụp", Toast.LENGTH_SHORT).show();
            return null;
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
            while (mediaContainer.getChildCount() > 2) {
                mediaContainer.removeViewAt(0);
            }
            int insertIndex = 0;
            for (ReviewMedia item : media) {
                mediaContainer.addView(createMediaThumb(context, item), insertIndex++);
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

        private View createMediaThumb(Context context, ReviewMedia mediaItem) {
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
                Bitmap thumbnail = getVideoThumbnail(context, mediaItem.uri);
                if (thumbnail != null) {
                    image.setImageBitmap(thumbnail);
                } else {
                    image.setImageResource(R.drawable.ic_camera);
                }
                frame.addView(overlayLabel(context, "Video"));
            } else {
                image.setImageURI(mediaItem.uri);
            }

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
                renderMedia(context);
            });

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

        private TextView overlayLabel(Context context, String text) {
            TextView label = new TextView(context);
            label.setText(text);
            label.setTextColor(context.getColor(R.color.background_main));
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

        private int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }

    private static class ReviewMedia {
        final Uri uri;
        final String fileName;
        final String mimeType;

        ReviewMedia(Uri uri, String fileName, String mimeType) {
            this.uri = uri;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }

        boolean isVideo() {
            return mimeType != null && mimeType.startsWith("video/");
        }
    }
}
