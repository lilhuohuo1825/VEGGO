package com.veggo.app.core.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.veggo.app.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ReviewMediaHelper {

    private static final String PLAY_OVERLAY_TAG = "review_play_overlay";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ReviewMediaHelper() {
    }

    public static boolean isVideoUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.US);
        return lower.contains(".mp4")
                || lower.contains(".mov")
                || lower.contains(".webm")
                || lower.contains(".m4v")
                || lower.contains(".3gp");
    }

    @Nullable
    public static String resolveUrl(@Nullable String url) {
        return UserAvatarHelper.resolveUrl(url);
    }

    public static void loadThumbnail(
            @NonNull Context context,
            @NonNull ImageView target,
            @Nullable String rawUrl
    ) {
        String url = resolveUrl(rawUrl);
        if (url == null || url.isEmpty()) {
            target.setImageResource(R.drawable.ic_leaf);
            return;
        }
        if (isVideoUrl(url)) {
            target.setImageResource(R.drawable.ic_leaf);
            loadVideoThumbnailAsync(target, url);
            return;
        }
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_leaf)
                .error(R.drawable.ic_leaf)
                .into(target);
    }

    public static void bindMediaCard(
            @NonNull View card,
            @NonNull ImageView imageView,
            @Nullable String rawUrl
    ) {
        String url = resolveUrl(rawUrl);
        if (url == null || url.isEmpty()) {
            card.setVisibility(View.GONE);
            card.setOnClickListener(null);
            return;
        }
        card.setVisibility(View.VISIBLE);
        boolean isVideo = isVideoUrl(url);
        loadThumbnail(card.getContext(), imageView, url);
        updatePlayOverlay(card, isVideo);
        card.setOnClickListener(v -> openMedia(v.getContext(), url));
    }

    public static void openMedia(@NonNull Context context, @Nullable String rawUrl) {
        String url = resolveUrl(rawUrl);
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), isVideoUrl(url) ? "video/*" : "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, isVideoUrl(url) ? "Xem video" : "Xem ảnh"));
        } catch (Exception exception) {
            Toast.makeText(context, "Không thể mở video/ảnh đánh giá", Toast.LENGTH_SHORT).show();
        }
    }

    public static Bitmap loadVideoThumbnail(@Nullable String rawUrl) {
        String url = resolveUrl(rawUrl);
        if (url == null || url.isEmpty()) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Map<String, String> headers = new HashMap<>();
            retriever.setDataSource(url, headers);
            return retriever.getFrameAtTime(0);
        } catch (Exception exception) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static void loadVideoThumbnailAsync(@NonNull ImageView target, @NonNull String url) {
        new Thread(() -> {
            Bitmap frame = loadVideoThumbnail(url);
            MAIN_HANDLER.post(() -> {
                if (frame != null) {
                    target.setImageBitmap(frame);
                } else {
                    target.setImageResource(R.drawable.ic_camera);
                }
            });
        }).start();
    }

    private static void updatePlayOverlay(@NonNull View card, boolean show) {
        if (!(card instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) card;
        View overlay = group.findViewWithTag(PLAY_OVERLAY_TAG);
        if (!show) {
            if (overlay != null) {
                overlay.setVisibility(View.GONE);
            }
            return;
        }
        if (overlay == null) {
            ImageView play = new ImageView(group.getContext());
            play.setTag(PLAY_OVERLAY_TAG);
            play.setImageResource(R.drawable.ic_play_overlay);
            int size = dp(group.getContext(), 36);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER;
            group.addView(play, params);
        } else {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
