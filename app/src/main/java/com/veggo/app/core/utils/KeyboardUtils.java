package com.veggo.app.core.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class KeyboardUtils {

    private KeyboardUtils() {
    }

    public static void hideKeyboard(@Nullable View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        View focused = activity.getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
            hideKeyboard(focused);
        }
    }

    public static void handleActivityTouchToHideKeyboard(@NonNull Activity activity, @NonNull MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return;
        }
        hideKeyboardIfTouchOutsideFocusedEditText(activity.getCurrentFocus(), event);
    }

    public static void setupHideKeyboardOnOutsideTap(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        View decorView = window.getDecorView();
        decorView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboardIfTouchOutsideFocusedEditText(decorView.findFocus(), event);
            }
            return false;
        });
    }

    private static void hideKeyboardIfTouchOutsideFocusedEditText(
            @Nullable View focusedView,
            @NonNull MotionEvent event
    ) {
        if (!(focusedView instanceof EditText)) {
            return;
        }

        Rect focusedRect = new Rect();
        focusedView.getGlobalVisibleRect(focusedRect);
        if (!focusedRect.contains((int) event.getRawX(), (int) event.getRawY())) {
            focusedView.clearFocus();
            hideKeyboard(focusedView);
        }
    }
}
