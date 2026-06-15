package com.veggo.app.presentation.chatbot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.veggo.app.R;
import com.veggo.app.adapter.ChatAdapter;
import com.veggo.app.databinding.ActivityChatbotBinding;
import com.veggo.app.domain.model.ChatMessage;

public class ChatbotActivity extends AppCompatActivity {

    private ActivityChatbotBinding binding;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupChat();
        setupListeners();
        setupQuickReplies();

        // Thêm tin nhắn chào mừng ngay từ đầu
        chatAdapter.addMessage(new ChatMessage("Xin chào! 👋\nMình là Trợ lý AI của VEGGO.\nBạn cần mình giúp gì hôm nay?", ChatMessage.TYPE_WELCOME));
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // Bỏ setStackFromEnd(true) để tin nhắn bắt đầu từ trên xuống
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.edtMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });
    }

    private void setupQuickReplies() {
        String[] tags = {"Giá sản phẩm", "Tìm kiếm", "Bảo quản"};
        
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
        addUserMessage(text);
        binding.edtMessage.setText("");
        
        binding.rvChat.postDelayed(() -> {
            handleBotResponse(text);
        }, 800);
    }

    private void addUserMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_USER));
        binding.rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void addBotMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_BOT));
        binding.rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void handleBotResponse(String userMessage) {
        String msg = userMessage.toLowerCase();
        if (msg.contains("giá sản phẩm")) {
            chatAdapter.addMessage(new ChatMessage("", ChatMessage.TYPE_PRODUCT));
            binding.rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        } else if (msg.contains("tìm kiếm")) {
            addBotMessage("Bạn muốn tìm loại thực phẩm nào? Hãy nhập tên sản phẩm vào ô chat nhé.");
        } else if (msg.contains("bảo quản")) {
            addBotMessage("Để giữ thực phẩm tươi lâu, hãy để rau củ trong ngăn mát tủ lạnh từ 2-5°C và bọc kín bằng túi giấy nhé.");
        } else {
            addBotMessage("Cảm ơn bạn, Veggo AI đã ghi nhận yêu cầu và sẽ phản hồi sớm nhất có thể.");
        }
    }
}
