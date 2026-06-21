package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veggo.app.R;

final class TasteTagDialog {
    private TasteTagDialog() {
    }

    static void show(Context context, TastePreferenceStore store, TastePreferenceStore.TasteTag tag, Runnable onChanged) {
        Dialog dialog = new Dialog(context);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_taste_dialog);
        panel.setPadding(dp(context, 22), dp(context, 20), dp(context, 22), dp(context, 18));

        TextView title = new TextView(context);
        title.setText(tag == null ? "Thêm tag cần tránh" : "Sửa tag cần tránh");
        title.setTextColor(context.getColor(R.color.neutral_100));
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        panel.addView(title, matchWrap());

        EditText labelInput = new EditText(context);
        labelInput.setHint("Ví dụ: ca cao, hải sản, sữa");
        labelInput.setSingleLine(true);
        labelInput.setText(tag == null ? "" : tag.label);
        labelInput.setTextColor(context.getColor(R.color.neutral_100));
        labelInput.setHintTextColor(context.getColor(R.color.neutral_60));
        labelInput.setTextSize(15);
        labelInput.setBackgroundResource(R.drawable.bg_taste_dialog_input);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 48)
        );
        inputParams.setMargins(0, dp(context, 18), 0, 0);
        panel.addView(labelInput, inputParams);

        final String[] action = {tag == null ? TastePreferenceStore.ACTION_BLOCK : tag.action};
        final boolean[] enabled = {tag == null || tag.enabled};
        TextView actionChip = dialogChip(context, "");
        TextView enabledChip = dialogChip(context, "");
        Runnable renderChips = () -> {
            actionChip.setText(actionLabel(action[0]));
            styleTagChip(context, actionChip, action[0], true);
            enabledChip.setText(enabled[0] ? "Đang bật" : "Đang tắt");
            enabledChip.setBackgroundResource(enabled[0] ? R.drawable.bg_taste_chip_green : R.drawable.bg_taste_chip_green_outline);
            enabledChip.setTextColor(context.getColor(enabled[0] ? R.color.background_main : R.color.neutral_70));
        };
        actionChip.setOnClickListener(v -> {
            if (TastePreferenceStore.ACTION_BLOCK.equals(action[0])) {
                action[0] = TastePreferenceStore.ACTION_ALLERGY;
            } else if (TastePreferenceStore.ACTION_ALLERGY.equals(action[0])) {
                action[0] = TastePreferenceStore.ACTION_WARN;
            } else {
                action[0] = TastePreferenceStore.ACTION_BLOCK;
            }
            renderChips.run();
        });
        enabledChip.setOnClickListener(v -> {
            enabled[0] = !enabled[0];
            renderChips.run();
        });
        renderChips.run();

        LinearLayout chipRow = new LinearLayout(context);
        chipRow.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams chipRowParams = matchWrap();
        chipRowParams.setMargins(0, dp(context, 14), 0, 0);
        panel.addView(chipRow, chipRowParams);
        chipRow.addView(actionChip);
        chipRow.addView(enabledChip);

        LinearLayout buttons = new LinearLayout(context);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonRowParams = matchWrap();
        buttonRowParams.setMargins(0, dp(context, 18), 0, 0);
        panel.addView(buttons, buttonRowParams);

        if (tag != null) {
            TextView delete = actionButton(context, "Xóa", R.drawable.bg_taste_danger_button, R.color.danger_main);
            delete.setOnClickListener(v -> {
                store.removeTag(tag.label);
                if (onChanged != null) onChanged.run();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(context, 44), 1f);
            deleteParams.setMarginEnd(dp(context, 8));
            buttons.addView(delete, deleteParams);
        }

        TextView cancel = actionButton(context, "Hủy", R.drawable.bg_order_cancel_button, R.color.primary_main);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(context, 44), 1f);
        cancelParams.setMarginEnd(dp(context, 8));
        buttons.addView(cancel, cancelParams);

        TextView save = actionButton(context, "Lưu", R.drawable.bg_order_primary_button, R.color.background_main);
        save.setOnClickListener(v -> {
            String label = labelInput.getText().toString().trim();
            if (label.isEmpty()) {
                labelInput.setError("Nhập tên tag");
                return;
            }
            store.upsertTag(tag == null ? null : tag.label, label, action[0], label, enabled[0]);
            if (onChanged != null) onChanged.run();
            dialog.dismiss();
        });
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(context, 44), 1f));

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel);
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(
                        Math.min(context.getResources().getDisplayMetrics().widthPixels - dp(context, 32), dp(context, 360)),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
        });
        dialog.show();
    }

    private static String actionLabel(String action) {
        if (TastePreferenceStore.ACTION_ALLERGY.equals(action)) return "Dị ứng";
        if (TastePreferenceStore.ACTION_WARN.equals(action)) return "Cảnh báo";
        return "Không ăn";
    }

    private static TextView dialogChip(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextSize(14);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 42)
        );
        params.setMargins(0, 0, 0, dp(context, 8));
        view.setLayoutParams(params);
        return view;
    }

    private static TextView actionButton(Context context, String text, int background, int textColor) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(14);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setBackgroundResource(background);
        view.setTextColor(context.getColor(textColor));
        return view;
    }

    private static void styleTagChip(Context context, TextView view, String action, boolean enabled) {
        if (!enabled) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_green_outline);
            view.setTextColor(context.getColor(R.color.neutral_70));
        } else if (TastePreferenceStore.ACTION_WARN.equals(action)) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_yellow);
            view.setTextColor(context.getColor(R.color.secondary_hover));
        } else {
            view.setBackgroundResource(R.drawable.bg_taste_chip_red);
            view.setTextColor(context.getColor(R.color.danger_main));
        }
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
