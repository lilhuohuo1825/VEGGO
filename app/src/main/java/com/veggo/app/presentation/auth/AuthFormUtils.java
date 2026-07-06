package com.veggo.app.presentation.auth;

import android.app.Activity;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.veggo.app.R;

import java.util.Locale;
import java.util.Random;

final class AuthFormUtils {
    static final String PHONE_REGEX = "^0\\d{9}$";
    static final int OTP_TTL_MS = 60_000;
    static final int MAX_OTP_ATTEMPTS = 3;
    private static final int AUTH_COVER_HEIGHT_DP = 210;

    private AuthFormUtils() {}

    static void setupAuthScreen(@NonNull Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        View root = content.getChildAt(0);

        final int[] basePadding = {
                root.getPaddingLeft(),
                root.getPaddingTop(),
                root.getPaddingRight(),
                root.getPaddingBottom()
        };

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(basePadding[0], 0, basePadding[2], basePadding[3]);

            resizeCover(activity, R.id.authCoverImage, statusTop);
            resizeCover(activity, R.id.authCoverOverlay, statusTop);

            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private static void resizeCover(@NonNull Activity activity, int viewId, int statusTop) {
        View cover = activity.findViewById(viewId);
        if (cover == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = cover.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = 0;
        }
        layoutParams.height = dp(activity, AUTH_COVER_HEIGHT_DP) + statusTop;
        cover.setLayoutParams(layoutParams);
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    static String phoneError(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Vui lòng nhập số điện thoại";
        }
        if (!phone.trim().matches(PHONE_REGEX)) {
            return "Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số";
        }
        return "";
    }

    static String loginPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Vui lòng nhập mật khẩu";
        }
        return "";
    }

    static String passwordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Vui lòng nhập mật khẩu";
        }
        if (password.length() < 8) {
            return "Mật khẩu phải có tối thiểu 8 ký tự";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Mật khẩu phải có ít nhất 1 chữ cái in hoa";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Mật khẩu phải có ít nhất 1 ký tự đặc biệt";
        }
        return "";
    }

    static boolean showError(TextView errorView, String error) {
        boolean hasError = error != null && !error.isEmpty();
        errorView.setText(hasError ? error : "");
        errorView.setVisibility(hasError ? View.VISIBLE : View.GONE);
        return !hasError;
    }

    static String randomOtp() {
        return String.format(Locale.US, "%06d", new Random().nextInt(1_000_000));
    }

    static void wireOtpFields(@NonNull EditText[] fields, @NonNull Runnable onComplete) {
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < fields.length - 1) {
                            fields[index + 1].requestFocus();
                        } else {
                            onComplete.run();
                        }
                    }
                }
            });
            fields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && fields[index].getText().length() == 0
                        && index > 0) {
                    fields[index - 1].requestFocus();
                }
                return false;
            });
        }
    }
}
