package com.veggo.app.speech;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Central manager for Android speech-to-text using {@link SpeechRecognizer}.
 *
 * <p>Encapsulates recognizer lifecycle, {@link RecognizerIntent} configuration,
 * state tracking via {@link VoiceState}, and callback delivery through
 * {@link SpeechCallback}. UI layers must not touch {@link SpeechRecognizer} directly.
 *
 * <p>To swap in a cloud engine later (Google Cloud Speech, Whisper), replace the
 * internal recognition logic while keeping this public API and {@link SpeechCallback}.
 */
public class SpeechManager implements RecognitionListener {

    private final Context applicationContext;
    private final Handler mainHandler;
    private final SpeechCallback callback;

    private SpeechConfig config;
    private SpeechRecognizer speechRecognizer;
    private ActivityResultLauncher<Intent> speechIntentLauncher;
    private VoiceState currentState = VoiceState.IDLE;
    private boolean sessionActive;
    private boolean intentSessionActive;
    private boolean callbacksEnabled = true;

    public SpeechManager(@NonNull Context context, @NonNull SpeechCallback callback) {
        this(context, callback, SpeechConfig.defaultConfig());
    }

    public SpeechManager(
            @NonNull Context context,
            @NonNull SpeechCallback callback,
            @NonNull SpeechConfig config
    ) {
        this.applicationContext = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.callback = callback;
        this.config = config;
    }

    /**
     * Returns the current recognition state.
     */
    @NonNull
    public VoiceState getCurrentState() {
        return currentState;
    }

    /**
     * Returns the locale used for the next recognition session.
     */
    @NonNull
    public Locale getLocale() {
        return config.getLocale();
    }

    /**
     * Updates the recognition locale for subsequent sessions.
     */
    public void setLocale(@NonNull Locale locale) {
        this.config = config.withLocale(locale);
    }

    /**
     * Replaces the full speech configuration (locale, max results, etc.).
     */
    public void setConfig(@NonNull SpeechConfig newConfig) {
        this.config = newConfig;
    }

    /**
     * Required for fallback voice UI on devices where {@link SpeechRecognizer} API is blocked.
     */
    public void setIntentLauncher(@NonNull ActivityResultLauncher<Intent> launcher) {
        this.speechIntentLauncher = launcher;
    }

    public void setCallbacksEnabled(boolean enabled) {
        callbacksEnabled = enabled;
    }

    /**
     * Stops any active session without destroying the recognizer.
     * Safe to call before navigation or when the host activity pauses.
     */
    public void releaseSession() {
        runOnMainThread(this::releaseSessionInternal);
    }

    /**
     * Returns whether any speech input method is available on this device.
     */
    public static boolean isSupported(@NonNull Context context) {
        return SpeechAvailability.isAnyRecognitionAvailable(context);
    }

    /**
     * Handles the result from the system speech recognition activity (fallback mode).
     */
    public void handleActivityResult(int resultCode, @Nullable Intent data) {
        runOnMainThread(() -> handleActivityResultInternal(resultCode, data));
    }

    /**
     * Starts listening for speech input.
     * Must be called from a valid {@link Activity} context while the activity is active.
     */
    public void startListening(@NonNull Activity activity) {
        runOnMainThread(() -> startListeningInternal(activity));
    }

    /**
     * Stops listening and processes any captured audio.
     */
    public void stopListening() {
        runOnMainThread(this::stopListeningInternal);
    }

    /**
     * Cancels the current recognition session without waiting for results.
     */
    public void cancelListening() {
        runOnMainThread(this::cancelListeningInternal);
    }

    /**
     * Releases recognizer resources. Call from {@code onDestroy()} of the host screen.
     */
    public void destroy() {
        runOnMainThread(this::destroyInternal);
    }

    // region RecognitionListener

    @Override
    public void onReadyForSpeech(android.os.Bundle params) {
        updateState(VoiceState.READY);
        notifyReady();
    }

