package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentItemAdapter extends RecyclerView.Adapter<PaymentItemAdapter.PaymentItemViewHolder> {
    private final List<PaymentItemUiModel> items = new ArrayList<>();

    public PaymentItemAdapter(List<PaymentItemUiModel> items) {
        setItems(items);
    }

    public void setItems(List<PaymentItemUiModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        return new PaymentItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentItemViewHolder holder, int position) {
        holder.bind(items.get(position), position == items.size() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class PaymentItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productView;
        private final TextView nameView;
        private final TextView weightView;
        private final TextView priceView;
        private final TextView quantityView;
        private final View dividerView;

        private PaymentItemViewHolder(@NonNull View itemView) {
            super(itemView);
            productView = itemView.findViewById(R.id.imgProduct);
            nameView = itemView.findViewById(R.id.txtName);
            weightView = itemView.findViewById(R.id.txtWeight);
            priceView = itemView.findViewById(R.id.txtPrice);
            quantityView = itemView.findViewById(R.id.txtQuantity);
            dividerView = itemView.findViewById(R.id.viewDivider);
        }

        private void bind(PaymentItemUiModel item, boolean isLastItem) {
            productView.setImageResource(item.imageResId);
            nameView.setText(item.name);
            weightView.setText(item.weight);
            priceView.setText(formatCurrency(item.price));
            quantityView.setText("x" + item.quantity);
            dividerView.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
        }
    }

    public static final class PaymentItemUiModel {
        public final String name;
        public final String weight;
        public final int price;
        public final int quantity;
        public final int imageResId;

        public PaymentItemUiModel(String name, String weight, int price, int quantity, int imageResId) {
            this.name = name;
            this.weight = weight;
            this.price = price;
            this.quantity = quantity;
            this.imageResId = imageResId;
        }
    }

    private static String formatCurrency(int amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "\u0111";
    }
}
