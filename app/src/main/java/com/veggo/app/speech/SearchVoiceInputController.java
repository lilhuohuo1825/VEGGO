package com.veggo.app.speech;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.veggo.app.R;

/**
 * Reusable voice search controller with the same UI/UX as {@link com.veggo.app.presentation.chatbot.ChatbotActivity}.
 */
public final class SearchVoiceInputController implements SpeechCallback {

    private final AppCompatActivity activity;
    private final EditText searchInput;
    private final Runnable onQueryUpdated;
    private final View layoutVoiceStatus;
    private final View viewVoicePulse;
    private final TextView tvVoiceStatus;
    private final TextView tvVoiceHint;
    private final ProgressBar progressVoice;
    private final FrameLayout btnMic;
    private final ImageView imgMic;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SpeechManager speechManager;
    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<Intent> speechIntentLauncher;
    private boolean voiceSessionActive;
    private AnimatorSet pulseAnimator;
    private boolean callbacksEnabled = true;

    private SearchVoiceInputController(
            @NonNull AppCompatActivity activity,
            @NonNull ActivityResultCaller caller,
            @NonNull View searchBarRoot,
            @NonNull EditText searchInput,
            @Nullable Runnable onQueryUpdated
    ) {
        this.activity = activity;
        this.searchInput = searchInput;
        this.onQueryUpdated = onQueryUpdated;
        this.layoutVoiceStatus = searchBarRoot.findViewById(R.id.layoutVoiceStatus);
        this.viewVoicePulse = searchBarRoot.findViewById(R.id.viewVoicePulse);
        this.tvVoiceStatus = searchBarRoot.findViewById(R.id.tvVoiceStatus);
        this.tvVoiceHint = searchBarRoot.findViewById(R.id.tvVoiceHint);
        this.progressVoice = searchBarRoot.findViewById(R.id.progressVoice);
        this.btnMic = searchBarRoot.findViewById(R.id.btnMic);
        this.imgMic = searchBarRoot.findViewById(R.id.imgMic);

        recordAudioPermissionLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isActive()) {
                        return;
                    }
                    if (Boolean.TRUE.equals(granted)) {
                        startVoiceInput();
                    } else {
                        Toast.makeText(
                                activity,
                                R.string.chat_voice_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
        speechIntentLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (speechManager != null) {
                        speechManager.handleActivityResult(result.getResultCode(), result.getData());
                    }
                }
        );

        speechManager = new SpeechManager(activity, this);
        speechManager.setIntentLauncher(speechIntentLauncher);

