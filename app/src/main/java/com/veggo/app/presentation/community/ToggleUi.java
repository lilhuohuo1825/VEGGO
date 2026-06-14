package com.veggo.app.presentation.community;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.veggo.app.R;

final class ToggleUi {
    private ToggleUi() {
    }

    static void bindToggle(View button, int inactiveIcon, int activeIcon, boolean active) {
        button.setTag(active);
        render(button, inactiveIcon, activeIcon, active);
        button.setOnClickListener(v -> {
            boolean next = !(Boolean) v.getTag();
            v.setTag(next);
            render(v, inactiveIcon, activeIcon, next);
        });
    }

    static void renderSelected(View button, int iconRes, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_community_circle_green : R.drawable.bg_community_circle);
        ImageView icon = findIcon(button);
        if (icon == null) {
            return;
        }
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? Color.WHITE : Color.TRANSPARENT);
    }

    private static void render(View button, int inactiveIcon, int activeIcon, boolean active) {
        button.setBackgroundResource(active ? R.drawable.bg_community_circle_green : R.drawable.bg_community_circle);
        ImageView icon = findIcon(button);
        if (icon == null) {
            return;
        }
        icon.setImageResource(active ? activeIcon : inactiveIcon);
        if (active) {
            icon.setColorFilter(Color.WHITE);
        } else {
            icon.clearColorFilter();
        }
    }

    private static ImageView findIcon(View view) {
        if (view instanceof ImageView) {
            return (ImageView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ImageView icon = findIcon(group.getChildAt(i));
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }
}
