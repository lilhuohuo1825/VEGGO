package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.ProductDisplayValidator;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.databinding.ItemRelatedProductBinding;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {

    private List<Product> products = new ArrayList<>();
    private OnAddClickListener addListener;
    private OnProductClickListener productClickListener;

    public interface OnAddClickListener {
        void onAdd(Product product);
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public void setOnAddClickListener(OnAddClickListener l) {
        this.addListener = l;
    }

    public void setOnProductClickListener(OnProductClickListener l) {
        this.productClickListener = l;
    }

    public void setProducts(List<Product> products) {
        this.products = products == null ? new ArrayList<>() : products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRelatedProductBinding binding = ItemRelatedProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRelatedProductBinding binding;

        public ViewHolder(@NonNull ItemRelatedProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Product product) {
            binding.tvRelatedTitle.setText(product.getName());
            if (ProductDisplayValidator.hasValidWeight(product.getWeight())) {
                binding.tvRelatedWeight.setVisibility(android.view.View.VISIBLE);
                binding.tvRelatedWeight.setText(product.getWeight());
            } else {
                binding.tvRelatedWeight.setVisibility(android.view.View.GONE);
            }
            binding.tvRelatedPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));

            if (product.hasActiveDiscount()) {
                binding.tvRelatedOriginalPrice.setVisibility(android.view.View.VISIBLE);
                binding.tvRelatedOriginalPrice.setText(CurrencyFormatter.formatVnd(product.getOriginalPrice()));
                binding.tvRelatedOriginalPrice.setPaintFlags(binding.tvRelatedOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvDiscountBadge.setVisibility(android.view.View.VISIBLE);
                binding.tvDiscountBadge.setText(calculateDiscountPercentage(product));
            } else {
                binding.tvRelatedOriginalPrice.setVisibility(android.view.View.GONE);
                binding.tvDiscountBadge.setVisibility(android.view.View.GONE);
            }

            ProductImageUtils.loadProductImage(binding.getRoot().getContext(), binding.ivRelatedProduct, product);

            binding.getRoot().setOnClickListener(v -> {
                if (productClickListener != null) productClickListener.onProductClick(product);
            });

            binding.btnAddRelated.setOnClickListener(v -> {
                if (addListener != null) addListener.onAdd(product);
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
