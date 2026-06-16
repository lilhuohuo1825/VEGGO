package com.veggo.app.presentation.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemCategorySearchBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategorySearchAdapter extends RecyclerView.Adapter<CategorySearchAdapter.ViewHolder> {
    private List<AssetModels.Category> categories = new ArrayList<>();
    private Map<String, Integer> categoryProductCounts = new HashMap<>();
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(AssetModels.Category category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<AssetModels.Category> categories, Map<String, Integer> counts) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.categoryProductCounts = counts != null ? counts : new HashMap<>();
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
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategorySearchBinding binding;

        ViewHolder(ItemCategorySearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AssetModels.Category category) {
            binding.txtName.setText(category.categoryName);

            int count = categoryProductCounts.containsKey(category.categoryId) 
                    ? categoryProductCounts.get(category.categoryId) : 0;
            binding.txtCount.setText(count + " sản phẩm");

            // Map category icon
            int iconRes = getCategoryIcon(category.categoryId);
            binding.imgCategory.setImageResource(iconRes);

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
                case "CAT004": return R.drawable.ic_seaweed;
                case "CAT005": return R.drawable.ic_nutritous;
                case "CAT006": return R.drawable.ic_dryfood;
                case "CAT007": return R.drawable.ic_leaf;
                case "CAT008": return R.drawable.ic_fruit;
                default: return R.drawable.ic_vegetable;
            }
        }
    }
}
