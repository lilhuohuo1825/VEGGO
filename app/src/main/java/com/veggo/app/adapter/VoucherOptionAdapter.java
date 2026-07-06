package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.data.remote.dto.PromotionDto;

import java.util.List;
import java.util.Locale;

public class VoucherOptionAdapter extends RecyclerView.Adapter<VoucherOptionAdapter.VoucherViewHolder> {
    private final List<VoucherItemUiModel> items;
    private final OnVoucherSelectedListener listener;
    @Nullable
    private String selectedProductPromotionId;
    @Nullable
    private String selectedShippingPromotionId;

    public VoucherOptionAdapter(
            List<VoucherItemUiModel> items,
            @Nullable String selectedProductPromotionId,
            @Nullable String selectedShippingPromotionId,
            OnVoucherSelectedListener listener
    ) {
        this.items = items;
        this.selectedProductPromotionId = selectedProductPromotionId;
        this.selectedShippingPromotionId = selectedShippingPromotionId;
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
        boolean isSelected = isSelected(item);
        holder.bind(item, isSelected);
        holder.itemView.setOnClickListener(v -> select(position));
        holder.radioButton.setOnClickListener(v -> select(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setSelectedPromotionIds(
            @Nullable String productPromotionId,
            @Nullable String shippingPromotionId
    ) {
        this.selectedProductPromotionId = productPromotionId;
        this.selectedShippingPromotionId = shippingPromotionId;
        notifyDataSetChanged();
    }

    @Nullable
    public String getSelectedProductPromotionId() {
        return selectedProductPromotionId;
    }

    @Nullable
    public String getSelectedShippingPromotionId() {
        return selectedShippingPromotionId;
    }

    private void select(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        VoucherItemUiModel item = items.get(position);
        if (!item.enabled) {
            return;
        }

        if (item.shipping) {
            if (item.promotionId != null && item.promotionId.equals(selectedShippingPromotionId)) {
                selectedShippingPromotionId = null;
            } else {
                selectedShippingPromotionId = item.promotionId;
            }
        } else {
            if (item.promotionId != null && item.promotionId.equals(selectedProductPromotionId)) {
                selectedProductPromotionId = null;
            } else {
                selectedProductPromotionId = item.promotionId;
            }
        }

        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(
                    findSelectedItem(selectedProductPromotionId),
                    findSelectedItem(selectedShippingPromotionId)
            );
        }
    }

    private boolean isSelected(@NonNull VoucherItemUiModel item) {
        if (item.promotionId == null) {
            return false;
        }
        if (item.shipping) {
            return item.promotionId.equals(selectedShippingPromotionId);
        }
        return item.promotionId.equals(selectedProductPromotionId);
    }

    @Nullable
    private VoucherItemUiModel findSelectedItem(@Nullable String promotionId) {
        if (promotionId == null) {
            return null;
        }
        for (VoucherItemUiModel item : items) {
            if (promotionId.equals(item.promotionId)) {
                return item;
            }
        }
        return null;
    }

    public static boolean isShippingPromotion(@Nullable PromotionDto promotion) {
        if (promotion == null) {
            return false;
        }
        String scope = promotion.getScope();
        if (scope != null && !scope.trim().isEmpty()) {
            return "Shipping".equalsIgnoreCase(scope.trim());
        }
        String haystack = ((promotion.getName() == null ? "" : promotion.getName()) + " "
                + (promotion.getDescription() == null ? "" : promotion.getDescription()) + " "
                + (promotion.getCode() == null ? "" : promotion.getCode())).toLowerCase(Locale.US);
        return haystack.contains("ship") || haystack.contains("vận chuyển") || haystack.contains("shipping");
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
        void onSelectionChanged(
                @Nullable VoucherItemUiModel productVoucher,
                @Nullable VoucherItemUiModel shippingVoucher
        );
    }

    public static final class VoucherItemUiModel {
        public final String title;
        public final String condition;
        public final String expiry;
        public final int iconResId;
        public final String promotionId;
        public final boolean enabled;
        public final boolean shipping;
        public final PromotionDto promotion;

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId) {
            this(title, condition, expiry, iconResId, null, true, false, null);
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId,
                                  String promotionId, boolean enabled) {
            this(title, condition, expiry, iconResId, promotionId, enabled, false, null);
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId,
                                  String promotionId, boolean enabled, PromotionDto promotion) {
            this(title, condition, expiry, iconResId, promotionId, enabled,
                    isShippingPromotion(promotion), promotion);
        }

        public VoucherItemUiModel(String title, String condition, String expiry, int iconResId,
                                  String promotionId, boolean enabled, boolean shipping,
                                  PromotionDto promotion) {
            this.title = title;
            this.condition = condition;
            this.expiry = expiry;
            this.iconResId = iconResId;
            this.promotionId = promotionId;
            this.enabled = enabled;
            this.shipping = shipping;
            this.promotion = promotion;
        }
    }
}
