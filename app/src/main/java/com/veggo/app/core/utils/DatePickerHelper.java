package com.veggo.app.core.utils;

import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DatePickerHelper {
    public static void setupDatePicker(AppCompatActivity activity, EditText editText, android.view.View... clickables) {
        editText.setFocusable(false);
        editText.setClickable(true);
        
        android.view.View.OnClickListener listener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Chọn ngày")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedDate = sdf.format(new Date(selection));
                editText.setText(formattedDate);
            });

            datePicker.show(activity.getSupportFragmentManager(), "DATE_PICKER");
        };

        editText.setOnClickListener(listener);
        if (clickables != null) {
            for (android.view.View view : clickables) {
                if (view != null) {
                    view.setOnClickListener(listener);
                }
            }
        }
    }
}
