package com.veggo.app.core.utils;

import android.app.DatePickerDialog;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatePickerHelper {
    public static final String BIRTHDAY_PATTERN = "dd/MM/yyyy";

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat(BIRTHDAY_PATTERN, Locale.getDefault());
    private static final String[] PARSE_PATTERNS = {
            BIRTHDAY_PATTERN,
            "d/M/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
    };

    public static void setupDatePicker(AppCompatActivity activity, EditText editText, android.view.View... clickables) {
        editText.setFocusable(false);
        editText.setClickable(true);

        android.view.View.OnClickListener listener = v -> {
            DISPLAY_FORMAT.setLenient(false);

            Calendar selected = Calendar.getInstance();
            String currentText = editText.getText() == null ? "" : editText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                try {
                    Date parsed = DISPLAY_FORMAT.parse(currentText);
                    if (parsed != null) {
                        selected.setTime(parsed);
                    }
                } catch (ParseException ignored) {
                    selected = Calendar.getInstance();
                }
            }

            DatePickerDialog dialog = new DatePickerDialog(
                    activity,
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth);
                        editText.setText(DISPLAY_FORMAT.format(chosen.getTime()));
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

    @Nullable
    public static String formatBirthdayDisplay(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String trimmed = raw.trim();
        DISPLAY_FORMAT.setLenient(false);
        try {
            Date parsed = DISPLAY_FORMAT.parse(trimmed);
            if (parsed != null) {
                return DISPLAY_FORMAT.format(parsed);
            }
        } catch (ParseException ignored) {
            // Try alternate patterns below.
        }

        for (String pattern : PARSE_PATTERNS) {
            if (BIRTHDAY_PATTERN.equals(pattern)) {
                continue;
            }
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                parser.setLenient(false);
                Date parsed = parser.parse(trimmed);
                if (parsed != null) {
                    return DISPLAY_FORMAT.format(parsed);
                }
            } catch (ParseException ignored) {
                // Continue with next pattern.
            }
        }
        return trimmed;
    }
}
