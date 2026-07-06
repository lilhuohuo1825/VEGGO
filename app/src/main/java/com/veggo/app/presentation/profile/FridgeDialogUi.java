package com.veggo.app.presentation.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;

final class FridgeDialogUi {

    private static final float SIDE_MARGIN_DP = 32f;

    private FridgeDialogUi() {
    }

    static void applyPopupWindowStyle(Window window, Context context) {
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        float density = context.getResources().getDisplayMetrics().density;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = screenWidth - (int) (SIDE_MARGIN_DP * 2 * density);
        window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
    }
}
