package com.veggo.app.presentation.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemCategoryHorizontalBinding;
import com.veggo.app.databinding.ItemCategorySidebarBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_SIDEBAR = 0;
    public static final int TYPE_HORIZONTAL = 1;

    private final int viewType;
    private List<AssetModels.Category> categories = new ArrayList<>();
    private String selectedCategoryId = "";
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(AssetModels.Category category);
    }

    public CategoryAdapter(int viewType) {
        this.viewType = viewType;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<AssetModels.Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Alias for compatibility with HorizontalAdapter's setData
    public void setData(List<AssetModels.Category> categories) {
        setCategories(categories);
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId != null ? selectedCategoryId : "";
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HORIZONTAL) {
            ItemCategoryHorizontalBinding binding = ItemCategoryHorizontalBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new HorizontalViewHolder(binding);
        } else {
            ItemCategorySidebarBinding binding = ItemCategorySidebarBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new SidebarViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AssetModels.Category category = categories.get(position);
        if (holder instanceof SidebarViewHolder) {
            ((SidebarViewHolder) holder).bind(category);
        } else if (holder instanceof HorizontalViewHolder) {
            ((HorizontalViewHolder) holder).bind(category);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    private int getCategoryIcon(String categoryId) {
        if (categoryId == null) return R.drawable.ic_category;
        
        // Handle both CategoryID (CATxxx) and Mongo _id ($oid)
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

    class SidebarViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategorySidebarBinding binding;

        SidebarViewHolder(ItemCategorySidebarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AssetModels.Category category) {
            binding.tvCategoryName.setText(category.categoryName);
            
            boolean isSelected = category.categoryId.equals(selectedCategoryId);
            binding.selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            binding.rootView.setBackgroundResource(isSelected ? R.color.background_main : android.R.color.transparent);
            binding.tvCategoryName.setTextColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_main : R.color.neutral_80));

            binding.ivCategoryIcon.setImageResource(getCategoryIcon(category.categoryId));
            binding.ivCategoryIcon.setColorFilter(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_main : R.color.neutral_60));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }

    class HorizontalViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryHorizontalBinding binding;

        HorizontalViewHolder(ItemCategoryHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AssetModels.Category category) {
            binding.tvCategoryName.setText(category.categoryName);

            boolean isSelected = category.categoryId.equals(selectedCategoryId);
            
            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            binding.cvCategoryIcon.setStrokeWidth(isSelected ? (int)(2 * density) : 0);
            binding.cvCategoryIcon.setStrokeColor(binding.getRoot().getContext().getColor(R.color.primary_main));
            binding.cvCategoryIcon.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.primary_bg));
            
            binding.tvCategoryName.setTextColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_main : R.color.neutral_100));

            binding.ivCategoryIcon.setImageResource(getCategoryIcon(category.categoryId));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
}
