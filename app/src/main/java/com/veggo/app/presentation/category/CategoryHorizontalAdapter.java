package com.veggo.app.presentation.category;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemCategoryHorizontalBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryHorizontalAdapter extends RecyclerView.Adapter<CategoryHorizontalAdapter.ViewHolder> {
    private List<AssetModels.Category> categories = new ArrayList<>();
    private String selectedCategoryId = "";
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(AssetModels.Category category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<AssetModels.Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedCategoryId(String id) {
        this.selectedCategoryId = id != null ? id : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryHorizontalBinding binding = ItemCategoryHorizontalBinding.inflate(
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
        private final ItemCategoryHorizontalBinding binding;

        ViewHolder(ItemCategoryHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AssetModels.Category category) {
            binding.tvCategoryName.setText(category.categoryName);

            boolean isSelected = category.categoryId.equals(selectedCategoryId);
            
            // Set card border stroke and background based on selection
            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            binding.cvCategoryIcon.setStrokeWidth(isSelected ? (int)(2 * density) : 0);
            binding.cvCategoryIcon.setCardBackgroundColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_bg : R.color.neutral_20));
            
            binding.tvCategoryName.setTextColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_main : R.color.neutral_100));

            // Bind category icon
            binding.ivCategoryIcon.setImageResource(getCategoryIcon(category.categoryId));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }

        private int getCategoryIcon(String categoryId) {
            switch (categoryId != null ? categoryId : "") {
                case "CAT001": return R.drawable.ic_coffee_green;
                case "CAT002": return R.drawable.ic_grain_dark;
                case "CAT003": return R.drawable.ic_vegetable;
                case "CAT004": return R.drawable.ic_leaf;
                case "CAT005": return R.drawable.ic_success;
                case "CAT006": return R.drawable.ic_grain_dark;
                case "CAT007": return R.drawable.ic_leaf;
                case "CAT008": return R.drawable.ic_fruit;
                default: return R.drawable.ic_vegetable;
            }
        }
    }
}
