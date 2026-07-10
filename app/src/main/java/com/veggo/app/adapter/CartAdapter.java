package com.veggo.app.adapter;

import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.data.remote.dto.ProductDto;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private final List<CartItemUiModel> items;
    private final CartItemActionListener listener;
    private boolean editMode;

    public CartAdapter(List<CartItemUiModel> items, CartItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setEditMode(boolean editMode) {
        if (this.editMode == editMode) {
            return;
        }
        this.editMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setItems(List<CartItemUiModel> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItemUiModel item = items.get(position);
        holder.bind(item, listener, position == items.size() - 1, editMode);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class CartViewHolder extends RecyclerView.ViewHolder {
        private final ImageView checkboxView;
        private final ImageView productView;
        private final TextView nameView;
        private final TextView unitView;
        private final TextView carbonPointView;
        private final TextView priceView;
        private final TextView oldPriceView;
        private final TextView decreaseView;
        private final TextView quantityView;
        private final TextView increaseView;
        private final TextView deleteView;
        private final View contentView;
        private final View dividerView;

        private CartViewHolder(@NonNull View itemView) {
            super(itemView);
            contentView = itemView.findViewById(R.id.layoutCartItemContent);
            checkboxView = itemView.findViewById(R.id.imgItemCheckbox);
            productView = itemView.findViewById(R.id.imgProduct);
            nameView = itemView.findViewById(R.id.tvItemName);
            unitView = itemView.findViewById(R.id.tvItemUnit);
            carbonPointView = itemView.findViewById(R.id.tvCarbonPoint);
            priceView = itemView.findViewById(R.id.tvItemPrice);
            oldPriceView = itemView.findViewById(R.id.tvItemOldPrice);
            decreaseView = itemView.findViewById(R.id.tvDecreaseQuantity);
            quantityView = itemView.findViewById(R.id.tvQuantity);
            increaseView = itemView.findViewById(R.id.tvIncreaseQuantity);
            deleteView = itemView.findViewById(R.id.tvDeleteCartItem);
            dividerView = itemView.findViewById(R.id.viewDivider);
        }

        private void bind(CartItemUiModel item, CartItemActionListener listener, boolean isLastItem, boolean editMode) {
            checkboxView.setImageResource(item.isChecked ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_uncheck);
            
            Glide.with(itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_vegetable)
                    .into(productView);

            nameView.setText(item.name);
            unitView.setText(item.selectedWeight);
            boolean showUnit = item.selectedWeight != null && !item.selectedWeight.isEmpty();
            unitView.setVisibility(showUnit ? View.VISIBLE : View.GONE);
            if (editMode && item.hasWeightOptions) {
                unitView.setClickable(true);
                unitView.setFocusable(true);
                unitView.setBackgroundResource(R.drawable.bg_tag_green);
                unitView.setOnClickListener(v -> dispatchVariantClicked(listener));
            } else {
                unitView.setClickable(false);
                unitView.setFocusable(false);
                unitView.setOnClickListener(null);
            }
            if (carbonPointView != null) {
                carbonPointView.setText(String.format(Locale.US, "Carbon: %.1f", item.carbonSavingPoint));
                carbonPointView.setVisibility(editMode ? View.GONE : View.VISIBLE);
            }
            priceView.setText(item.priceText);
            
            if (item.price >= item.oldPrice) {
                oldPriceView.setVisibility(View.GONE);
            } else {
                oldPriceView.setVisibility(View.VISIBLE);
                oldPriceView.setText(item.oldPriceText);
                oldPriceView.setPaintFlags(oldPriceView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            quantityView.setText(String.valueOf(item.quantity));
            dividerView.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
            contentView.setTranslationX(0f);

            decreaseView.setVisibility(View.VISIBLE);
            quantityView.setVisibility(View.VISIBLE);
            increaseView.setVisibility(View.VISIBLE);
            deleteView.setVisibility(View.VISIBLE);

            checkboxView.setOnClickListener(v -> dispatchCheckedChanged(listener));
            deleteView.setOnClickListener(v -> dispatchRemoved(listener));
            decreaseView.setOnClickListener(v -> dispatchQuantityChanged(listener, -1));
            increaseView.setOnClickListener(v -> dispatchQuantityChanged(listener, 1));
            contentView.setOnClickListener(v -> {
                if (editMode) {
                    if (item.hasWeightOptions) {
                        dispatchVariantClicked(listener);
                    }
                } else {
                    dispatchClicked(listener);
                }
            });
            contentView.setOnLongClickListener(v -> {
                if (!editMode) {
                    dispatchLongClicked(listener);
                }
                return true;
            });
            setupSwipeToRevealDelete();
        }

        private void setupSwipeToRevealDelete() {
            final float touchSlop = itemView.getResources().getDisplayMetrics().density * 8f;
            contentView.setOnTouchListener(new View.OnTouchListener() {
                private float downX;
                private float downY;
                private boolean swiping;
                private boolean longPressTriggered;
                private Runnable longPressRunnable;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float deleteWidth = deleteView.getWidth() > 0
                            ? deleteView.getWidth()
                            : 76f * itemView.getResources().getDisplayMetrics().density;
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = event.getX();
                            downY = event.getY();
                            swiping = false;
                            longPressTriggered = false;
                            longPressRunnable = () -> {
                                if (!swiping) {
                                    longPressTriggered = true;
                                    contentView.performLongClick();
                                }
                            };
                            contentView.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getX() - downX;
                            float deltaY = event.getY() - downY;
                            // Hủy runnable long click ngay khi ngón tay di chuyển nhẹ để tránh đụng độ với thao tác vuốt xóa
                            if (Math.abs(deltaX) > touchSlop / 2 || Math.abs(deltaY) > touchSlop / 2) {
                                if (longPressRunnable != null) {
                                    contentView.removeCallbacks(longPressRunnable);
                                    longPressRunnable = null;
                                }
                            }
                            if (Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                                swiping = true;
                                itemView.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            if (swiping) {
                                float currentTranslation = contentView.getTranslationX();
                                float targetTranslation = Math.max(-deleteWidth, Math.min(0f, currentTranslation + deltaX));
                                contentView.setTranslationX(targetTranslation);
                                downX = event.getX();
                                return true;
                            }
                            return false;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (longPressRunnable != null) {
                                contentView.removeCallbacks(longPressRunnable);
                                longPressRunnable = null;
                            }
                            itemView.getParent().requestDisallowInterceptTouchEvent(false);
                            if (swiping) {
                                float target = contentView.getTranslationX() < -deleteWidth / 2f ? -deleteWidth : 0f;
                                contentView.animate().translationX(target).setDuration(120).start();
                            } else if (!longPressTriggered && event.getActionMasked() == MotionEvent.ACTION_UP) {
                                contentView.performClick();
                            }
                            swiping = false;
                            return true;
                        default:
                            return true;
                    }
                }
            });
        }

        private void dispatchClicked(CartItemActionListener listener) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClicked(position);
            }
        }

        private void dispatchVariantClicked(CartItemActionListener listener) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemVariantClicked(position);
            }
        }

        private void dispatchLongClicked(CartItemActionListener listener) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClicked(position);
            }
        }

        private void dispatchCheckedChanged(CartItemActionListener listener) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemCheckedChanged(position);
            }
        }

        private void dispatchRemoved(CartItemActionListener listener) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemRemoved(position);
            }
        }

        private void dispatchQuantityChanged(CartItemActionListener listener, int delta) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemQuantityChanged(position, delta);
            }
        }
    }

    public interface CartItemActionListener {
        void onItemCheckedChanged(int position);

        void onItemRemoved(int position);

        void onItemQuantityChanged(int position, int delta);

        void onItemClicked(int position);

        void onItemLongClicked(int position);

        void onItemVariantClicked(int position);
    }

    public static final class CartItemUiModel {
        public final String sku;
        public final String name;
        public final String selectedWeight;
        public final double selectedWeightValue;
        public final boolean hasWeightOptions;
        public final double carbonSavingPoint;
        public final long price;
        public final long oldPrice;
        public final String priceText;
        public final String oldPriceText;
        public final String imageUrl;
        public final ProductDto product;
        public boolean isChecked;
        public int quantity;

        public CartItemUiModel(String sku, String name, String selectedWeight, double selectedWeightValue,
                               boolean hasWeightOptions, double carbonSavingPoint, long price, long oldPrice,
                               String imageUrl, int quantity) {
            this(sku, name, selectedWeight, selectedWeightValue, hasWeightOptions, carbonSavingPoint,
                    price, oldPrice, imageUrl, quantity, null);
        }

        public CartItemUiModel(String sku, String name, String selectedWeight, double selectedWeightValue,
                               boolean hasWeightOptions, double carbonSavingPoint, long price, long oldPrice,
                               String imageUrl, int quantity, ProductDto product) {
            this.sku = sku;
            this.name = name;
            this.selectedWeight = selectedWeight;
            this.selectedWeightValue = selectedWeightValue > 0 ? selectedWeightValue : 1.0;
            this.hasWeightOptions = hasWeightOptions;
            this.carbonSavingPoint = carbonSavingPoint;
            this.price = price;
            this.oldPrice = oldPrice;
            this.priceText = formatCurrency(getVariantUnitPrice(price));
            this.oldPriceText = formatCurrency(getVariantUnitPrice(oldPrice));
            this.imageUrl = imageUrl;
            this.product = product;
            this.quantity = quantity;
            this.isChecked = true;
        }

        public long getLineTotal() {
            return getVariantUnitPrice(price) * quantity;
        }

        public long getLineOriginalTotal() {
            long originalUnitPrice = oldPrice > price ? oldPrice : price;
            return getVariantUnitPrice(originalUnitPrice) * quantity;
        }

        public String cartLineKey() {
            return sku + "#" + trimTrailingZeros(selectedWeightValue);
        }

        private long getVariantUnitPrice(long unitPrice) {
            double multiplier = hasWeightOptions ? selectedWeightValue : 1.0;
            return Math.round(unitPrice * multiplier);
        }

        private static String formatCurrency(long amount) {
            return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
        }

        private static String trimTrailingZeros(double value) {
            String text = String.format(Locale.US, "%.3f", value);
            while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
                text = text.substring(0, text.length() - 1);
            }
            return text;
        }
    }
}
