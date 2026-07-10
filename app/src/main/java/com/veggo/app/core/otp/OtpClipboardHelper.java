package com.veggo.app.core.otp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Publishes OTP to the system clipboard so keyboards (e.g. Gboard) can suggest it.
 * The user must still tap the keyboard suggestion — nothing is auto-filled in the UI.
 */
public final class OtpClipboardHelper {

    private static final String CLIP_LABEL = "VEGGO OTP";

    private OtpClipboardHelper() {
    }

    public static void publishForKeyboardSuggestion(@NonNull Context context, @NonNull String otp) {
        if (!OtpMessageParser.isValidOtp(otp)) {
            return;
        }
        ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, otp));
    }
}
