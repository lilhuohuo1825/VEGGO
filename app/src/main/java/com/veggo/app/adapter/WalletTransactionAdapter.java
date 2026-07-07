package com.veggo.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    public WalletTransactionAdapter(List<WalletTransactionDto> transactions) {
        this.transactions = transactions;
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

        holder.tvTitle.setText(tx.getDescription());
        holder.tvDate.setText(formatDate(tx.getCreatedAt()));

        double amount = tx.getAmount();
        String formatted = CurrencyFormatter.formatVnd((long) Math.abs(amount));

        // Choose icon semantically
        String desc = tx.getDescription().toLowerCase();
        int iconRes;
        if (desc.contains("nạp tiền")) {
            iconRes = R.drawable.ic_wallet;
        } else if (desc.contains("thanh toán") || desc.contains("order") || desc.contains("đơn hàng")) {
            iconRes = R.drawable.ic_shopping_cart;
        } else if (desc.contains("quyên góp") || desc.contains("donate") || desc.contains("trồng cây")) {
            iconRes = R.drawable.ic_tree_stage_3;
        } else if (amount >= 0) {
            iconRes = R.drawable.ic_tx_receive;
        } else {
            iconRes = R.drawable.ic_tx_send;
        }

        holder.imgIcon.setImageResource(iconRes);

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
        public final TextView tvTitle;
        public final TextView tvDate;
        public final TextView tvAmount;
        public final ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTxTitle);
            tvDate = itemView.findViewById(R.id.tvTxDate);
            tvAmount = itemView.findViewById(R.id.tvTxAmount);
            imgIcon = itemView.findViewById(R.id.imgTxIcon);
        }
    }
}
