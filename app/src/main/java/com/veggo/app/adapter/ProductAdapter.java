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

    private List<Product> productList = new ArrayList<>();

    public void submitList(List<Product> list) {
        this.productList = list;
        notifyDataSetChanged();
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
        holder.bind(productList.get(position));
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductGridBinding binding;

        public ProductViewHolder(ItemProductGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Product product) {
            binding.tvProductName.setText(product.getProductName());
            binding.tvProductPrice.setText(CurrencyFormatter.formatVnd((long) product.getPrice()));
            binding.tvRating.setText(String.valueOf(product.getRating() == 0 ? 5.0f : product.getRating()));
            
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.ic_leaf)
                        .error(R.drawable.ic_leaf)
                        .into(binding.imgProduct);
            } else {
                binding.imgProduct.setImageResource(R.drawable.ic_leaf);
            }
        }
    }
}