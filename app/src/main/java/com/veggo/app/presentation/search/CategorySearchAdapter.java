package com.veggo.app.presentation.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemCategorySearchBinding;
import java.util.ArrayList;
import java.util.List;

public class CategorySearchAdapter extends RecyclerView.Adapter<CategorySearchAdapter.ViewHolder> {
    private List<AssetModels.Category> categories = new ArrayList<>();
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(AssetModels.Category category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<AssetModels.Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategorySearchBinding binding = ItemCategorySearchBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetModels.Category category = categories.get(position);
        holder.binding.txtName.setText(category.categoryName);
        // Assuming there's a way to get product count or just hide it
        holder.binding.txtCount.setVisibility(View.GONE);

        int iconRes = getCategoryIcon(category.categoryId);
        Glide.with(holder.itemView.getContext())
                .load(iconRes)
                .into(holder.binding.imgCategory);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    private int getCategoryIcon(String categoryId) {
        if (categoryId == null) return R.drawable.ic_category;
        switch (categoryId) {
            case "CAT001": case "6a0960fc6ea39eac566cfc14": 
                return R.drawable.ic_coffee_green;
            case "CAT002": case "6a0960fc6ea39eac566cfc15": 
                return R.drawable.ic_grain_dark;
            case "CAT003": case "6a0960fc6ea39eac566cfc16": 
                return R.drawable.ic_vegetable;
            case "CAT004": case "6a0960fc6ea39eac566cfc17": 
                return R.drawable.ic_seaweed;
            case "CAT005": case "6a0960fc6ea39eac566cfc18": 
                return R.drawable.ic_nutritous;
            case "CAT006": case "6a0960fc6ea39eac566cfc19": 
                return R.drawable.ic_dryfood;
            case "CAT007": case "6a0960fc6ea39eac566cfc1a": 
                return R.drawable.ic_leaf;
            case "CAT008": case "6a0960fc6ea39eac566cfc1b": 
                return R.drawable.ic_fruit;
            default: 
                return R.drawable.ic_category;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemCategorySearchBinding binding;
        ViewHolder(ItemCategorySearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
