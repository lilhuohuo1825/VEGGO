package com.veggo.app.speech;

import android.content.Context;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.veggo.app.R;

/**
 * Maps Android {@link SpeechRecognizer} error codes to localized user messages.
 * Single responsibility: error translation only.
 */
final class SpeechErrorMapper {

    private SpeechErrorMapper() {
    }

    @NonNull
    static String map(@NonNull Context context, int errorCode) {
        return context.getString(resolveMessageRes(errorCode));
    }

    @StringRes
    private static int resolveMessageRes(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return R.string.speech_error_network;
            case SpeechRecognizer.ERROR_NO_MATCH:
                return R.string.speech_error_no_match;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return R.string.speech_error_speech_timeout;
            case SpeechRecognizer.ERROR_AUDIO:
                return R.string.speech_error_audio;
            case SpeechRecognizer.ERROR_CLIENT:
                return R.string.speech_error_cancelled;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return R.string.speech_error_permission;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return R.string.speech_error_busy;
            case SpeechRecognizer.ERROR_SERVER:
                return R.string.speech_error_server;
            default:
                return R.string.speech_error_unknown;
        }
    }

    static boolean isUserCancellation(int errorCode) {
        return errorCode == SpeechRecognizer.ERROR_CLIENT;
    }

    @Nullable
    static String unavailableMessage(@NonNull Context context) {
        return context.getString(R.string.speech_error_unavailable);
    }

    @Nullable
    static String noNetworkMessage(@NonNull Context context) {
        return context.getString(R.string.speech_error_network);
    }
}