    @Override
    public void onBeginningOfSpeech() {
        updateState(VoiceState.LISTENING);
        notifyListening();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Reserved for future waveform / volume UI feedback.
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // Not used for standard free-form recognition.
    }

    @Override
    public void onEndOfSpeech() {
        updateState(VoiceState.PROCESSING);
        notifyProcessing();
    }

    @Override
    public void onError(int error) {
        sessionActive = false;
        updateState(VoiceState.ERROR);

        if (!SpeechErrorMapper.isUserCancellation(error)) {
            notifyError(SpeechErrorMapper.map(applicationContext, error));
        }

        notifyStop();
        resetToIdleAfterSession();
    }

    @Override
    public void onResults(android.os.Bundle results) {
        sessionActive = false;
        String text = extractBestResult(results);
        updateState(VoiceState.PROCESSING);

        if (text != null && !text.trim().isEmpty()) {
            notifyResult(text.trim());
        } else {
            notifyError(SpeechErrorMapper.map(applicationContext, SpeechRecognizer.ERROR_NO_MATCH));
        }

        notifyStop();
        resetToIdleAfterSession();
    }

    @Override
    public void onPartialResults(android.os.Bundle partialResults) {
        String text = extractBestResult(partialResults);
        if (text != null && !text.trim().isEmpty()) {
            notifyPartialResult(text.trim());
        }
    }

    @Override
    public void onEvent(int eventType, android.os.Bundle params) {
        // No-op for standard recognition events.
    }

    // endregion

    private void startListeningInternal(@NonNull Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            return;
        }

        if (!SpeechAvailability.isAnyRecognitionAvailable(applicationContext)) {
            deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            return;
        }

        if (!isNetworkAvailable()) {
            deliverError(SpeechErrorMapper.noNetworkMessage(applicationContext));
            return;
        }

        if (sessionActive) {
            cancelListeningInternal();
        }

