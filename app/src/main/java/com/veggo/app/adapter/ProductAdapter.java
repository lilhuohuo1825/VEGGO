package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.databinding.ItemProductGridBinding;
import com.veggo.app.domain.model.Product;
import com.veggo.app.presentation.profile.TastePreferenceStore;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private static final float HORIZONTAL_CARD_WIDTH_DP = 168f;
    private static final float HORIZONTAL_CARD_MARGIN_END_DP = 12f;

    private List<Product> products = new ArrayList<>();
    private OnProductClickListener listener;
    private OnAddProductClickListener addProductClickListener;
    private boolean horizontalScrollMode;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnAddProductClickListener {
        void onAddProductClick(Product product);
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setOnAddProductClickListener(OnAddProductClickListener listener) {
        this.addProductClickListener = listener;
    }

    public void setHorizontalScrollMode(boolean horizontalScrollMode) {
        this.horizontalScrollMode = horizontalScrollMode;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    public void submitList(List<Product> products) {
        setProducts(products);
    }

    public List<Product> getProducts() {
        return products;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductGridBinding binding = ItemProductGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        if (horizontalScrollMode) {
            float density = parent.getResources().getDisplayMetrics().density;
            int width = Math.round(HORIZONTAL_CARD_WIDTH_DP * density);
            int marginEnd = Math.round(HORIZONTAL_CARD_MARGIN_END_DP * density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(marginEnd);
            binding.getRoot().setLayoutParams(params);
        }
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener, addProductClickListener);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductGridBinding binding;

        public ProductViewHolder(@NonNull ItemProductGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                final Product product,
                final OnProductClickListener listener,
                final OnAddProductClickListener addProductClickListener
        ) {
            binding.tvProductName.setText(product.getName());
            binding.tvProductPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
            binding.tvRating.setText(String.valueOf(product.getRating() == 0 ? 5.0f : product.getRating()));

            if (product.hasActiveDiscount()) {
                binding.tvDiscountBadge.setText(calculateDiscountPercentage(product));
                binding.tvDiscountBadge.setVisibility(View.VISIBLE);
                binding.tvOriginalPrice.setText(CurrencyFormatter.formatVnd(product.getOriginalPrice()));
                binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                binding.tvDiscountBadge.setVisibility(View.GONE);
                binding.tvOriginalPrice.setVisibility(View.GONE);
            }

            TastePreferenceStore tasteStore = new TastePreferenceStore(binding.getRoot().getContext());
            String tasteWarning = tasteStore.productWarning(product);
            if (tasteWarning.isEmpty()) {
                binding.tvTasteTag.setVisibility(android.view.View.GONE);
            } else {
                binding.tvTasteTag.setVisibility(android.view.View.VISIBLE);
                binding.tvTasteTag.setText(tasteWarning);
            }

            ProductImageUtils.loadProductImage(binding.getRoot().getContext(), binding.imgProduct, product);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
            binding.btnAddProduct.getRoot().setOnClickListener(v -> {
                if (addProductClickListener != null) {
                    addProductClickListener.onAddProductClick(product);
                } else if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }

        private String calculateDiscountPercentage(Product product) {
            if (product.getOriginalPrice() <= 0) return "";
            long discount = product.getOriginalPrice() - product.getPrice();
            int percentage = (int) ((discount * 100.0f) / product.getOriginalPrice());
            return "-" + percentage + "%";
        }
    }
}
