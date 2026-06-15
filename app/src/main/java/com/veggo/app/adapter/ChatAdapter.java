package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_WELCOME) {
            View view = inflater.inflate(R.layout.item_chat_welcome, parent, false);
            return new WelcomeViewHolder(view);
        } else if (viewType == ChatMessage.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == ChatMessage.TYPE_PRODUCT) {
            View view = inflater.inflate(R.layout.item_chat_product, parent, false);
            return new ProductViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof WelcomeViewHolder) {
            ((WelcomeViewHolder) holder).tvMessage.setText(message.getMessage());
        } else if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(message.getMessage());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvMessage.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class WelcomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        WelcomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
