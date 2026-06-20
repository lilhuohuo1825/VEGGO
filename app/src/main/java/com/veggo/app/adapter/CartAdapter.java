package com.veggo.app.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private final List<CartItemUiModel> items;
    private final CartItemActionListener listener;

    public CartAdapter(List<CartItemUiModel> items, CartItemActionListener listener) {
        this.items = items;
        this.listener = listener;
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
        holder.bind(item, listener, position == items.size() - 1);
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
        private final ImageView deleteView;
        private final View dividerView;

        private CartViewHolder(@NonNull View itemView) {
            super(itemView);
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
            deleteView = itemView.findViewById(R.id.imgDeleteCartItem);
            dividerView = itemView.findViewById(R.id.viewDivider);
        }

        private void bind(CartItemUiModel item, CartItemActionListener listener, boolean isLastItem) {
            checkboxView.setImageResource(item.isChecked ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_uncheck);
            
            Glide.with(itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_vegetable)
                    .into(productView);

            nameView.setText(item.name);
            unitView.setText(item.selectedWeight);
            if (carbonPointView != null) {
                carbonPointView.setText(String.format(Locale.US, "Carbon: %.1f", item.carbonSavingPoint));
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

            checkboxView.setOnClickListener(v -> dispatchCheckedChanged(listener));
            deleteView.setOnClickListener(v -> dispatchRemoved(listener));
            decreaseView.setOnClickListener(v -> dispatchQuantityChanged(listener, -1));
            increaseView.setOnClickListener(v -> dispatchQuantityChanged(listener, 1));
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
    }

    public static final class CartItemUiModel {
        public final String sku;
        public final String name;
        public final String selectedWeight;
        public final double carbonSavingPoint;
        public final long price;
        public final long oldPrice;
        public final String priceText;
        public final String oldPriceText;
        public final String imageUrl;
        public boolean isChecked;
        public int quantity;

        public CartItemUiModel(String sku, String name, String selectedWeight, double carbonSavingPoint, long price, long oldPrice, String imageUrl, int quantity) {
            this.sku = sku;
            this.name = name;
            this.selectedWeight = selectedWeight;
            this.carbonSavingPoint = carbonSavingPoint;
            this.price = price;
            this.oldPrice = oldPrice;
            this.priceText = formatCurrency(price);
            this.oldPriceText = formatCurrency(oldPrice);
            this.imageUrl = imageUrl;
            this.quantity = quantity;
            this.isChecked = true;
        }

        public long getLineTotal() {
            return price * quantity;
        }

        private static String formatCurrency(long amount) {
            return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
        }
    }
}
