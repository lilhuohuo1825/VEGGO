package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class SupportChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_DATE_DIVIDER = 99;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            for (int i = 0; i < newMessages.size(); i++) {
                ChatMessage current = newMessages.get(i);
                if (i == 0 || shouldShowDateDivider(newMessages.get(i - 1), current)) {
                    messages.add(new ChatMessage(formatDateDivider(current.getTimestamp()), TYPE_DATE_DIVIDER, current.getTimestamp()));
                }
                messages.add(current);
            }
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        if (messages.isEmpty() || shouldShowDateDivider(messages.get(messages.size() - 1), message)) {
            messages.add(new ChatMessage(formatDateDivider(message.getTimestamp()), TYPE_DATE_DIVIDER, message.getTimestamp()));
            notifyItemInserted(messages.size() - 1);
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private boolean shouldShowDateDivider(ChatMessage prev, ChatMessage current) {
        if (prev.getType() == TYPE_DATE_DIVIDER) return false;
        
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(prev.getTimestamp());
        
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(current.getTimestamp());
        
        return cal1.get(java.util.Calendar.YEAR) != cal2.get(java.util.Calendar.YEAR)
                || cal1.get(java.util.Calendar.DAY_OF_YEAR) != cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private static String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    private static String formatDateDivider(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_USER) {
            return new SimpleTextHolder(inflater.inflate(R.layout.item_support_chat_user, parent, false));
        } else if (viewType == TYPE_DATE_DIVIDER) {
            return new DateDividerHolder(inflater.inflate(R.layout.item_support_chat_date_divider, parent, false));
        }
        return new SimpleTextHolder(inflater.inflate(R.layout.item_support_chat_admin, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SimpleTextHolder) {
            String timeStr = formatTime(message.getTimestamp());
            ((SimpleTextHolder) holder).bind(message.getMessage(), timeStr);
        } else if (holder instanceof DateDividerHolder) {
            ((DateDividerHolder) holder).bind(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SimpleTextHolder extends RecyclerView.ViewHolder {
        private final android.widget.TextView tvMessage;
        private final android.widget.TextView tvTime;

        SimpleTextHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(String text, String time) {
            tvMessage.setText(text);
            if (tvTime != null) {
                tvTime.setText(time);
            }
        }
    }

    static class DateDividerHolder extends RecyclerView.ViewHolder {
        private final android.widget.TextView tvDate;

        DateDividerHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
        }

        void bind(String dateText) {
            tvDate.setText(dateText);
        }
    }
}
