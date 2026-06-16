package com.veggo.app.presentation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.veggo.app.R;

public class VeggoDialog {

    // Interface để lắng nghe sự kiện từ các nút bấm
    public interface DialogListener {
        void onConfirm();
        default void onCancel() {} // Default method (tùy chọn implement)
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
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        // Hiển thị và cài đặt Icon
        if (iconResId != null) {
            imgIcon.setImageResource(iconResId);
            imgIcon.setVisibility(View.VISIBLE);
        } else {
            imgIcon.setVisibility(View.GONE);
        }

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
}