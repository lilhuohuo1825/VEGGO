package com.veggo.app.core.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.veggo.app.R;

import java.io.File;
import java.io.IOException;

/**
 * Opens the device camera with runtime permission handling and a FileProvider-backed output URI.
 * Prefer this over {@link ActivityResultContracts.TakePicturePreview}, which can crash without
 * CAMERA permission on many devices.
 */
public final class CameraCaptureHelper {

    public interface Listener {
        void onImageCaptured(@NonNull Uri imageUri);

        default void onPermissionDenied() {
        }

        default void onCaptureCancelled() {
        }
    }

    private final ComponentActivity activity;
    private final ActivityResultLauncher<Uri> takePictureLauncher;
    private final ActivityResultLauncher<String> permissionLauncher;
    @Nullable
    private Listener listener;
    @Nullable
    private Uri pendingUri;

    public CameraCaptureHelper(@NonNull ComponentActivity activity) {
        this.activity = activity;
        takePictureLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    Listener currentListener = listener;
                    Uri capturedUri = pendingUri;
                    pendingUri = null;
                    listener = null;
                    if (currentListener == null) {
                        return;
                    }
                    if (Boolean.TRUE.equals(success) && capturedUri != null) {
                        currentListener.onImageCaptured(capturedUri);
                    } else {
                        currentListener.onCaptureCancelled();
                    }
                }
        );
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                        launchCameraInternal();
                        return;
                    }
                    if (listener != null) {
                        listener.onPermissionDenied();
                    } else {
                        Toast.makeText(activity, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                    }
                    listener = null;
                    pendingUri = null;
                }
        );
    }

    public void openCamera(@NonNull Listener listener) {
        this.listener = listener;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraInternal();
            return;
        }
        permissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void launchCameraInternal() {
        try {
            pendingUri = createImageUri(activity);
            takePictureLauncher.launch(pendingUri);
        } catch (IOException exception) {
            pendingUri = null;
            listener = null;
            Toast.makeText(activity, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    public static Uri createImageUri(@NonNull Context context) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "camera");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Unable to create camera cache directory");
        }
        File imageFile = File.createTempFile("capture_", ".jpg", cacheDir);
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                imageFile
        );
    }
}
