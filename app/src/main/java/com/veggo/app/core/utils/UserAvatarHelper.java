package com.veggo.app.core.utils;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.veggo.app.R;

public final class UserAvatarHelper {

    private UserAvatarHelper() {
    }

    @Nullable
    public static String resolveUrl(@Nullable String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        String url = avatarUrl.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = Constants.API_BASE_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/api")) {
            base = base.substring(0, base.length() - 4);
        }
        if (url.startsWith("/")) {
            return base + url;
        }
        return base + "/" + url;
    }

    public static void bind(ImageView imageView, @Nullable String avatarUrl) {
        if (imageView == null) {
            return;
        }
        String resolved = resolveUrl(avatarUrl);
        if (resolved == null || resolved.isEmpty()) {
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setImageResource(R.drawable.ic_profile_avatar);
            return;
        }
        Glide.with(imageView.getContext())
                .load(resolved)
                .placeholder(R.drawable.ic_profile_avatar)
                .error(R.drawable.ic_profile_avatar)
                .circleCrop()
                .into(imageView);
    }
}
