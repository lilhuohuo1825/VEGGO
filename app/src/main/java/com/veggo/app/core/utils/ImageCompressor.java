package com.veggo.app.core.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageCompressor {
    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 85;

    private ImageCompressor() {
    }

    @NonNull
    public static File compressToJpeg(@NonNull Context context, @NonNull Uri sourceUri) throws IOException {
        Bitmap decoded = decodeSampledBitmap(context, sourceUri);
        if (decoded == null) {
            throw new IOException("Unable to decode image");
        }

        Bitmap scaled = scaleDown(decoded);
        if (scaled != decoded) {
            decoded.recycle();
        }

        File output = new File(context.getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                throw new IOException("Unable to compress image");
            }
        } finally {
            scaled.recycle();
        }
        return output;
    }

    @NonNull
    public static File compressToJpeg(@NonNull Context context, @NonNull Bitmap bitmap) throws IOException {
        Bitmap scaled = scaleDown(bitmap);
        File output = new File(context.getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                throw new IOException("Unable to compress image");
            }
        } finally {
            if (scaled != bitmap) {
                scaled.recycle();
            }
        }
        return output;
    }

    @Nullable
    private static Bitmap decodeSampledBitmap(@NonNull Context context, @NonNull Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        readBitmap(context, uri, bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, MAX_DIMENSION, MAX_DIMENSION);
        return readBitmap(context, uri, options);
    }

    @Nullable
    private static Bitmap readBitmap(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull BitmapFactory.Options options
    ) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @NonNull
    private static Bitmap scaleDown(@NonNull Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = Math.min(1f, Math.min((float) MAX_DIMENSION / width, (float) MAX_DIMENSION / height));
        if (scale >= 1f) {
            return source;
        }

        int targetWidth = Math.round(width * scale);
        int targetHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }
}
