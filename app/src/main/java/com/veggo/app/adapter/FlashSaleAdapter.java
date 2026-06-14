package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.domain.model.FlashSale;

public class FlashSaleAdapter extends ListAdapter<FlashSale, FlashSaleAdapter.FlashSaleViewHolder> {

    private OnFlashSaleClickListener listener;

    public interface OnFlashSaleClickListener {
        void onFlashSaleClick(FlashSale flashSale);
    }

    public void setOnFlashSaleClickListener(OnFlashSaleClickListener listener) {
        this.listener = listener;
    }

    public FlashSaleAdapter() {
        super(new DiffUtil.ItemCallback<FlashSale>() {
            @Override
            public boolean areItemsTheSame(@NonNull FlashSale oldItem, @NonNull FlashSale newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull FlashSale oldItem, @NonNull FlashSale newItem) {
                return oldItem.getName().equals(newItem.getName()) && oldItem.getPrice() == newItem.getPrice();
            }
        });
    }

    @NonNull
    @Override
    public FlashSaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_flash_sale, parent, false);
        return new FlashSaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashSaleViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class FlashSaleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgProduct;
        private final TextView tvDiscount;
        private final TextView tvProductName;
        private final TextView tvRating;
        private final TextView tvProductPrice;

        FlashSaleViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }

        void bind(FlashSale flashSale, OnFlashSaleClickListener listener) {
            tvProductName.setText(flashSale.getName());
            tvProductPrice.setText(CurrencyFormatter.formatVnd(flashSale.getPrice()));
            tvRating.setText(String.valueOf(flashSale.getRating()));
            tvDiscount.setText(flashSale.getDiscount());
            
            if (flashSale.getImageUrl() != null && !flashSale.getImageUrl().isEmpty()) {
                Glide.with(imgProduct.getContext())
                        .load(flashSale.getImageUrl())
                        .placeholder(R.drawable.ic_vegetable)
                        .error(R.drawable.ic_vegetable)
                        .into(imgProduct);
            } else if (flashSale.getImageRes() != 0) {
                imgProduct.setImageResource(flashSale.getImageRes());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFlashSaleClick(flashSale);
                }
            });
        }
    }
}
