package com.veggo.app.core.utils;

import android.app.DatePickerDialog;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerHelper {
    public static void setupDatePicker(AppCompatActivity activity, EditText editText, android.view.View... clickables) {
        editText.setFocusable(false);
        editText.setClickable(true);

        android.view.View.OnClickListener listener = v -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Calendar selected = Calendar.getInstance();
            String currentText = editText.getText() == null ? "" : editText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                try {
                    selected.setTime(sdf.parse(currentText));
                } catch (ParseException ignored) {
                    selected = Calendar.getInstance();
                }
            }

            DatePickerDialog dialog = new DatePickerDialog(
                    activity,
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth);
                        editText.setText(sdf.format(chosen.getTime()));
                    },
                    selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH)
            );
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
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
