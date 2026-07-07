package com.veggo.app.presentation.support;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.veggo.app.R;
import com.veggo.app.adapter.SupportChatAdapter;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.network.SupportSocketManager;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.data.remote.api.AuthApi;
import com.veggo.app.data.remote.api.SupportApi;
import com.veggo.app.data.remote.dto.AccessTokenResponseDto;
import com.veggo.app.data.remote.dto.SupportConversationDto;
import com.veggo.app.data.remote.dto.SupportConversationsResponseDto;
import com.veggo.app.data.remote.dto.SupportMessageDto;
import com.veggo.app.data.remote.dto.SupportMessagesResponseDto;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.remote.request.FirebaseLoginRequest;
import com.veggo.app.data.remote.request.RefreshAccessTokenRequest;
import com.veggo.app.databinding.ActivitySupportChatBinding;
import com.veggo.app.domain.model.ChatMessage;
import com.veggo.app.speech.SpeechCallback;
import com.veggo.app.speech.SpeechManager;
import com.veggo.app.speech.VoiceState;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportChatActivity extends AppCompatActivity implements SpeechCallback {

    private static final long VOICE_SEND_DELAY_MS = 800L;

    private ActivitySupportChatBinding binding;
    private SupportChatAdapter adapter;
    private SupportApi supportApi;
    private AuthApi authApi;
    private SupportSocketManager socketManager;
    private SpeechManager speechManager;
    private AppPreferences appPreferences;
    private PreferencesManager preferencesManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private String conversationId;
    private boolean socketConnected = false;
    private boolean sessionReady = false;
    private boolean pendingVoiceSend;
    private boolean voiceSessionActive;
    private AnimatorSet pulseAnimator;
    private boolean isLocalTyping = false;
    private boolean peerTypingVisible = false;
    private final Runnable typingStopRunnable = () -> emitTypingState(false);
    private AnimatorSet typingDotsAnimator;
    private View dotTyping1;
    private View dotTyping2;
    private View dotTyping3;

    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<Intent> speechIntentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupportChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appPreferences = new AppPreferences(this);
        preferencesManager = new PreferencesManager(this);

        recordAudioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isUiActive()) return;
                    if (Boolean.TRUE.equals(granted)) {
                        startVoiceInput();
                    } else {
                        Toast.makeText(this, R.string.chat_voice_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );
        speechIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (speechManager != null) {
                        speechManager.handleActivityResult(result.getResultCode(), result.getData());
                    }
                }
        );

        speechManager = new SpeechManager(this, this);
        speechManager.setIntentLauncher(speechIntentLauncher);

        adapter = new SupportChatAdapter();
        binding.rvChat.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChat.setAdapter(adapter);

        supportApi = ApiClient.createService(SupportApi.class);
        authApi = ApiClient.createService(AuthApi.class);
        socketManager = new SupportSocketManager();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> submitInputText());
        binding.btnMic.setOnClickListener(v -> onMicClicked());
        binding.edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitInputText();
                return true;
            }
            return false;
        });
        setupTypingViews();
        setupTypingListener();

        ensureAccessTokenThenStart();
    }

    private void setupTypingViews() {
        View typingRoot = binding.layoutTypingIndicator.getRoot();
        dotTyping1 = typingRoot.findViewById(R.id.dotTyping1);
        dotTyping2 = typingRoot.findViewById(R.id.dotTyping2);
        dotTyping3 = typingRoot.findViewById(R.id.dotTyping3);
    }

    private void setupTypingListener() {
        binding.edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleInputTypingChanged(s != null && s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void handleInputTypingChanged(boolean hasText) {
        if (!socketConnected || TextUtils.isEmpty(conversationId)) return;
        mainHandler.removeCallbacks(typingStopRunnable);
        if (!hasText) {
            emitTypingState(false);
            return;
        }
        emitTypingState(true);
        mainHandler.postDelayed(typingStopRunnable, 2000L);
    }

    private void emitTypingState(boolean isTyping) {
        if (isLocalTyping == isTyping || TextUtils.isEmpty(conversationId)) return;
        isLocalTyping = isTyping;
        socketManager.emitTypingUpdate(conversationId, isTyping);
    }

    private void updatePeerTypingIndicator(boolean visible) {
        if (!isUiActive()) return;
        peerTypingVisible = visible;
        binding.layoutTypingIndicator.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            startTypingDotAnimation();
            scrollToBottom();
        } else {
            stopTypingDotAnimation();
        }
    }

    private void startTypingDotAnimation() {
        stopTypingDotAnimation();
        if (dotTyping1 == null || dotTyping2 == null || dotTyping3 == null) return;

        ObjectAnimator d1 = createTypingDotAnimator(dotTyping1, 0L);
        ObjectAnimator d2 = createTypingDotAnimator(dotTyping2, 160L);
        ObjectAnimator d3 = createTypingDotAnimator(dotTyping3, 320L);
        typingDotsAnimator = new AnimatorSet();
        typingDotsAnimator.playTogether(d1, d2, d3);
        typingDotsAnimator.start();
    }

    private ObjectAnimator createTypingDotAnimator(View dot, long startDelay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, View.ALPHA, 0.25f, 1f);
        animator.setDuration(500L);
        animator.setStartDelay(startDelay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private void stopTypingDotAnimation() {
        if (typingDotsAnimator != null) {
            typingDotsAnimator.cancel();
            typingDotsAnimator = null;
        }
        if (dotTyping1 != null) dotTyping1.setAlpha(1f);
        if (dotTyping2 != null) dotTyping2.setAlpha(1f);
        if (dotTyping3 != null) dotTyping3.setAlpha(1f);
    }

    private void submitInputText() {
        String text = binding.edtMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            sendMessage(text);
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceInput() {
        binding.edtMessage.setText("");
        voiceSessionActive = true;
        showVoiceStatus(
                R.string.chat_voice_status_preparing,
                R.string.chat_voice_hint_cancel,
                false
        );
        speechManager.startListening(this);
    }

    @Override
    public void onReady() {
        if (!isUiActive()) return;
        showVoiceStatus(
                R.string.chat_voice_status_listening,
                R.string.chat_voice_hint_listening,
                true
        );
        updateMicActiveUi(true);
    }

    @Override
    public void onListening() {
        if (!isUiActive()) return;
        showVoiceStatus(
                R.string.chat_voice_status_listening,
                R.string.chat_voice_hint_cancel,
                true
        );
    }

    @Override
    public void onProcessing() {
        if (!isUiActive()) return;
        showVoiceStatus(
                R.string.chat_voice_status_processing,
                R.string.chat_voice_hint_processing,
                false
        );
        binding.progressVoice.setVisibility(View.VISIBLE);
        stopPulseAnimation();
        updateMicActiveUi(false);
    }

    @Override
    public void onPartialResult(String text) {
        if (!isUiActive() || !voiceSessionActive || TextUtils.isEmpty(text)) return;
        binding.edtMessage.setText(text);
        binding.edtMessage.setSelection(text.length());
        binding.tvVoiceHint.setText(getString(R.string.chat_voice_preview_format, text));
    }

    @Override
    public void onResult(String text) {
        if (!isUiActive() || TextUtils.isEmpty(text)) return;

        pendingVoiceSend = true;
        binding.edtMessage.setText(text);
        binding.edtMessage.setSelection(text.length());
        binding.progressVoice.setVisibility(View.VISIBLE);
        showVoiceStatus(
                R.string.chat_voice_status_recognized,
                R.string.chat_voice_preview_format,
                text,
                false
        );

        mainHandler.postDelayed(() -> {
            if (!isUiActive()) return;
            showVoiceStatus(
                    R.string.chat_voice_status_sending,
                    R.string.chat_voice_preview_format,
                    text,
                    false
            );
            mainHandler.postDelayed(() -> {
                pendingVoiceSend = false;
                hideVoiceStatus();
                sendMessage(text);
            }, 350L);
        }, VOICE_SEND_DELAY_MS);
    }

    @Override
    public void onError(String message) {
        if (!isUiActive()) return;
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        resetVoiceUi();
    }

    @Override
    public void onVoiceStop() {
        if (!isUiActive() || pendingVoiceSend) return;
        resetVoiceUi();
    }

    private void showVoiceStatus(int statusRes, int hintRes, boolean animatePulse) {
        showVoiceStatus(statusRes, hintRes, null, animatePulse);
    }

    private void showVoiceStatus(int statusRes, int hintRes, @Nullable String hintArg, boolean animatePulse) {
        if (!isUiActive()) return;
        binding.layoutVoiceStatus.setVisibility(View.VISIBLE);
        binding.tvVoiceStatus.setText(getString(statusRes));
        if (hintArg == null) {
            binding.tvVoiceHint.setText(getString(hintRes));
        } else {
            binding.tvVoiceHint.setText(getString(hintRes, hintArg));
        }
        binding.progressVoice.setVisibility(animatePulse ? View.GONE : View.VISIBLE);
        if (animatePulse) {
            startPulseAnimation();
        } else {
            stopPulseAnimation();
        }
    }

    private void hideVoiceStatus() {
        voiceSessionActive = false;
        if (!isUiActive()) return;
        binding.layoutVoiceStatus.setVisibility(View.GONE);
        binding.progressVoice.setVisibility(View.GONE);
        binding.edtMessage.setHint(R.string.chat_input_hint);
        stopPulseAnimation();
        updateMicActiveUi(false);
    }

    private void resetVoiceUi() {
        pendingVoiceSend = false;
        mainHandler.removeCallbacksAndMessages(null);
        hideVoiceStatus();
    }

    private void updateMicActiveUi(boolean active) {
        if (!isUiActive()) return;
        binding.btnMic.setBackgroundResource(active ? R.drawable.bg_voice_mic_active : R.drawable.bg_home_mic);
        binding.imgMic.setColorFilter(ContextCompat.getColor(
                this,
                active ? R.color.white : R.color.neutral_80
        ));
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        if (!isUiActive()) return;

        ObjectAnimator dotScaleX = ObjectAnimator.ofFloat(binding.viewVoicePulse, View.SCALE_X, 1f, 1.5f);
        ObjectAnimator dotScaleY = ObjectAnimator.ofFloat(binding.viewVoicePulse, View.SCALE_Y, 1f, 1.5f);
        ObjectAnimator dotAlpha = ObjectAnimator.ofFloat(binding.viewVoicePulse, View.ALPHA, 1f, 0.45f);
        dotScaleX.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleY.setRepeatCount(ValueAnimator.INFINITE);
        dotAlpha.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleX.setRepeatMode(ValueAnimator.REVERSE);
        dotScaleY.setRepeatMode(ValueAnimator.REVERSE);
        dotAlpha.setRepeatMode(ValueAnimator.REVERSE);
        dotScaleX.setDuration(700);
        dotScaleY.setDuration(700);
        dotAlpha.setDuration(700);

        ObjectAnimator micScaleX = ObjectAnimator.ofFloat(binding.btnMic, View.SCALE_X, 1f, 1.08f);
        ObjectAnimator micScaleY = ObjectAnimator.ofFloat(binding.btnMic, View.SCALE_Y, 1f, 1.08f);
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
        if (!isUiActive()) return;
        binding.viewVoicePulse.setScaleX(1f);
        binding.viewVoicePulse.setScaleY(1f);
        binding.viewVoicePulse.setAlpha(1f);
        binding.btnMic.setScaleX(1f);
        binding.btnMic.setScaleY(1f);
    }

    private boolean isUiActive() {
        if (binding == null || isFinishing()) return false;
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed();
    }

    private void ensureAccessTokenThenStart() {
        String customerId = appPreferences.getCustomerId();
        if (TextUtils.isEmpty(customerId)) {
            Toast.makeText(this, "Vui lòng đăng nhập để chat hỗ trợ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String token = preferencesManager.getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            startChatSession();
            return;
        }

        binding.tvSubTitle.setText("Đang xác thực...");
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            firebaseUser.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String firebaseIdToken = task.getResult().getToken();
                    authApi.firebaseLogin(new FirebaseLoginRequest(firebaseIdToken)).enqueue(new Callback<UserDto>() {
                        @Override
                        public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && !TextUtils.isEmpty(response.body().getAccessToken())) {
                                preferencesManager.saveAccessToken(response.body().getAccessToken().trim());
                                startChatSession();
                            } else {
                                refreshAccessTokenByCustomer();
                            }
                        }

                        @Override
                        public void onFailure(Call<UserDto> call, Throwable t) {
                            refreshAccessTokenByCustomer();
                        }
                    });
                } else {
                    refreshAccessTokenByCustomer();
                }
            });
            return;
        }

        refreshAccessTokenByCustomer();
    }

    private void refreshAccessTokenByCustomer() {
        String customerId = appPreferences.getCustomerId();
        String phone = appPreferences.getCurrentPhone();
        authApi.refreshAccessToken(new RefreshAccessTokenRequest(customerId, phone))
                .enqueue(new Callback<AccessTokenResponseDto>() {
                    @Override
                    public void onResponse(Call<AccessTokenResponseDto> call, Response<AccessTokenResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !TextUtils.isEmpty(response.body().getAccessToken())) {
                            preferencesManager.saveAccessToken(response.body().getAccessToken().trim());
                            startChatSession();
                        } else {
                            runOnUiThread(() -> {
                                binding.tvSubTitle.setText("Vui lòng đăng nhập lại");
                                Toast.makeText(SupportChatActivity.this,
                                        "Không thể xác thực phiên chat. Hãy đăng xuất và đăng nhập lại.",
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<AccessTokenResponseDto> call, Throwable t) {
                        runOnUiThread(() -> {
                            binding.tvSubTitle.setText("Lỗi kết nối");
                            Toast.makeText(SupportChatActivity.this,
                                    "Không thể kết nối máy chủ. Vui lòng thử lại.",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void startChatSession() {
        if (sessionReady) return;
        sessionReady = true;
        loadConversationAndHistory();
        connectSocket();
    }

    private void loadConversationAndHistory() {
        String customerId = appPreferences.getCustomerId();
        if (TextUtils.isEmpty(customerId)) return;

        binding.tvSubTitle.setText("Đang tải cuộc trò chuyện...");
        supportApi.getConversations(customerId).enqueue(new Callback<SupportConversationsResponseDto>() {
            @Override
            public void onResponse(Call<SupportConversationsResponseDto> call, Response<SupportConversationsResponseDto> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    runOnUiThread(() -> binding.tvSubTitle.setText("Không tải được cuộc trò chuyện"));
                    return;
                }
                List<SupportConversationDto> convos = response.body().getData();
                if (convos == null || convos.isEmpty() || convos.get(0) == null) {
                    runOnUiThread(() -> binding.tvSubTitle.setText("Không tạo được cuộc trò chuyện"));
                    return;
                }
                conversationId = convos.get(0).getId();
                loadMessages();
                if (socketConnected && !TextUtils.isEmpty(conversationId)) {
                    socketManager.joinConversation(conversationId, args -> {});
                }
            }

            @Override
            public void onFailure(Call<SupportConversationsResponseDto> call, Throwable t) {
                runOnUiThread(() -> binding.tvSubTitle.setText("Lỗi tải cuộc trò chuyện"));
            }
        });
    }

    private void loadMessages() {
        if (TextUtils.isEmpty(conversationId)) return;

        supportApi.getMessages(conversationId, null, 50).enqueue(new Callback<SupportMessagesResponseDto>() {
            @Override
            public void onResponse(Call<SupportMessagesResponseDto> call, Response<SupportMessagesResponseDto> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }
                List<SupportMessageDto> data = response.body().getData();
                List<ChatMessage> mapped = new ArrayList<>();
                if (data != null) {
                    for (SupportMessageDto msg : data) {
                        if (msg == null) continue;
                        int type = "user".equalsIgnoreCase(msg.getSenderType())
                                ? ChatMessage.TYPE_USER
                                : ChatMessage.TYPE_BOT;
                        mapped.add(new ChatMessage(msg.getText(), type));
                    }
                }
                adapter.setMessages(mapped);
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<SupportMessagesResponseDto> call, Throwable t) {
                // ignore
            }
        });
    }

    private void connectSocket() {
        String token = preferencesManager.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            binding.tvSubTitle.setText("Chưa có token đăng nhập");
            return;
        }

        socketManager.connect(token.trim(), new SupportSocketManager.Listener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    socketConnected = true;
                    binding.tvSubTitle.setText("Đã kết nối");
                    if (!TextUtils.isEmpty(conversationId)) {
                        socketManager.joinConversation(conversationId, args -> {});
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    socketConnected = false;
                    binding.tvSubTitle.setText("Mất kết nối");
                });
            }

            @Override
            public void onNewMessage(JSONObject messageJson) {
                runOnUiThread(() -> {
                    String senderType = messageJson.optString("senderType", "");
                    String text = messageJson.optString("text", "");
                    int type = "user".equalsIgnoreCase(senderType) ? ChatMessage.TYPE_USER : ChatMessage.TYPE_BOT;
                    adapter.addMessage(new ChatMessage(text, type));
                    if ("admin".equalsIgnoreCase(senderType)) {
                        updatePeerTypingIndicator(false);
                    }
                    scrollToBottom();
                });
            }

            @Override
            public void onTypingUpdate(JSONObject typingJson) {
                runOnUiThread(() -> {
                    String senderType = typingJson.optString("senderType", "");
                    boolean isTyping = typingJson.optBoolean("isTyping", false);
                    if ("admin".equalsIgnoreCase(senderType)) {
                        updatePeerTypingIndicator(isTyping);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> binding.tvSubTitle.setText("Lỗi kết nối"));
            }
        });
    }

    private void sendMessage(String text) {
        if (TextUtils.isEmpty(text)) return;
        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Đang tạo cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            loadConversationAndHistory();
            return;
        }
        if (!socketConnected) {
            Toast.makeText(this, "Đang kết nối, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            return;
        }
        emitTypingState(false);
        mainHandler.removeCallbacks(typingStopRunnable);
        binding.edtMessage.setText("");

        String clientMessageId = UUID.randomUUID().toString();
        socketManager.sendMessage(conversationId, text, clientMessageId, args -> {});
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.rvChat.scrollToPosition(count - 1);
        }
        if (peerTypingVisible) {
            binding.rvChat.post(() -> binding.rvChat.scrollToPosition(Math.max(adapter.getItemCount() - 1, 0)));
        }
    }

    @Override
    protected void onDestroy() {
        emitTypingState(false);
        mainHandler.removeCallbacks(typingStopRunnable);
        stopTypingDotAnimation();
        mainHandler.removeCallbacksAndMessages(null);
        stopPulseAnimation();
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(false);
            speechManager.releaseSession();
        }
        if (socketManager != null) {
            socketManager.disconnect();
        }
        binding = null;
        super.onDestroy();
    }
}
