package com.veggo.app.core.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.veggo.app.R;

public final class BadgeUiHelper {
    private BadgeUiHelper() {}

    /** Red badge with white text — cart, notifications, wallet, chat bubble, profile order shortcuts. */
    public static void applyAlertBadge(@Nullable TextView badge, int count) {
        if (badge == null) {
            return;
        }
        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(badge.getContext(), R.color.danger_main));
        badge.setBackground(background);
        ViewCompat.setBackgroundTintList(badge, null);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), R.color.white));
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisibility(View.VISIBLE);
        adjustBadgeSize(badge, count);
    }

    /** Yellow badge with black text when selected — tabs on orders, reviews, returns, etc. */
    public static void applyTabBadge(@Nullable TextView badge, int count, boolean selected) {
        if (badge == null) {
            return;
        }
        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setText(String.valueOf(count));
        badge.setVisibility(View.VISIBLE);
        styleTabBadge(badge, selected);
    }

    public static void styleTabBadge(@Nullable TextView badge, boolean selected) {
        if (badge == null) {
            return;
        }
        if (selected) {
            badge.setBackgroundResource(R.drawable.bg_notification_badge_tab);
            ViewCompat.setBackgroundTintList(badge, null);
            badge.setTextColor(ContextCompat.getColor(badge.getContext(), R.color.neutral_100));
        } else {
            badge.setBackgroundResource(R.drawable.bg_notification_badge_dark);
            ViewCompat.setBackgroundTintList(badge, null);
            badge.setTextColor(ContextCompat.getColor(badge.getContext(), R.color.background_main));
        }
    }

    private static void adjustBadgeSize(TextView badge, int count) {
        ViewGroup.LayoutParams params = badge.getLayoutParams();
        if (params == null) {
            return;
        }
        if (count > 9) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            int horizontalPadding = dp(badge, 4);
            int verticalPadding = dp(badge, 1);
            badge.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            badge.setMinWidth(dp(badge, 18));
            badge.setMinHeight(dp(badge, 18));
        } else {
            params.width = dp(badge, 18);
            params.height = dp(badge, 18);
            badge.setPadding(0, 0, 0, 0);
            badge.setMinWidth(0);
            badge.setMinHeight(0);
        }
        badge.requestLayout();
    }

    private static int dp(TextView badge, int value) {
        return Math.round(value * badge.getResources().getDisplayMetrics().density);
    }
}
