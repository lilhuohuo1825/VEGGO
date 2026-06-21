package com.veggo.app.presentation.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.data.remote.dto.FridgeItemDto;
import com.veggo.app.presentation.common.AssetScreenData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FridgeInventoryAdapter extends RecyclerView.Adapter<FridgeInventoryAdapter.ViewHolder> {

    private final List<FridgeItemDto> items;
    private final OnItemClickListener listener;
    private int swipedPosition = -1;

    public int getSwipedPosition() {
        return swipedPosition;
    }

    public void setSwipedPosition(int position) {
        if (swipedPosition == position) return;
        int previous = swipedPosition;
        swipedPosition = position;
        if (previous != -1 && previous < items.size()) {
            notifyItemChanged(previous);
        }
        if (swipedPosition != -1 && swipedPosition < items.size()) {
            notifyItemChanged(swipedPosition);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(FridgeItemDto item);
        void onDeleteClick(FridgeItemDto item, int position);
    }

    public FridgeInventoryAdapter(List<FridgeItemDto> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fridge_ingredient_urgent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FridgeItemDto item = items.get(position);
        
        long daysLeft = -1;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        long now = System.currentTimeMillis();

        try {
            if (item.getExpiryDate() != null) {
                Date d = format.parse(item.getExpiryDate());
                if (d != null) {
                    long diff = d.getTime() - now;
                    daysLeft = diff / (1000L * 60 * 60 * 24);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String name = item.getName() != null && !item.getName().isEmpty() ? item.getName() : "Nguyên liệu";
        String imageUrl = item.getImage();

        AssetScreenData.setText(holder.itemView, R.id.fridgeIngredientName, name);
        
        String qtyStr = String.valueOf(item.getQuantity());
        if (qtyStr.endsWith(".0")) qtyStr = qtyStr.substring(0, qtyStr.length() - 2);
        AssetScreenData.setText(holder.itemView, R.id.fridgeIngredientMeta, "Số lượng: " + qtyStr + " " + (item.getUnit() != null ? item.getUnit() : ""));
        
        String expiryStr = daysLeft < 0 ? "Hết hạn" : ("Còn " + daysLeft + " ngày");
        AssetScreenData.setText(holder.itemView, R.id.fridgeIngredientExpiry, expiryStr);

        ImageView image = holder.itemView.findViewById(R.id.fridgeIngredientImage);
        if (image != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                image.setPadding(0, 0, 0, 0);
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_vegetable)
                        .circleCrop()
                        .into(image);
            } else {
                int padding = (int) (10 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                image.setPadding(padding, padding, padding, padding);
                image.setImageResource(R.drawable.ic_vegetable);
            }
        }

        View foreground = holder.itemView.findViewById(R.id.fridgeItemForeground);
        View deleteBg = holder.itemView.findViewById(R.id.fridgeItemDeleteBg);
        
        // Maintain translation state for swiped item
        if (foreground != null) {
            if (position == swipedPosition) {
                float maxSwipe = 80 * holder.itemView.getContext().getResources().getDisplayMetrics().density;
                foreground.setTranslationX(-maxSwipe);
            } else {
                foreground.setTranslationX(0f);
            }
            
            foreground.setOnClickListener(v -> {
                if (swipedPosition != -1) {
                    setSwipedPosition(-1); // Close swipe on tap
                    return;
                }
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
        
        if (deleteBg != null) {
            deleteBg.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item, holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public FridgeItemDto getItem(int position) {
        return items.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View foreground;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            foreground = itemView.findViewById(R.id.fridgeItemForeground);
        }
    }
}
