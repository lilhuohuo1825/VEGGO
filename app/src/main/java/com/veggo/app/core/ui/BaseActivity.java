package com.veggo.app.core.ui;

import android.os.Build;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.core.utils.KeyboardUtils;

import java.util.WeakHashMap;

public abstract class BaseActivity extends AppCompatActivity {
    private static final WeakHashMap<View, int[]> INITIAL_PADDING = new WeakHashMap<>();

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyTopSystemInsetPadding();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyTopSystemInsetPadding();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyTopSystemInsetPadding();
    }

    private void applyTopSystemInsetPadding() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        View root = content.getChildAt(0);
        INITIAL_PADDING.put(root, new int[] {
                root.getPaddingLeft(),
                root.getPaddingTop(),
                root.getPaddingRight(),
                root.getPaddingBottom()
        });
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int[] initial = INITIAL_PADDING.get(view);
            if (initial == null) {
                initial = new int[] {
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                };
                INITIAL_PADDING.put(view, initial);
            }

            int topInset = insets.getSystemWindowInsetTop();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    topInset = Math.max(topInset, cutout.getSafeInsetTop());
                }
            }
            view.setPadding(
                    initial[0],
                    initial[1] + topInset,
                    initial[2],
                    initial[3]
            );
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.handleActivityTouchToHideKeyboard(this, event);
        return super.dispatchTouchEvent(event);
    }
}
