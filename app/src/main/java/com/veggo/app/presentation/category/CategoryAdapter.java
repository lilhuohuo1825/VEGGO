package com.veggo.app.presentation.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemCategorySidebarBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<AssetModels.Category> categories = new ArrayList<>();
    private String selectedCategoryId = "";
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(AssetModels.Category category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<AssetModels.Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategorySidebarBinding binding = ItemCategorySidebarBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetModels.Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategorySidebarBinding binding;

        ViewHolder(ItemCategorySidebarBinding binding) {
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

            // Map icon based on categoryId or name if available, for now use default
            // binding.ivCategoryIcon.setImageResource(...);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
}
