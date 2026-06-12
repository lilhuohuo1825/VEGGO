package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.domain.model.Recipe;

public class RecipeAdapter extends ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder> {

    public RecipeAdapter() {
        super(new DiffUtil.ItemCallback<Recipe>() {
            @Override
            public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
                return oldItem.getName().equals(newItem.getName()) && 
                       oldItem.getImageRes() == newItem.getImageRes() &&
                       (oldItem.getImageUrl() == null ? newItem.getImageUrl() == null : oldItem.getImageUrl().equals(newItem.getImageUrl()));
            }
        });
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgRecipe;
        private final TextView tvRecipeName;
        private final TextView tvRecipeTime;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvRecipeTime = itemView.findViewById(R.id.tvRecipeTime);
        }

        void bind(Recipe recipe) {
            tvRecipeName.setText(recipe.getName());
            tvRecipeTime.setVisibility(View.GONE);
            
            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(imgRecipe.getContext())
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.onboarding_2)
                        .error(R.drawable.onboarding_2)
                        .into(imgRecipe);
            } else if (recipe.getImageRes() != 0) {
                imgRecipe.setImageResource(recipe.getImageRes());
            }
        }
    }
}
