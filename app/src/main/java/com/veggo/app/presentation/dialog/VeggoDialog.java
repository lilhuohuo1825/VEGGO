package com.veggo.app.presentation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.veggo.app.R;

public class VeggoDialog {

    // Interface để lắng nghe sự kiện từ các nút bấm
    public interface DialogListener {
        void onConfirm();
        default void onCancel() {} // Default method (tùy chọn implement)
    }

    public interface CustomDialogListener {
        void onConfirm(Dialog dialog);
        default void onCancel(Dialog dialog) {}
    }

    public static void show(
            Context context,
            Integer iconResId,
            String title,
            String message,
            String confirmText,
            String cancelText,
            DialogListener listener
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggo);
        
        // Đặt nền window là trong suốt để bo góc/hiển thị theo background của layout XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView imgIcon = dialog.findViewById(R.id.imgIcon);
        FrameLayout iconContainer = dialog.findViewById(R.id.iconContainer);
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        configureDialogIcon(context, imgIcon, iconContainer, iconResId);

        // Hiển thị Title
        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        if (message != null) tvMessage.setText(message);
        if (confirmText != null && !confirmText.isEmpty()) btnConfirm.setText(confirmText);

        // Cài đặt nút Cancel
        if (cancelText != null && !cancelText.isEmpty()) {
            btnCancel.setText(cancelText);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        // Gắn sự kiện click
        btnConfirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirm();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel();
            dialog.dismiss();
        });

        dialog.setCancelable(false); // Bắt buộc người dùng chọn
        dialog.show();
    }

    public static void showWithContent(
            Context context,
            Integer iconResId,
            String title,
            String message,
            View contentView,
            String confirmText,
            String cancelText,
            CustomDialogListener listener
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggo);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView imgIcon = dialog.findViewById(R.id.imgIcon);
        FrameLayout iconContainer = dialog.findViewById(R.id.iconContainer);
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        LinearLayout customContentContainer = dialog.findViewById(R.id.customContentContainer);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        configureDialogIcon(context, imgIcon, iconContainer, iconResId);

        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        if (message != null && !message.isEmpty()) {
            tvMessage.setText(message);
            tvMessage.setVisibility(View.VISIBLE);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        if (contentView != null) {
            customContentContainer.removeAllViews();
            customContentContainer.addView(contentView);
            customContentContainer.setVisibility(View.VISIBLE);
        } else {
            customContentContainer.setVisibility(View.GONE);
        }

        if (confirmText != null && !confirmText.isEmpty()) btnConfirm.setText(confirmText);
        if (cancelText != null && !cancelText.isEmpty()) {
            btnCancel.setText(cancelText);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm(dialog);
            } else {
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(dialog);
            dialog.dismiss();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private static void configureDialogIcon(
            Context context,
            ImageView imgIcon,
            FrameLayout iconContainer,
            Integer iconResId
    ) {
        if (iconResId == null) {
            imgIcon.setVisibility(View.GONE);
            return;
        }

        imgIcon.setImageResource(iconResId);
        imgIcon.setVisibility(View.VISIBLE);

        boolean isDangerIcon = iconResId == R.drawable.ic_profile_logout;

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imgIcon.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
        }

        iconContainer.setBackgroundResource(isDangerIcon
                ? R.drawable.bg_dialog_icon_circle_danger
                : R.drawable.bg_dialog_icon_circle);
        ImageViewCompat.setImageTintList(
                imgIcon,
                ContextCompat.getColorStateList(
                        context,
                        isDangerIcon ? R.color.danger_main : R.color.primary_main
                )
        );
        int iconSize = (int) (32f * context.getResources().getDisplayMetrics().density);
        layoutParams.width = iconSize;
        layoutParams.height = iconSize;
        layoutParams.gravity = android.view.Gravity.CENTER;
        imgIcon.setLayoutParams(layoutParams);
    }
}