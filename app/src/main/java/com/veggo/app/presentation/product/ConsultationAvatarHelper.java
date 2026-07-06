package com.veggo.app.presentation.product;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.veggo.app.R;

public final class ConsultationAvatarHelper {
    private static final String ADMIN_DISPLAY_NAME = "VEGGO Admin";

    private ConsultationAvatarHelper() {
    }

    public static void bindUserAvatar(ImageView imageView, String avatarUrl) {
        if (imageView == null) {
            return;
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.color.neutral_40)
                    .error(R.color.neutral_40)
                    .into(imageView);
        } else {
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setImageResource(R.drawable.ic_profile_avatar);
        }
    }

    public static void bindAdminAvatar(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        Glide.with(imageView.getContext()).clear(imageView);
        imageView.setImageResource(R.drawable.ic_logo_koten);
    }

    public static String displayName(String name, boolean admin) {
        if (admin) {
            return ADMIN_DISPLAY_NAME;
        }
        return name != null && !name.trim().isEmpty() ? name.trim() : "Khách hàng";
    }
}
