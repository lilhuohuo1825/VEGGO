package com.veggo.app.presentation.community;

import android.graphics.Color;
import android.widget.ImageButton;

import com.veggo.app.R;

final class ToggleUi {
    private ToggleUi() {
    }

    static void bindToggle(ImageButton button, int inactiveIcon, int activeIcon, boolean active) {
        button.setTag(active);
        render(button, inactiveIcon, activeIcon, active);
        button.setOnClickListener(v -> {
            boolean next = !(Boolean) v.getTag();
            v.setTag(next);
            render((ImageButton) v, inactiveIcon, activeIcon, next);
        });
    }

    private static void render(ImageButton button, int inactiveIcon, int activeIcon, boolean active) {
        button.setBackgroundResource(active ? R.drawable.bg_follow_button_green : R.drawable.bg_community_circle);
        button.setImageResource(active ? activeIcon : inactiveIcon);
        if (active) {
            button.setColorFilter(Color.WHITE);
        } else {
            button.clearColorFilter();
        }
    }
}
