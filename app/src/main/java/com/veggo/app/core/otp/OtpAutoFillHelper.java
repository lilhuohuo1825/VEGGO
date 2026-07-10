package com.veggo.app.core.otp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.veggo.app.core.utils.OtpInputHelper;

/**
 * Surfaces OTP on the system keyboard (Gboard / Autofill) for the user to tap.
 * Does not auto-fill — input only arrives after the user selects a keyboard suggestion
 * or types/pastes manually into the hidden autofill target field.
 */
public final class OtpAutoFillHelper implements DefaultLifecycleObserver {

    private static final String AUTOFILL_HINT_SMS_OTP = "smsOTPCode";

    private final Activity activity;
    private final EditText[] otpFields;

    private boolean active;
    private boolean syncingFromKeyboardTarget;
    @Nullable
    private EditText keyboardTarget;
    @Nullable
    private TextWatcher keyboardTargetWatcher;
    @Nullable
    private BroadcastReceiver smsConsentReceiver;
    private OtpNotificationStore.Listener notificationListener;

    private OtpAutoFillHelper(@NonNull Activity activity, @NonNull EditText[] otpFields) {
        this.activity = activity;
        this.otpFields = otpFields;
        this.notificationListener = entry ->
                activity.runOnUiThread(() -> {
                    if (!active) {
                        return;
                    }
                    OtpClipboardHelper.publishForKeyboardSuggestion(activity, entry.otp);
                    notifyAutofillChanged();
                });
    }

    @NonNull
    public static OtpAutoFillHelper create(
            @NonNull Activity activity,
            @NonNull EditText[] otpFields
    ) {
        return new OtpAutoFillHelper(activity, otpFields);
    }

    @NonNull
    public static OtpAutoFillHelper attach(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Activity activity,
            @NonNull EditText[] otpFields
    ) {
        OtpAutoFillHelper helper = new OtpAutoFillHelper(activity, otpFields);
        lifecycleOwner.getLifecycle().addObserver(helper);
        return helper;
    }

    public void start() {
        if (otpFields.length == 0 || otpFields[0] == null) {
            return;
        }
        stop();
        active = true;
        setupKeyboardTarget();
        redirectVisibleFieldFocus();
        OtpNotificationStore.addListener(notificationListener);

        OtpNotificationStore.Entry pending = OtpNotificationStore.consumeLatest(
                OtpNotificationStore.DEFAULT_TTL_MS
        );
        if (pending != null) {
            OtpClipboardHelper.publishForKeyboardSuggestion(activity, pending.otp);
        }

        startSmsUserConsent();
        focusKeyboardTarget();
    }

    public void stop() {
        if (!active && keyboardTarget == null && smsConsentReceiver == null) {
            return;
        }
        active = false;
        OtpNotificationStore.removeListener(notificationListener);
        unregisterSmsConsentReceiver();
        removeKeyboardTarget();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (active) {
            startSmsUserConsent();
            focusKeyboardTarget();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        unregisterSmsConsentReceiver();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        stop();
        owner.getLifecycle().removeObserver(this);
    }

    private void setupKeyboardTarget() {
        EditText anchor = otpFields[0];
        ViewGroup parent = findOtpContainer(anchor);
        if (parent == null) {
            return;
        }

        keyboardTarget = new EditText(activity);
        keyboardTarget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        keyboardTarget.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        keyboardTarget.setSingleLine(true);
        keyboardTarget.setCursorVisible(false);
        keyboardTarget.setBackgroundColor(0x00000000);
        keyboardTarget.setAlpha(0f);
        keyboardTarget.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyboardTarget.setAutofillHints(AUTOFILL_HINT_SMS_OTP);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(1, 1);
        parent.addView(keyboardTarget, 0, params);

        keyboardTargetWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!active || syncingFromKeyboardTarget) {
                    return;
                }
                String digits = s.toString().replaceAll("\\D", "");
                if (!digits.equals(s.toString())) {
                    syncingFromKeyboardTarget = true;
                    keyboardTarget.setText(digits);
                    keyboardTarget.setSelection(digits.length());
                    syncingFromKeyboardTarget = false;
                    return;
                }
                syncingFromKeyboardTarget = true;
                OtpInputHelper.syncFields(otpFields, digits);
                syncingFromKeyboardTarget = false;
            }
        };
        keyboardTarget.addTextChangedListener(keyboardTargetWatcher);
    }

    private void redirectVisibleFieldFocus() {
        View.OnClickListener focusTarget = v -> focusKeyboardTarget();
        for (EditText field : otpFields) {
            if (field == null) {
                continue;
            }
            field.setOnClickListener(focusTarget);
            field.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    focusKeyboardTarget();
                }
            });
        }
    }

    private void focusKeyboardTarget() {
        if (!active || keyboardTarget == null) {
            return;
        }
        keyboardTarget.post(() -> {
            keyboardTarget.requestFocus();
            notifyAutofillChanged();
        });
    }

    private void notifyAutofillChanged() {
        if (keyboardTarget == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        AutofillManager autofillManager = activity.getSystemService(AutofillManager.class);
        if (autofillManager != null && autofillManager.isEnabled()) {
            autofillManager.notifyValueChanged(keyboardTarget);
        }
    }

    private void startSmsUserConsent() {
        unregisterSmsConsentReceiver();
        SmsRetrieverClient client = SmsRetriever.getClient(activity);
        client.startSmsUserConsent(null)
                .addOnSuccessListener(ignored -> registerSmsConsentReceiver())
                .addOnFailureListener(ignored -> {
                });
    }

    private void registerSmsConsentReceiver() {
        if (smsConsentReceiver != null) {
            return;
        }
        smsConsentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction()) || !active) {
                    return;
                }
                Status status = intent.getParcelableExtra(SmsRetriever.EXTRA_STATUS);
                if (status == null || status.getStatusCode() != CommonStatusCodes.SUCCESS) {
                    return;
                }
                String message = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                String otp = OtpMessageParser.extract(message);
                if (otp == null) {
                    return;
                }
                if (message != null) {
                    OtpNotificationStore.save(message, otp);
                }
                activity.runOnUiThread(() -> {
                    OtpClipboardHelper.publishForKeyboardSuggestion(activity, otp);
                    focusKeyboardTarget();
                });
            }
        };

        IntentFilter filter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(smsConsentReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(smsConsentReceiver, filter);
        }
    }

    private void unregisterSmsConsentReceiver() {
        if (smsConsentReceiver == null) {
            return;
        }
        try {
            activity.unregisterReceiver(smsConsentReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        smsConsentReceiver = null;
    }

    @Nullable
    private ViewGroup findOtpContainer(@Nullable EditText anchor) {
        if (anchor == null) {
            return null;
        }
        View parent = (View) anchor.getParent();
        if (parent instanceof ViewGroup) {
            return (ViewGroup) parent;
        }
        return null;
    }

    private void removeKeyboardTarget() {
        if (keyboardTarget == null) {
            return;
        }
        if (keyboardTargetWatcher != null) {
            keyboardTarget.removeTextChangedListener(keyboardTargetWatcher);
            keyboardTargetWatcher = null;
        }
        ViewGroup parent = (ViewGroup) keyboardTarget.getParent();
        if (parent != null) {
            parent.removeView(keyboardTarget);
        }
        keyboardTarget = null;
    }
}