        if (SpeechAvailability.isApiRecognitionAvailable(applicationContext)) {
            startApiRecognition(activity);
        } else {
            startIntentRecognition(activity);
        }
    }

    private void startApiRecognition(@NonNull Activity activity) {
        if (!ensureRecognizer()) {
            if (SpeechAvailability.isIntentRecognitionAvailable(applicationContext)) {
                startIntentRecognition(activity);
            } else {
                deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            }
            return;
        }

        try {
            speechRecognizer.startListening(buildRecognizerIntent());
            sessionActive = true;
            intentSessionActive = false;
        } catch (IllegalStateException | SecurityException exception) {
            sessionActive = false;
            if (SpeechAvailability.isIntentRecognitionAvailable(applicationContext)) {
                startIntentRecognition(activity);
            } else {
                deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            }
        }
    }

    private void startIntentRecognition(@NonNull Activity activity) {
        if (speechIntentLauncher == null) {
            deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            return;
        }

        ComponentName recognitionActivity =
                SpeechAvailability.findRecognitionActivity(applicationContext);
        if (recognitionActivity == null) {
            deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
            return;
        }

        try {
            Intent intent = buildRecognizerIntent();
            intent.setComponent(recognitionActivity);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    applicationContext.getString(R.string.chat_voice_hint_listening));
            sessionActive = true;
            intentSessionActive = true;
            updateState(VoiceState.READY);
            notifyReady();
            speechIntentLauncher.launch(intent);
        } catch (Exception exception) {
            sessionActive = false;
            intentSessionActive = false;
            deliverError(SpeechErrorMapper.unavailableMessage(applicationContext));
        }
    }

    private void handleActivityResultInternal(int resultCode, @Nullable Intent data) {
        if (!intentSessionActive) {
            return;
        }

        intentSessionActive = false;
        sessionActive = false;

        if (resultCode == Activity.RESULT_OK && data != null) {
            String text = extractBestResultFromIntent(data);
            if (text != null && !text.trim().isEmpty()) {
                updateState(VoiceState.PROCESSING);
                notifyProcessing();
                notifyResult(text.trim());
                notifyStop();
                resetToIdleAfterSession();
                return;
            }
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            notifyStop();
            resetToIdleAfterSession();
            return;
        }

        notifyError(SpeechErrorMapper.map(applicationContext, SpeechRecognizer.ERROR_NO_MATCH));
        notifyStop();
        resetToIdleAfterSession();
    }

    private void stopListeningInternal() {
        if (intentSessionActive || speechRecognizer == null || !sessionActive) {
            return;
        }

        try {
            speechRecognizer.stopListening();
        } catch (IllegalStateException ignored) {
            sessionActive = false;
            resetToIdleAfterSession();
        }
    }

    private void cancelListeningInternal() {
        if (intentSessionActive) {
            intentSessionActive = false;
            sessionActive = false;
            notifyStop();
            resetToIdleAfterSession();
            return;
        }

        if (speechRecognizer == null || !sessionActive) {
            return;
        }

        try {
            speechRecognizer.cancel();
        } catch (IllegalStateException ignored) {
            sessionActive = false;
            notifyStop();
            resetToIdleAfterSession();
        }
    }

    private void releaseSessionInternal() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (IllegalStateException ignored) {
                // Ignore stale recognizer state during navigation.
            }
        }
        sessionActive = false;
        intentSessionActive = false;
        updateState(VoiceState.IDLE);
    }

    private void destroyInternal() {
        callbacksEnabled = false;
        sessionActive = false;
        intentSessionActive = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.setRecognitionListener(null);
            } catch (Exception ignored) {
                // Best-effort detach before destroy.
            }
            try {
                speechRecognizer.cancel();
            } catch (IllegalStateException ignored) {
                // Best-effort cleanup before destroy.
            }
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        updateState(VoiceState.IDLE);
    }

    private boolean ensureRecognizer() {
        if (speechRecognizer != null) {
            return true;
        }

        ComponentName service = SpeechAvailability.findRecognitionService(applicationContext);
        if (service != null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext, service);
        } else if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext);
        } else {
            return false;
        }

        if (speechRecognizer == null) {
            return false;
        }

        speechRecognizer.setRecognitionListener(this);
        return true;
    }

    @NonNull
    private Intent buildRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.getLocale().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.isPartialResultsEnabled());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, config.getMaxResults());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.getPackageName());
        return intent;
    }

    private void deliverError(@Nullable String message) {
        updateState(VoiceState.ERROR);
        if (message != null && !message.isEmpty()) {
            notifyError(message);
        }
        notifyStop();
        resetToIdleAfterSession();
    }

    private void resetToIdleAfterSession() {
        updateState(VoiceState.IDLE);
    }

    private void updateState(@NonNull VoiceState newState) {
        currentState = newState;
    }

    @Nullable
    private String extractBestResult(@Nullable android.os.Bundle results) {
        if (results == null) {
            return null;
        }

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            return null;
        }

        return matches.get(0);
    }

    @Nullable
    private String extractBestResultFromIntent(@NonNull Intent data) {
        ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void runOnMainThread(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    private void notifyReady() {
        if (!callbacksEnabled) {
            return;
        }
        callback.onReady();
    }

    private void notifyListening() {
        if (!callbacksEnabled) {
            return;
        }
        callback.onListening();
    }

    private void notifyProcessing() {
        if (!callbacksEnabled) {
            return;
        }
        callback.onProcessing();
    }

    private void notifyPartialResult(@NonNull String text) {
        if (!callbacksEnabled) {
            return;
        }
        callback.onPartialResult(text);
    }

    private void notifyResult(@NonNull String text) {
        if (!callbacksEnabled) {
            return;
        }
        callback.onResult(text);
    }

    private void notifyError(@NonNull String message) {
        if (!callbacksEnabled) {
            return;
        }
        callback.onError(message);
    }

    private void notifyStop() {
        if (!callbacksEnabled) {
            return;
        }
        callback.onVoiceStop();
    }
}
