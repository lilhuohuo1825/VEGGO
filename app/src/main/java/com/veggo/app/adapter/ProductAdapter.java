package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.databinding.ItemProductGridBinding;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products = new ArrayList<>();
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
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
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener);
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

        public void bind(final Product product, final OnProductClickListener listener) {
            binding.tvProductName.setText(product.getName());
            binding.tvProductPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
            binding.tvRating.setText(String.valueOf(product.getRating() == 0 ? 5.0f : product.getRating()));
            
            Glide.with(binding.getRoot().getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_leaf)
                    .error(R.drawable.ic_leaf)
                    .into(binding.imgProduct);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}
