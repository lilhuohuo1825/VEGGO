package com.veggo.app.adapter;

import android.graphics.Color;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.data.remote.dto.WalletTransactionDto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {
    private final List<WalletTransactionDto> transactions;
    private final ReadState readState;

    public interface ReadState {
        boolean isUnread(WalletTransactionDto tx);
        void markRead(WalletTransactionDto tx);
    }

    public WalletTransactionAdapter(List<WalletTransactionDto> transactions, ReadState readState) {
        this.transactions = transactions;
        this.readState = readState;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransactionDto tx = transactions.get(position);

        String title = tx.getDescription();
        holder.tvTitle.setText(title == null || title.trim().isEmpty() ? "Biến động số dư" : title);
        holder.tvDate.setText(formatDate(tx.getCreatedAt()));

        double amount = tx.getAmount();
        String formatted = CurrencyFormatter.formatVnd((long) Math.abs(amount));

        String type = tx.getType() == null ? "" : tx.getType().toLowerCase(Locale.getDefault());
        String desc = title == null ? "" : title.toLowerCase(Locale.getDefault());
        int iconRes;
        if ("donation".equals(type) || desc.contains("quyên góp") || desc.contains("donate")
                || desc.contains("trồng cây") || desc.contains("hạt giống")) {
            iconRes = R.drawable.ic_tree_outline;
        } else if ("deposit".equals(type) || desc.contains("nạp tiền")) {
            iconRes = R.drawable.ic_wallet;
        } else if ("payment".equals(type) || desc.contains("thanh toán") || desc.contains("order") || desc.contains("đơn hàng")) {
            iconRes = R.drawable.ic_shopping_cart;
        } else if (amount >= 0) {
            iconRes = R.drawable.ic_tx_receive;
        } else {
            iconRes = R.drawable.ic_tx_send;
        }

        holder.imgIcon.setImageResource(iconRes);

        boolean unread = readState != null && readState.isUnread(tx);
        if (holder.card != null) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(
                    holder.itemView.getContext(),
                    unread ? R.color.primary_bg : R.color.background_main
            ));
        }

        if (amount >= 0) {
            holder.tvAmount.setText("+" + formatted);
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32")); // Green
            holder.imgIcon.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.primary_bg));
            holder.imgIcon.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.primary_main));
        } else {
            holder.tvAmount.setText("-" + formatted);
            holder.tvAmount.setTextColor(Color.parseColor("#D32F2F")); // Red
            holder.imgIcon.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.danger_bg));
            holder.imgIcon.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.danger_main));
        }

        holder.itemView.setOnClickListener(v -> {
            if (readState != null) {
                readState.markRead(tx);
                notifyItemChanged(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "";
        }
        try {
            // ISO date parse fallback
            String cleanDate = isoDate.replace("Z", "+0000");
            java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
            Date date = df.parse(cleanDate);
            if (date != null) {
                return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            try {
                // Secondary fallback
                String cleanDate = isoDate.replace("Z", "+00:00");
                java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                Date date = df.parse(cleanDate);
                if (date != null) {
                    return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
                }
            } catch (Exception ignored) {}
        }
        return isoDate;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final CardView card;
        public final TextView tvTitle;
        public final TextView tvDate;
        public final TextView tvAmount;
        public final ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardTxItem);
            tvTitle = itemView.findViewById(R.id.tvTxTitle);
            tvDate = itemView.findViewById(R.id.tvTxDate);
            tvAmount = itemView.findViewById(R.id.tvTxAmount);
            imgIcon = itemView.findViewById(R.id.imgTxIcon);
        }
    }
}
