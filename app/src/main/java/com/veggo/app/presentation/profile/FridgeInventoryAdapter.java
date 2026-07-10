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

    public List<FridgeItemDto> getItems() {
        return items;
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

        String expiryStr = "Sắp hết hạn";
        try {
            if (item.getExpiryDate() != null) {
                Date d = format.parse(item.getExpiryDate());
                if (d != null) {
                    java.util.Calendar today = java.util.Calendar.getInstance();
                    today.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    today.set(java.util.Calendar.MINUTE, 0);
                    today.set(java.util.Calendar.SECOND, 0);
                    today.set(java.util.Calendar.MILLISECOND, 0);
                    
                    java.util.Calendar expiry = java.util.Calendar.getInstance();
                    expiry.setTime(d);
                    expiry.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    expiry.set(java.util.Calendar.MINUTE, 0);
                    expiry.set(java.util.Calendar.SECOND, 0);
                    expiry.set(java.util.Calendar.MILLISECOND, 0);
                    
                    long diffDays = (expiry.getTimeInMillis() - today.getTimeInMillis()) / (1000L * 60 * 60 * 24);
                    
                    if (diffDays < 0) {
                        expiryStr = "Đã hết hạn";
                    } else if (diffDays == 0) {
                        expiryStr = "Hết hạn";
                    } else {
                        expiryStr = "Còn " + diffDays + " ngày";
                    }
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
        View deleteView = holder.itemView.findViewById(R.id.tvDeleteFridgeItem);
        
        if (foreground != null) {
            foreground.setTranslationX(0f);
            foreground.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
            
            final float touchSlop = holder.itemView.getResources().getDisplayMetrics().density * 8f;
            foreground.setOnTouchListener(new View.OnTouchListener() {
                private float downX, downY;
                private boolean swiping, longPressTriggered;
                private Runnable longPressRunnable;

                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    float deleteWidth = deleteView != null && deleteView.getWidth() > 0
                            ? deleteView.getWidth()
                            : 76f * holder.itemView.getResources().getDisplayMetrics().density;
                    switch (event.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            downX = event.getX();
                            downY = event.getY();
                            swiping = false;
                            longPressTriggered = false;
                            longPressRunnable = () -> {
                                if (!swiping) {
                                    longPressTriggered = true;
                                    foreground.performLongClick();
                                }
                            };
                            foreground.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE:
                            float deltaX = event.getX() - downX;
                            float deltaY = event.getY() - downY;
                            if (Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                                swiping = true;
                                if (longPressRunnable != null) {
                                    foreground.removeCallbacks(longPressRunnable);
                                    longPressRunnable = null;
                                }
                                holder.itemView.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            if (swiping) {
                                float currentTranslation = foreground.getTranslationX();
                                float targetTranslation = Math.max(-deleteWidth, Math.min(0f, currentTranslation + deltaX));
                                foreground.setTranslationX(targetTranslation);
                                downX = event.getX();
                                return true;
                            }
                            return false;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            if (longPressRunnable != null) {
                                foreground.removeCallbacks(longPressRunnable);
                                longPressRunnable = null;
                            }
                            holder.itemView.getParent().requestDisallowInterceptTouchEvent(false);
                            if (swiping) {
                                float target = foreground.getTranslationX() < -deleteWidth / 2f ? -deleteWidth : 0f;
                                foreground.animate().translationX(target).setDuration(120).start();
                            } else if (!longPressTriggered && event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                                foreground.performClick();
                            }
                            swiping = false;
                            return true;
                        default:
                            return false;
                    }
                }
            });
        }
        
        if (deleteView != null) {
            deleteView.setOnClickListener(v -> {
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
