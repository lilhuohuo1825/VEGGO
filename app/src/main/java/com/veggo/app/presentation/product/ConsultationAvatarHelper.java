package com.veggo.app.presentation.product;

import android.widget.ImageView;

import com.veggo.app.R;
import com.veggo.app.core.utils.UserAvatarHelper;

public final class ConsultationAvatarHelper {
    private static final String ADMIN_DISPLAY_NAME = "VEGGO Admin";

    private ConsultationAvatarHelper() {
    }

    public static void bindUserAvatar(ImageView imageView, String avatarUrl) {
        UserAvatarHelper.bind(imageView, avatarUrl);
    }

    public static void bindAdminAvatar(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        imageView.setImageResource(R.drawable.ic_logo_koten);
    }

    public static String displayName(String name, boolean admin) {
        if (admin) {
            return ADMIN_DISPLAY_NAME;
        }
        return name != null && !name.trim().isEmpty() ? name.trim() : "Khách hàng";
    }
}
