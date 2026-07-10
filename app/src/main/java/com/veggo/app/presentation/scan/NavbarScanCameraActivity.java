package com.veggo.app.presentation.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.veggo.app.R;
import com.veggo.app.core.utils.CameraCaptureHelper;
import com.veggo.app.presentation.profile.AddFridgeIngredientActivity;
import com.veggo.app.presentation.search.SearchImageSuggestionsActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NavbarScanCameraActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_RECEIPT_MODE = "EXTRA_SCAN_RECEIPT_MODE";
    public static final String EXTRA_DELIVER_RESULT = "EXTRA_DELIVER_RESULT";
    public static final String EXTRA_FROM_NAVBAR = "EXTRA_FROM_NAVBAR";
    public static final String EXTRA_SEARCH_SUGGESTION_MODE = "EXTRA_SEARCH_SUGGESTION_MODE";

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private boolean isReceiptMode = true;
    private boolean isCapturing = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navbar_scan_camera);

        isReceiptMode = getIntent().getBooleanExtra(EXTRA_SCAN_RECEIPT_MODE, true);
        previewView = findViewById(R.id.cameraPreview);
        cameraExecutor = Executors.newSingleThreadExecutor();

        setupUi();
        requestCameraPermissionAndStart();
    }

    private void setupUi() {
        TextView title = findViewById(R.id.scanTitle);
        TextView hint = findViewById(R.id.scanHint);
        View scanFrame = findViewById(R.id.scanFrame);

        if (getIntent().getBooleanExtra(EXTRA_SEARCH_SUGGESTION_MODE, false)) {
            title.setText("Tìm sản phẩm bằng ảnh");
            hint.setText("Chụp hoặc tải ảnh sản phẩm để VEGGO gợi ý sản phẩm phù hợp");
            applyScanFrameSize(scanFrame, 1.2f);
        } else if (isReceiptMode) {
            title.setText(R.string.scan_camera_title_receipt);
            hint.setText(R.string.scan_camera_hint_receipt);
            applyScanFrameSize(scanFrame, 1.35f);
        } else {
            title.setText(R.string.scan_camera_title_ingredient);
            hint.setText(R.string.scan_camera_hint_ingredient);
            applyScanFrameSize(scanFrame, 1.2f);
        }

        findViewById(R.id.scanBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.scanCaptureButton).setOnClickListener(v -> capturePhoto());
        findViewById(R.id.scanUploadButton).setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void applyScanFrameSize(@NonNull View view, float heightToWidthRatio) {
        int horizontalMargin = dp(20);
        int frameWidth = getResources().getDisplayMetrics().widthPixels - horizontalMargin * 2;
        int frameHeight = Math.round(frameWidth * heightToWidthRatio);

        int maxHeight = getResources().getDisplayMetrics().heightPixels - dp(56 + 120 + 40 + 16);
        if (frameHeight > maxHeight) {
            frameHeight = maxHeight;
            if (heightToWidthRatio >= 1f) {
                frameWidth = Math.round(frameHeight / heightToWidthRatio);
            }
        }

        setFrameSize(view, frameWidth, frameHeight);
    }

    private void setFrameSize(@NonNull View view, int widthPx, int heightPx) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = widthPx;
        params.height = heightPx;
        view.setLayoutParams(params);
    }

    private void requestCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            return;
        }
        permissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception exception) {
                Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void capturePhoto() {
        if (imageCapture == null || isCapturing) {
            return;
        }
        isCapturing = true;
        try {
            File imageFile = CameraCaptureHelper.createImageFile(this);
            Uri outputUri = CameraCaptureHelper.getUriForFile(this, imageFile);
            ImageCapture.OutputFileOptions fileOptions =
                    new ImageCapture.OutputFileOptions.Builder(imageFile).build();

            imageCapture.takePicture(
                    fileOptions,
                    cameraExecutor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            runOnUiThread(() -> {
                                isCapturing = false;
                                onImageSelected(outputUri);
                            });
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            runOnUiThread(() -> {
                                isCapturing = false;
                                Toast.makeText(
                                        NavbarScanCameraActivity.this,
                                        R.string.camera_open_failed,
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }
                    }
            );
        } catch (IOException exception) {
            isCapturing = false;
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void onImageSelected(@Nullable Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        if (getIntent().getBooleanExtra(EXTRA_DELIVER_RESULT, false)) {
            Intent result = new Intent();
            if (isReceiptMode) {
                result.putExtra("EXTRA_AI_IMAGE_URI", imageUri.toString());
            } else {
                result.putExtra("EXTRA_AI_INGREDIENT_URI", imageUri.toString());
            }
            result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setResult(RESULT_OK, result);
            finish();
            return;
        }

        if (getIntent().getBooleanExtra(EXTRA_SEARCH_SUGGESTION_MODE, false)) {
            Intent intent = new Intent(this, SearchImageSuggestionsActivity.class);
            intent.putExtra(SearchImageSuggestionsActivity.EXTRA_IMAGE_URI, imageUri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            finish();
            return;
        }

        Intent intent = new Intent(this, AddFridgeIngredientActivity.class);
        if (isReceiptMode) {
            intent.putExtra("EXTRA_AI_IMAGE_URI", imageUri.toString());
        } else {
            intent.putExtra("EXTRA_AI_INGREDIENT_URI", imageUri.toString());
        }
        intent.putExtra("EXTRA_FROM_NAVBAR", getIntent().getBooleanExtra(EXTRA_FROM_NAVBAR, false));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
