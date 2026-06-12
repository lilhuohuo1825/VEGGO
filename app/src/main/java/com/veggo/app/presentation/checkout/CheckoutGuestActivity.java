package com.veggo.app.presentation.checkout;

import android.content.Intent;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

import java.util.Calendar;
import java.util.Locale;

public class CheckoutGuestActivity extends BaseActivity {
    private TextView tvCheckoutNote;
    private TextView tvScheduleDeliveryTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_guest);

        View layoutNote = findViewById(R.id.layoutNote);
        View layoutVoucher = findViewById(R.id.layoutVoucher);
        View layoutScheduleDelivery = findViewById(R.id.layoutScheduleDelivery);
        View btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        tvCheckoutNote = findViewById(R.id.tvCheckoutNote);
        tvScheduleDeliveryTime = findViewById(R.id.tvScheduleDeliveryTime);
        tvCheckoutNote.setText("");
        tvCheckoutNote.setHint("Nhập ghi chú");

        layoutVoucher.setOnClickListener(v -> showPopup(R.layout.dialog_voucher));
        layoutNote.setOnClickListener(v -> showNoteDialog());
        layoutScheduleDelivery.setOnClickListener(v -> showScheduleTimePicker());
        btnPlaceOrder.setOnClickListener(v -> startActivity(new Intent(this, QrPaymentActivity.class)));
    }

    private void showPopup(int layoutResId) {
        View dialogView = LayoutInflater.from(this).inflate(layoutResId, null, false);
        createBottomSheetDialog(dialogView).show();
    }

    private void showNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note, null, false);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        TextView btnSave = dialogView.findViewById(R.id.btnSave);

        CharSequence currentNote = tvCheckoutNote.getText();
        if (currentNote != null && currentNote.length() > 0) {
            edtNote.setText(currentNote.toString());
            edtNote.setSelection(edtNote.getText().length());
        }

        BottomSheetDialog dialog = createBottomSheetDialog(dialogView);
        btnSave.setOnClickListener(v -> {
            tvCheckoutNote.setText(edtNote.getText().toString().trim());
            dialog.dismiss();
        });
        dialog.show();
    }

    private BottomSheetDialog createBottomSheetDialog(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                bottomSheet.setPadding(0, 0, 0, 0);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    private void showScheduleTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int initialHour = calendar.get(Calendar.HOUR_OF_DAY);
        int initialMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> tvScheduleDeliveryTime.setText(buildScheduleText(hourOfDay, minute)),
                initialHour,
                initialMinute,
                true
        );
        timePickerDialog.show();
    }

    private String buildScheduleText(int startHour, int startMinute) {
        int endHour = (startHour + 2) % 24;
        return String.format(
                Locale.getDefault(),
                "Dự kiến nhận hàng hôm nay, %02d:%02d - %02d:%02d",
                startHour,
                startMinute,
                endHour,
                startMinute
        );
    }
}
