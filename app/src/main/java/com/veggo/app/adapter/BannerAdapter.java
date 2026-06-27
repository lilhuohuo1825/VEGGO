package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.domain.model.Banner;

public class BannerAdapter extends ListAdapter<Banner, BannerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
    }

    private OnBannerClickListener onBannerClickListener;
    private ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_CROP;

    public void setOnBannerClickListener(OnBannerClickListener listener) {
        this.onBannerClickListener = listener;
    }

    public void setScaleType(ImageView.ScaleType scaleType) {
        this.scaleType = scaleType == null ? ImageView.ScaleType.CENTER_CROP : scaleType;
    }

    public BannerAdapter() {
        super(new DiffUtil.ItemCallback<Banner>() {
            @Override
            public boolean areItemsTheSame(@NonNull Banner oldItem, @NonNull Banner newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Banner oldItem, @NonNull Banner newItem) {
                return oldItem.getImageRes() == newItem.getImageRes() &&
                       stringEquals(oldItem.getImageUrl(), newItem.getImageUrl());
            }
        });
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (getItemCount() > 0) {
            holder.bind(getItem(position % getCurrentList().size()));
        }
    }

    @Override
    public int getItemCount() {
        if (getCurrentList().isEmpty()) return 0;
        return Integer.MAX_VALUE;
    }

    public int getRealCount() {
        return getCurrentList().size();
    }

    class BannerViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgBanner;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onBannerClickListener != null) {
                    onBannerClickListener.onBannerClick(getItem(position % getCurrentList().size()));
                }
            });
        }

        void bind(Banner banner) {
            imgBanner.setScaleType(scaleType);
            if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
                if (scaleType == ImageView.ScaleType.FIT_CENTER || scaleType == ImageView.ScaleType.CENTER_INSIDE) {
                    Glide.with(imgBanner.getContext())
                            .load(banner.getImageUrl())
                            .fitCenter()
                            .placeholder(banner.getImageRes() != 0 ? banner.getImageRes() : R.drawable.banner_freeship)
                            .error(banner.getImageRes() != 0 ? banner.getImageRes() : R.drawable.banner_freeship)
                            .into(imgBanner);
                } else {
                    Glide.with(imgBanner.getContext())
                            .load(banner.getImageUrl())
                            .centerCrop()
                            .placeholder(banner.getImageRes() != 0 ? banner.getImageRes() : R.drawable.banner_freeship)
                            .error(banner.getImageRes() != 0 ? banner.getImageRes() : R.drawable.banner_freeship)
                            .into(imgBanner);
                }
            } else if (banner.getImageRes() != 0) {
                imgBanner.setImageResource(banner.getImageRes());
            }
        }
    }

    private static boolean stringEquals(String first, String second) {
        if (first == null) return second == null;
        return first.equals(second);
    }
}
