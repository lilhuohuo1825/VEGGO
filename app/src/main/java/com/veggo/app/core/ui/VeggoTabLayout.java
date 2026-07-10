package com.veggo.app.core.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        super(context, attrs, R.style.Widget_Veggo_CurvedTabLayout);
        setTabIndicatorAnimationMode(INDICATOR_ANIMATION_MODE_ELASTIC);
    }
}
