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
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PRODUCT = 1;
    private static final int TYPE_FEATURE = 2;
    private static final int TYPE_CATEGORY = 3;

    private List<Object> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onProductClick(Product product);
        void onFeatureClick(SearchViewModel.FeatureResult feature);
        void onCategoryClick(AssetModels.Category category);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setResults(
            List<Product> products,
            List<SearchViewModel.FeatureResult> features,
            List<AssetModels.Category> categories
    ) {
        items.clear();
        if (categories != null && !categories.isEmpty()) {
            items.add("Danh mục");
            items.addAll(categories);
        }
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
        if (item instanceof AssetModels.Category) return TYPE_CATEGORY;
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
        } else if (viewType == TYPE_CATEGORY) {
            return new CategoryViewHolder(inflater.inflate(R.layout.item_category_search, parent, false));
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
        } else if (holder instanceof CategoryViewHolder) {
            AssetModels.Category category = (AssetModels.Category) item;
            ((CategoryViewHolder) holder).bind(category);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(category);
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

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgCategory;
        private final TextView tvName;

        CategoryViewHolder(View v) {
            super(v);
            imgCategory = v.findViewById(R.id.imgCategory);
            tvName = v.findViewById(R.id.txtName);
            View countView = v.findViewById(R.id.txtCount);
            if (countView != null) {
                countView.setVisibility(View.GONE);
            }
        }

        void bind(AssetModels.Category category) {
            tvName.setText(category.categoryName);
            Glide.with(itemView.getContext())
                    .load(resolveCategoryIcon(category.categoryId))
                    .into(imgCategory);
        }

        private int resolveCategoryIcon(String categoryId) {
            if (categoryId == null) {
                return R.drawable.ic_category;
            }
            switch (categoryId) {
                case "CAT001":
                case "6a0960fc6ea39eac566cfc14":
                    return R.drawable.ic_coffee_green;
                case "CAT002":
                case "6a0960fc6ea39eac566cfc15":
                    return R.drawable.ic_grain_dark;
                case "CAT003":
                case "6a0960fc6ea39eac566cfc16":
                    return R.drawable.ic_vegetable;
                case "CAT004":
                case "6a0960fc6ea39eac566cfc17":
                    return R.drawable.ic_seaweed;
                case "CAT005":
                case "6a0960fc6ea39eac566cfc18":
                    return R.drawable.ic_nutritous;
                case "CAT006":
                case "6a0960fc6ea39eac566cfc19":
                    return R.drawable.ic_dryfood;
                case "CAT007":
                case "6a0960fc6ea39eac566cfc1a":
                    return R.drawable.ic_leaf;
                case "CAT008":
                case "6a0960fc6ea39eac566cfc1b":
                    return R.drawable.ic_fruit;
                default:
                    return R.drawable.ic_category;
            }
        }
    }
}
