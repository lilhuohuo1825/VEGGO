package com.veggo.app.presentation.category;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
            // binding.ivSubcategoryIcon.setImageResource(...);
        }
    }
}
