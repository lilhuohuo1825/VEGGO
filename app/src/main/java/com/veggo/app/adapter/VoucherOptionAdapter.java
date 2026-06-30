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
import com.veggo.app.data.remote.dto.PromotionDto;

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
        VoucherItemUiModel item = items.get(position);
        holder.bind(item, position == selectedPosition);
        holder.itemView.setOnClickListener(v -> select(position));
        holder.radioButton.setOnClickListener(v -> select(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setSelectedPosition(int selectedPosition) {
        int previous = this.selectedPosition;
        this.selectedPosition = selectedPosition;
        if (previous != RecyclerView.NO_POSITION && previous < items.size()) {
            notifyItemChanged(previous);
        }
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < items.size()) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private void select(int position) {
        if (position == RecyclerView.NO_POSITION || !items.get(position).enabled) {
            return;
        }
        int previous = selectedPosition;
        if (selectedPosition == position) {
            selectedPosition = RecyclerView.NO_POSITION;
            if (listener != null) {
                listener.onVoucherSelected(null);
            }
        } else {
            selectedPosition = position;
            if (listener != null) {
                listener.onVoucherSelected(items.get(position));
            }
        }
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
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
            radioButton.setEnabled(item.enabled);
            itemView.setBackgroundResource(isSelected ? R.drawable.bg_selected : R.drawable.bg_normal);
            itemView.setEnabled(item.enabled);
            itemView.setAlpha(item.enabled ? 1f : 0.45f);
        }
    }

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(VoucherItemUiModel item);
    }

    public static final class VoucherItemUiModel {
        public final String title;
        public final String condition;
        public final String expiry;
        public final int iconResId;
        public final String promotionId;
        public final boolean enabled;
        public final PromotionDto promotion;

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId) {
            this(title, condition, expiry, iconResId, null, true, null);
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId,
                                  String promotionId, boolean enabled) {
            this(title, condition, expiry, iconResId, promotionId, enabled, null);
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId,
                                  String promotionId, boolean enabled, PromotionDto promotion) {
            this.title = title;
            this.condition = condition;
            this.expiry = expiry;
            this.iconResId = iconResId;
            this.promotionId = promotionId;
            this.enabled = enabled;
            this.promotion = promotion;
        }
    }
}
