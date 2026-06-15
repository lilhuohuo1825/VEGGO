package com.veggo.app.presentation.category;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

<<<<<<< Updated upstream
=======
import com.bumptech.glide.Glide;
import com.veggo.app.R;
>>>>>>> Stashed changes
import com.veggo.app.assets.AssetModels;
import com.veggo.app.databinding.ItemSubcategoryBinding;

import java.util.ArrayList;
import java.util.List;

public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.ViewHolder> {
    private List<AssetModels.Subcategory> subcategories = new ArrayList<>();

    public void setSubcategories(List<AssetModels.Subcategory> subcategories) {
        this.subcategories = subcategories != null ? subcategories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubcategoryBinding binding = ItemSubcategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetModels.Subcategory subcategory = subcategories.get(position);
        holder.bind(subcategory);
    }

    @Override
    public int getItemCount() {
        return subcategories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubcategoryBinding binding;

        ViewHolder(ItemSubcategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AssetModels.Subcategory subcategory) {
            binding.tvSubcategoryName.setText(subcategory.subcategoryName);
<<<<<<< Updated upstream
            // binding.ivSubcategoryIcon.setImageResource(...);
=======
            
            if (subcategory.img != null && !subcategory.img.isEmpty()) {
                Glide.with(binding.ivSubcategoryIcon.getContext())
                        .load(subcategory.img)
                        .placeholder(R.drawable.ic_vegetable)
                        .error(getSubcategoryIcon(subcategory.subcategoryId))
                        .into(binding.ivSubcategoryIcon);
            } else {
                binding.ivSubcategoryIcon.setImageResource(getSubcategoryIcon(subcategory.subcategoryId));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubcategoryClick(subcategory);
                }
            });
        }

        private int getSubcategoryIcon(String subcategoryId) {
            if (subcategoryId != null) {
                if (subcategoryId.contains("SUB003")) {
                    return R.drawable.ic_vegetable;
                } else if (subcategoryId.contains("SUB008")) {
                    return R.drawable.ic_fruit;
                } else if (subcategoryId.contains("SUB001")) {
                    return R.drawable.ic_coffee_green;
                } else if (subcategoryId.contains("SUB007")) {
                    return R.drawable.ic_leaf;
                }
            }
            return R.drawable.ic_vegetable;
>>>>>>> Stashed changes
        }
    }
}
