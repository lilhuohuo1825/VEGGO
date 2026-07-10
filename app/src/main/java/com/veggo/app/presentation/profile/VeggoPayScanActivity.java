package com.veggo.app.presentation.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.veggo.app.R;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VeggoPayScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    @Nullable
    private Camera camera;
    private boolean torchEnabled = false;
    private boolean qrHandled = false;

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
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::scanQrFromGallery);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_scan);

        previewView = findViewById(R.id.cameraPreview);
        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
        );

        setupUi();
        requestCameraPermissionAndStart();
    }

    private void setupUi() {
        View scanFrame = findViewById(R.id.scanFrame);
        applyScanFrameSize(scanFrame, 1f);

        View laserBeam = findViewById(R.id.laserBeam);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f
        );
        animation.setDuration(3000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        laserBeam.startAnimation(animation);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFlash).setOnClickListener(v -> toggleTorch());
        findViewById(R.id.btnGallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void applyScanFrameSize(@NonNull View view, float heightToWidthRatio) {
        int horizontalMargin = dp(20);
        int frameWidth = getResources().getDisplayMetrics().widthPixels - horizontalMargin * 2;
        int frameHeight = Math.round(frameWidth * heightToWidthRatio);

        int maxHeight = getResources().getDisplayMetrics().heightPixels - dp(56 + 100 + 40 + 16);
        if (frameHeight > maxHeight) {
            frameHeight = maxHeight;
            frameWidth = Math.round(frameHeight / heightToWidthRatio);
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = frameWidth;
        params.height = frameHeight;
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

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (qrHandled || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(this::onBarcodesDetected)
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onBarcodesDetected(@NonNull List<Barcode> barcodes) {
        if (qrHandled) {
            return;
        }
        for (Barcode barcode : barcodes) {
            String rawValue = barcode.getRawValue();
            if (rawValue != null && !rawValue.trim().isEmpty()) {
                qrHandled = true;
                runOnUiThread(() -> handleQrPayload(rawValue));
                return;
            }
        }
    }

    private void scanQrFromGallery(@Nullable Uri imageUri) {
        if (imageUri == null) {
            return;
        }
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.trim().isEmpty()) {
                                handleQrPayload(rawValue);
                                return;
                            }
                        }
                        Toast.makeText(this, R.string.veggopay_qr_not_found, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(error ->
                            Toast.makeText(this, R.string.veggopay_qr_not_found, Toast.LENGTH_SHORT).show()
                    );
        } catch (IOException exception) {
            Toast.makeText(this, R.string.veggopay_qr_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleQrPayload(@NonNull String rawValue) {
        VeggoPayQrParser.Payload payload = VeggoPayQrParser.parse(rawValue);
        if (payload == null || payload.phone == null || payload.phone.trim().isEmpty()) {
            qrHandled = false;
            Toast.makeText(this, R.string.veggopay_qr_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, VeggoPayTransferActivity.class);
        intent.putExtra("recipientPhone", payload.phone);
        if (payload.amount != null && !payload.amount.trim().isEmpty()) {
            intent.putExtra("amount", payload.amount);
        }
        if (payload.description != null && !payload.description.trim().isEmpty()) {
            intent.putExtra("description", payload.description);
        }
        startActivity(intent);
        finish();
    }

    private void toggleTorch() {
        if (camera == null || camera.getCameraInfo() == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, R.string.veggopay_scan_flash_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        torchEnabled = !torchEnabled;
        camera.getCameraControl().enableTorch(torchEnabled);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        super.onDestroy();
    }
}
