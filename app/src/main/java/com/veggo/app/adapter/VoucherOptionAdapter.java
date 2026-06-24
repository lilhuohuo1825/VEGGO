package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;

import java.util.List;

public class VoucherOptionAdapter extends RecyclerView.Adapter<VoucherOptionAdapter.VoucherViewHolder> {
    private final List<VoucherItemUiModel> items;
    private final OnVoucherSelectedListener listener;
    private int selectedPosition;

    public VoucherOptionAdapter(List<VoucherItemUiModel> items, int selectedPosition, OnVoucherSelectedListener listener) {
        this.items = items;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        holder.bind(items.get(position), position == selectedPosition);
        holder.itemView.setOnClickListener(v -> select(position));
        holder.radioButton.setOnClickListener(v -> select(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void select(int position) {
        if (position == RecyclerView.NO_POSITION || position == selectedPosition) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        notifyItemChanged(position);
        if (listener != null) {
            listener.onVoucherSelected(items.get(position));
        }
    }

    static final class VoucherViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView conditionView;
        private final TextView expiryView;
        private final AppCompatRadioButton radioButton;

        private VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imgVoucherIcon);
            titleView = itemView.findViewById(R.id.tvVoucherTitle);
            conditionView = itemView.findViewById(R.id.tvVoucherCondition);
            expiryView = itemView.findViewById(R.id.tvVoucherExpiry);
            radioButton = itemView.findViewById(R.id.radioVoucher);
        }

        private void bind(VoucherItemUiModel item, boolean isSelected) {
            iconView.setImageResource(item.iconResId);
            titleView.setText(item.title);
            conditionView.setText(item.condition);
            expiryView.setText(item.expiry);
            radioButton.setChecked(isSelected);
            
            if (item.isEligible) {
                itemView.setAlpha(1.0f);
                radioButton.setEnabled(true);
                itemView.setEnabled(true);
                conditionView.setText(item.condition);
                conditionView.setTextColor(itemView.getContext().getColor(R.color.veggo_text_secondary));
            } else {
                itemView.setAlpha(0.5f);
                radioButton.setEnabled(false);
                itemView.setEnabled(false);
                if (item.reasonLabel != null && !item.reasonLabel.isEmpty()) {
                    conditionView.setText(item.reasonLabel);
                    conditionView.setTextColor(itemView.getContext().getColor(R.color.danger_main));
                }
            }
            
            itemView.setBackgroundResource(isSelected ? R.drawable.bg_selected : R.drawable.bg_normal);
        }
    }

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(VoucherItemUiModel item);
    }

    public static final class VoucherItemUiModel {
        public final String id;
        public final String title;
        public final String condition;
        public final String expiry;
        public final int iconResId;
        public boolean isEligible = true;
        public String reasonLabel = "";

        public VoucherItemUiModel(String id, String title, String condition, String expiry, int iconResId) {
            this.id = id;
            this.title = title;
            this.condition = condition;
            this.expiry = expiry;
            this.iconResId = iconResId;
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId) {
            this(title, title, condition, expiry, iconResId);
        }
    }
}