        if (btnMic != null) {
            btnMic.setOnClickListener(v -> onMicClicked());
        }
    }

    public static SearchVoiceInputController attach(
            @NonNull Fragment fragment,
            @NonNull View searchBarRoot,
            @NonNull EditText searchInput,
            @Nullable Runnable onQueryUpdated
    ) {
        return new SearchVoiceInputController(
                (AppCompatActivity) fragment.requireActivity(),
                fragment,
                searchBarRoot,
                searchInput,
                onQueryUpdated
        );
    }

    public static SearchVoiceInputController attach(
            @NonNull AppCompatActivity activity,
            @NonNull View searchBarRoot,
            @NonNull EditText searchInput,
            @Nullable Runnable onQueryUpdated
    ) {
        return new SearchVoiceInputController(activity, activity, searchBarRoot, searchInput, onQueryUpdated);
    }

    public void startVoiceInputIfPermitted() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void onMicClicked() {
        VoiceState state = speechManager.getCurrentState();
        if (state == VoiceState.READY
                || state == VoiceState.LISTENING
                || state == VoiceState.PROCESSING) {
            speechManager.cancelListening();
            return;
        }

        startVoiceInputIfPermitted();
    }

    private void startVoiceInput() {
        voiceSessionActive = true;
        searchInput.setText("");
        showVoiceStatus(
                R.string.chat_voice_status_preparing,
                R.string.chat_voice_hint_cancel,
                false
        );
        speechManager.startListening(activity);
    }

    @Override
    public void onReady() {
        if (!isActive()) {
            return;
        }
        showVoiceStatus(
                R.string.chat_voice_status_listening,
                R.string.chat_voice_hint_cancel,
                true
        );
        updateMicActiveUi(true);
    }

    @Override
    public void onListening() {
        if (!isActive()) {
            return;
        }
        showVoiceStatus(
                R.string.chat_voice_status_listening,
                R.string.chat_voice_hint_cancel,
                true
        );
    }

    @Override
    public void onProcessing() {
        if (!isActive()) {
            return;
        }
        showVoiceStatus(
                R.string.chat_voice_status_processing,
                R.string.chat_voice_hint_processing,
                false
        );
        if (progressVoice != null) {
            progressVoice.setVisibility(View.VISIBLE);
        }
        stopPulseAnimation();
        updateMicActiveUi(false);
    }

    @Override
    public void onPartialResult(String text) {
        if (!isActive() || !voiceSessionActive || TextUtils.isEmpty(text)) {
            return;
        }
        searchInput.setText(text);
        searchInput.setSelection(text.length());
        if (tvVoiceHint != null) {
            tvVoiceHint.setText(activity.getString(R.string.chat_voice_preview_format, text));
        }
    }

    @Override
    public void onResult(String text) {
        if (!isActive() || TextUtils.isEmpty(text)) {
            return;
        }
        searchInput.setText(text);
        searchInput.setSelection(text.length());
        hideVoiceStatus();
        if (onQueryUpdated != null) {
            onQueryUpdated.run();
        }
    }

    @Override
    public void onError(String message) {
        if (!isActive()) {
            return;
        }
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
        resetVoiceUi();
    }

    @Override
    public void onVoiceStop() {
        if (!isActive()) {
            return;
        }
        resetVoiceUi();
    }

    public void onResume() {
        callbacksEnabled = true;
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(true);
        }
    }

    public void onPause() {
        callbacksEnabled = false;
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(false);
            speechManager.releaseSession();
        }
        voiceSessionActive = false;
        mainHandler.removeCallbacksAndMessages(null);
        stopPulseAnimation();
        hideVoiceUiQuietly();
    }

    public void release() {
        onPause();
        stopPulseAnimation();
        mainHandler.removeCallbacksAndMessages(null);
        if (speechManager != null) {
            speechManager.destroy();
            speechManager = null;
        }
    }

    private void showVoiceStatus(int statusRes, int hintRes, boolean animatePulse) {
        if (!isActive() || layoutVoiceStatus == null) {
            return;
        }
        layoutVoiceStatus.setVisibility(View.VISIBLE);
        if (tvVoiceStatus != null) {
            tvVoiceStatus.setText(activity.getString(statusRes));
        }
        if (tvVoiceHint != null) {
            tvVoiceHint.setText(activity.getString(hintRes));
        }
        if (progressVoice != null) {
            progressVoice.setVisibility(animatePulse ? View.GONE : View.VISIBLE);
        }
        if (animatePulse) {
            startPulseAnimation();
        } else {
            stopPulseAnimation();
        }
    }

    private void hideVoiceStatus() {
        voiceSessionActive = false;
        if (!isActive() || layoutVoiceStatus == null) {
            return;
        }
        layoutVoiceStatus.setVisibility(View.GONE);
        if (progressVoice != null) {
            progressVoice.setVisibility(View.GONE);
        }
        stopPulseAnimation();
        updateMicActiveUi(false);
    }

    private void resetVoiceUi() {
        mainHandler.removeCallbacksAndMessages(null);
        hideVoiceStatus();
    }

    private void hideVoiceUiQuietly() {
        if (layoutVoiceStatus != null) {
            layoutVoiceStatus.setVisibility(View.GONE);
        }
        if (progressVoice != null) {
            progressVoice.setVisibility(View.GONE);
        }
        updateMicActiveUi(false);
    }

    private void updateMicActiveUi(boolean active) {
        if (!isActive() || btnMic == null || imgMic == null) {
            return;
        }
        btnMic.setBackgroundResource(active ? R.drawable.bg_voice_mic_active : R.drawable.bg_home_mic);
        imgMic.setColorFilter(ContextCompat.getColor(
                activity,
                active ? R.color.white : R.color.neutral_80
        ));
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        if (!isActive() || viewVoicePulse == null || btnMic == null) {
            return;
        }

        ObjectAnimator dotScaleX = ObjectAnimator.ofFloat(viewVoicePulse, View.SCALE_X, 1f, 1.5f);
        ObjectAnimator dotScaleY = ObjectAnimator.ofFloat(viewVoicePulse, View.SCALE_Y, 1f, 1.5f);
        ObjectAnimator dotAlpha = ObjectAnimator.ofFloat(viewVoicePulse, View.ALPHA, 1f, 0.45f);
        dotScaleX.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleY.setRepeatCount(ValueAnimator.INFINITE);
        dotAlpha.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleX.setRepeatMode(ValueAnimator.REVERSE);
        dotScaleY.setRepeatMode(ValueAnimator.REVERSE);
        dotAlpha.setRepeatMode(ValueAnimator.REVERSE);
        dotScaleX.setDuration(700);
        dotScaleY.setDuration(700);
        dotAlpha.setDuration(700);
        dotScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        dotScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        dotAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator micScaleX = ObjectAnimator.ofFloat(btnMic, View.SCALE_X, 1f, 1.08f);
        ObjectAnimator micScaleY = ObjectAnimator.ofFloat(btnMic, View.SCALE_Y, 1f, 1.08f);
        micScaleX.setRepeatCount(ValueAnimator.INFINITE);
        micScaleY.setRepeatCount(ValueAnimator.INFINITE);
        micScaleX.setRepeatMode(ValueAnimator.REVERSE);
        micScaleY.setRepeatMode(ValueAnimator.REVERSE);
        micScaleX.setDuration(700);
        micScaleY.setDuration(700);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(dotScaleX, dotScaleY, dotAlpha, micScaleX, micScaleY);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (!isActive()) {
            return;
        }
        if (viewVoicePulse != null) {
            viewVoicePulse.setScaleX(1f);
            viewVoicePulse.setScaleY(1f);
            viewVoicePulse.setAlpha(1f);
        }
        if (btnMic != null) {
            btnMic.setScaleX(1f);
            btnMic.setScaleY(1f);
        }
    }

    private boolean isActive() {
        if (!callbacksEnabled || activity.isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed();
    }
}
