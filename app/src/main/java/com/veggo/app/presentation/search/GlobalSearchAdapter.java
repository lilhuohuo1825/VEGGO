package com.veggo.app.presentation.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PRODUCT = 1;
    private static final int TYPE_FEATURE = 2;

    private List<Object> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onProductClick(Product product);
        void onFeatureClick(SearchViewModel.FeatureResult feature);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setResults(List<Product> products, List<SearchViewModel.FeatureResult> features) {
        items.clear();
        if (features != null && !features.isEmpty()) {
            items.add("Tính năng");
            items.addAll(features);
        }
        if (products != null && !products.isEmpty()) {
            items.add("Sản phẩm");
            items.addAll(products);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) return TYPE_HEADER;
        if (item instanceof Product) return TYPE_PRODUCT;
        return TYPE_FEATURE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_search_header, parent, false));
        } else if (viewType == TYPE_PRODUCT) {
            return new ProductViewHolder(inflater.inflate(R.layout.item_search_product, parent, false));
        } else {
            return new FeatureViewHolder(inflater.inflate(R.layout.item_search_feature, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) item);
        } else if (holder instanceof ProductViewHolder) {
            Product product = (Product) item;
            ((ProductViewHolder) holder).bind(product);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        } else if (holder instanceof FeatureViewHolder) {
            SearchViewModel.FeatureResult feature = (SearchViewModel.FeatureResult) item;
            ((FeatureViewHolder) holder).tvName.setText(feature.getName());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFeatureClick(feature);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tvHeader);
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice;

        ProductViewHolder(View v) {
            super(v);
            ivProduct = v.findViewById(R.id.ivProduct);
            tvName = v.findViewById(R.id.tvProductName);
            tvPrice = v.findViewById(R.id.tvProductPrice);
        }

        void bind(Product product) {
            tvName.setText(product.getName());
            tvPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
            Glide.with(itemView.getContext()).load(product.getImageUrl()).into(ivProduct);
        }
    }

    static class FeatureViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        FeatureViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvFeatureName);
        }
    }
}
