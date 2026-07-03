package com.veggo.app.presentation.chatbot;

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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veggo.app.R;
import com.veggo.app.adapter.ChatAdapter;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.data.remote.api.ChatApi;
import com.veggo.app.data.remote.dto.ChatMessageDataDto;
import com.veggo.app.data.remote.dto.ChatSuggestedProductDto;
import com.veggo.app.data.remote.dto.ChatSuggestedRecipeDto;
import com.veggo.app.data.repository.ChatRepository;
import com.veggo.app.databinding.ActivityChatbotBinding;
import com.veggo.app.domain.model.ChatMessage;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;
import com.veggo.app.presentation.community.InstructionRecipeDetailActivity;
import com.veggo.app.presentation.product.ProductDetailActivity;
import com.veggo.app.speech.SpeechCallback;
import com.veggo.app.speech.SpeechManager;
import com.veggo.app.speech.VoiceState;

import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity implements SpeechCallback {

    private static final long VOICE_SEND_DELAY_MS = 800L;

    private ActivityChatbotBinding binding;
    private ChatAdapter chatAdapter;
    private ChatRepository chatRepository;
    private SpeechManager speechManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String customerId;
    private String conversationId;
    private boolean isSending;
    private boolean pendingVoiceSend;
    private boolean voiceSessionActive;
    private AnimatorSet pulseAnimator;
    private long lastSentAtMs = 0;
    private static final long MIN_SEND_INTERVAL_MS = 4000;

    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<Intent> speechIntentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        recordAudioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isUiActive()) {
                        return;
                    }
                    if (Boolean.TRUE.equals(granted)) {
                        startVoiceInput();
                    } else {
                        Toast.makeText(
                                this,
                                R.string.chat_voice_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
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

        chatRepository = new ChatRepository(ApiClient.createService(ChatApi.class));
        speechManager = new SpeechManager(this, this);
        speechManager.setIntentLauncher(speechIntentLauncher);
        customerId = resolveCustomerId();

        setupChat();
        setupListeners();
        setupQuickReplies();

        chatAdapter.addMessage(new ChatMessage(
                "Xin chào! 👋\nMình là Trợ lý AI của VEGGO.\nBạn cần mình giúp gì hôm nay?",
                ChatMessage.TYPE_WELCOME
        ));
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnSuggestionClickListener(new ChatAdapter.OnSuggestionClickListener() {
            @Override
            public void onRecipeClick(String instructionId) {
                openRecipeDetail(instructionId);
            }

            @Override
            public void onProductClick(String productId, boolean openAddToCart) {
                openProductDetail(productId, openAddToCart);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(chatAdapter);
    }

    private void openRecipeDetail(String instructionId) {
        if (TextUtils.isEmpty(instructionId)) {
            return;
        }
        Intent intent = new Intent(this, InstructionRecipeDetailActivity.class);
        intent.putExtra(InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID, instructionId.trim());
        navigateTo(intent);
    }

    private void openProductDetail(String productId, boolean openAddToCart) {
        if (TextUtils.isEmpty(productId)) {
            return;
        }
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId.trim());
        if (openAddToCart) {
            intent.putExtra(ProductDetailActivity.EXTRA_OPEN_ADD_TO_CART, true);
        }
        navigateTo(intent);
    }

    private void navigateTo(Intent intent) {
        prepareForNavigation();
        if (!isUiActive()) {
            return;
        }
        startActivity(intent);
    }

    private void prepareForNavigation() {
        pendingVoiceSend = false;
        voiceSessionActive = false;
        mainHandler.removeCallbacksAndMessages(null);
        stopPulseAnimation();
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(false);
            speechManager.releaseSession();
        }
        hideVoiceUiQuietly();
    }

    private boolean isUiActive() {
        if (binding == null || isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed();
    }

    private void hideVoiceUiQuietly() {
        if (!isUiActive()) {
            return;
        }
        binding.layoutVoiceStatus.setVisibility(View.GONE);
        binding.progressVoice.setVisibility(View.GONE);
        binding.edtMessage.setHint(R.string.chat_input_hint);
        updateMicActiveUi(false);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.edtMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });

        binding.btnMic.setOnClickListener(v -> onMicClicked());
    }

    private void onMicClicked() {
        if (isSending) {
            return;
        }

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
        if (!isUiActive()) {
            return;
        }
        showVoiceStatus(
                R.string.chat_voice_status_listening,
                R.string.chat_voice_hint_listening,
                true
        );
        updateMicActiveUi(true);
    }

    @Override
    public void onListening() {
        if (!isUiActive()) {
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
        if (!isUiActive()) {
            return;
        }
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
        if (!isUiActive() || !voiceSessionActive || TextUtils.isEmpty(text)) {
            return;
        }
        binding.edtMessage.setText(text);
        binding.edtMessage.setSelection(text.length());
        binding.tvVoiceHint.setText(getString(R.string.chat_voice_preview_format, text));
    }

    @Override
    public void onResult(String text) {
        if (!isUiActive() || TextUtils.isEmpty(text)) {
            return;
        }

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
            if (!isUiActive()) {
                return;
            }
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
        if (!isUiActive()) {
            return;
        }
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        resetVoiceUi();
    }

    @Override
    public void onVoiceStop() {
        if (!isUiActive() || pendingVoiceSend) {
            return;
        }
        resetVoiceUi();
    }

    private void showVoiceStatus(int statusRes, int hintRes, boolean animatePulse) {
        showVoiceStatus(statusRes, hintRes, null, animatePulse);
    }

    private void showVoiceStatus(int statusRes, int hintRes, String hintArg, boolean animatePulse) {
        if (!isUiActive()) {
            return;
        }
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
        if (!isUiActive()) {
            return;
        }
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
        if (!isUiActive()) {
            return;
        }
        binding.btnMic.setBackgroundResource(active ? R.drawable.bg_voice_mic_active : R.drawable.bg_home_mic);
        binding.imgMic.setColorFilter(ContextCompat.getColor(
                this,
                active ? R.color.white : R.color.neutral_80
        ));
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        if (!isUiActive()) {
            return;
        }

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
        dotScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        dotScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        dotAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

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
        if (!isUiActive()) {
            return;
        }
        binding.viewVoicePulse.setScaleX(1f);
        binding.viewVoicePulse.setScaleY(1f);
        binding.viewVoicePulse.setAlpha(1f);
        binding.btnMic.setScaleX(1f);
        binding.btnMic.setScaleY(1f);
    }

    private void setupQuickReplies() {
        String[] tags = {
                "Giảm cân nên ăn gì",
                "Nấu gì với giỏ hàng",
                "Tính calories bữa ăn"
        };

        binding.layoutQuickReplies.removeAllViews();
        for (String tag : tags) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_quick_reply, binding.layoutQuickReplies, false);
            TextView tv = view.findViewById(R.id.tvReply);
            tv.setText(tag);
            view.setOnClickListener(v -> sendMessage(tag));
            binding.layoutQuickReplies.addView(view);
        }
    }

    private void sendMessage(String text) {
        if (isSending) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastSentAtMs > 0 && now - lastSentAtMs < MIN_SEND_INTERVAL_MS) {
            long waitSec = (MIN_SEND_INTERVAL_MS - (now - lastSentAtMs) + 999) / 1000;
            Toast.makeText(this, "Vui lòng đợi " + waitSec + " giây trước khi gửi tiếp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(customerId)) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng Trợ lý AI", Toast.LENGTH_SHORT).show();
            return;
        }

        addUserMessage(text);
        binding.edtMessage.setText("");
        lastSentAtMs = now;
        showLoadingState();

        chatRepository.sendMessage(customerId, text, conversationId, new ChatRepository.ResultCallback<ChatMessageDataDto>() {
            @Override
            public void onSuccess(ChatMessageDataDto result) {
                runOnUiThread(() -> handleChatResponse(result));
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> handleChatError(error));
            }
        });
    }

    private void showLoadingState() {
        isSending = true;
        setInputEnabled(false);
        chatAdapter.addMessage(ChatMessage.loading());
        scrollToBottom();
    }

    private void hideLoadingState() {
        chatAdapter.removeLastMessage();
        isSending = false;
        setInputEnabled(true);
    }

    private void handleChatResponse(ChatMessageDataDto result) {
        if (!isUiActive()) {
            return;
        }
        hideLoadingState();

        if (result == null) {
            addBotMessage("Xin lỗi, mình chưa nhận được phản hồi. Bạn thử hỏi lại nhé.");
            return;
        }

        if (!TextUtils.isEmpty(result.getConversationId())) {
            conversationId = result.getConversationId();
        }

        String reply = result.getReply();
        addBotMessage(TextUtils.isEmpty(reply)
                ? "Mình chưa có câu trả lời phù hợp. Bạn thử hỏi theo cách khác nhé."
                : reply);

        List<ChatSuggestedRecipeDto> recipes = result.getSuggestedRecipes();
        if (recipes != null && !recipes.isEmpty()) {
            chatAdapter.addMessage(ChatMessage.recipeList(new ArrayList<>(recipes)));
        }

        List<ChatSuggestedProductDto> products = result.getSuggestedProducts();
        if (products != null && !products.isEmpty()) {
            chatAdapter.addMessage(ChatMessage.productList(new ArrayList<>(products)));
        }

        if ((recipes != null && !recipes.isEmpty()) || (products != null && !products.isEmpty())) {
            scrollToBottom();
        }
    }

    private void handleChatError(Throwable error) {
        if (!isUiActive()) {
            return;
        }
        hideLoadingState();
        String message = ChatRepository.mapNetworkError(error);
        if (error instanceof com.veggo.app.core.network.ApiHttpException) {
            String apiMessage = error.getMessage();
            if (!TextUtils.isEmpty(apiMessage)) {
                message = apiMessage;
            }
        }
        addBotMessage(message);
        Toast.makeText(this, "Lỗi chatbot", Toast.LENGTH_SHORT).show();
    }

    private void addUserMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_USER));
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_BOT));
        scrollToBottom();
    }

    private void scrollToBottom() {
        binding.rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void setInputEnabled(boolean enabled) {
        binding.edtMessage.setEnabled(enabled);
        binding.btnSend.setEnabled(enabled);
        binding.btnSend.setAlpha(enabled ? 1f : 0.5f);
        binding.btnMic.setEnabled(enabled);
        binding.btnMic.setAlpha(enabled ? 1f : 0.5f);
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(this);
        String id = appPreferences.getCustomerId();
        if (!TextUtils.isEmpty(id)) {
            return id.trim();
        }

        id = new PreferencesManager(this).getUserId();
        if (!TextUtils.isEmpty(id)) {
            return id.trim();
        }

        return new PendingCheckoutStore(this).guestId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        if (speechManager != null) {
            speechManager.setCallbacksEnabled(false);
            speechManager.releaseSession();
        }
        pendingVoiceSend = false;
        voiceSessionActive = false;
        mainHandler.removeCallbacksAndMessages(null);
        stopPulseAnimation();
        hideVoiceUiQuietly();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPulseAnimation();
        mainHandler.removeCallbacksAndMessages(null);
        if (speechManager != null) {
            speechManager.destroy();
            speechManager = null;
        }
        binding = null;
        super.onDestroy();
    }
}
