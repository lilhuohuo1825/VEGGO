package com.veggo.app.core.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Shared OTP/PIN input behavior used across VeggoPay and profile flows.
 */
public final class OtpInputHelper {

    private OtpInputHelper() {
    }

    public static void setupAutoShift(EditText[] fields) {
        setupAutoShift(fields, null);
    }

    public static void setupAutoShift(EditText[] fields, @Nullable View confirmButton) {
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            final EditText field = fields[i];
            if (field == null) {
                continue;
            }

            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        if (index < fields.length - 1) {
                            EditText nextField = fields[index + 1];
                            if (nextField != null) {
                                nextField.requestFocus();
                            }
                        } else if (confirmButton != null) {
                            confirmButton.performClick();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            field.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (field.getText().length() == 0 && index > 0) {
                        EditText previousField = fields[index - 1];
                        if (previousField != null) {
                            previousField.requestFocus();
                            previousField.setText("");
                        }
                        return true;
                    }
                }
                return false;
            });
        }
    }

    public static void clearFields(EditText[] fields) {
        for (EditText field : fields) {
            if (field != null) {
                field.setText("");
            }
        }
    }

    public static void focusFirst(EditText[] fields) {
        if (fields.length > 0 && fields[0] != null) {
            fields[0].requestFocus();
        }
    }

    public static String readValue(EditText[] fields) {
        StringBuilder builder = new StringBuilder();
        for (EditText field : fields) {
            if (field != null) {
                builder.append(field.getText().toString().trim());
            }
        }
        return builder.toString();
    }

    public static void fillFields(@NonNull EditText[] fields, @NonNull String otp) {
        if (otp.length() != fields.length) {
            return;
        }
        syncFields(fields, otp);
        if (fields.length > 0 && fields[fields.length - 1] != null) {
            fields[fields.length - 1].requestFocus();
        }
    }

    public static void syncFields(@NonNull EditText[] fields, @NonNull String value) {
        String digits = value.replaceAll("\\D", "");
        for (int i = 0; i < fields.length; i++) {
            EditText field = fields[i];
            if (field == null) {
                continue;
            }
            if (i < digits.length()) {
                field.setText(String.valueOf(digits.charAt(i)));
            } else {
                field.setText("");
            }
        }
    }

    public static void configureAutofillHints(@NonNull EditText[] fields) {
        // Keyboard autofill is handled by OtpAutoFillHelper's dedicated target field.
    }
}
