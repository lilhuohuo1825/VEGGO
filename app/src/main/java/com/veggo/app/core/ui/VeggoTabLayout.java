package com.veggo.app.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;
import com.veggo.app.R;

/**
 * TabLayout with curved elastic indicator defaults used on category screens.
 */
public class VeggoTabLayout extends TabLayout {

    public VeggoTabLayout(@NonNull Context context) {
        this(context, null);
    }

    public VeggoTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        applyVeggoIndicatorStyle();
    }

    private void applyVeggoIndicatorStyle() {
        Drawable indicator = ContextCompat.getDrawable(getContext(), R.drawable.bg_order_tab_indicator);
        if (indicator != null) {
            setSelectedTabIndicator(indicator);
        }
        setSelectedTabIndicatorColor(ContextCompat.getColor(getContext(), R.color.primary_main));
        setSelectedTabIndicatorHeight(dp(8));
        setTabIndicatorFullWidth(false);
        setTabIndicatorAnimationMode(INDICATOR_ANIMATION_MODE_ELASTIC);
        setTabTextColors(
                ContextCompat.getColor(getContext(), R.color.neutral_60),
                ContextCompat.getColor(getContext(), R.color.primary_main)
        );
        setTabRippleColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
