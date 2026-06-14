package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.domain.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private List<Recipe> recipes = new ArrayList<>();

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvRecipeName.setText(recipe.getName());
        holder.tvRecipePrice.setText(recipe.getPrice());
        holder.tvCookingTime.setText("Cooking time: " + recipe.getCookingTime());
        holder.rbRecipeRating.setRating(recipe.getRating());
        holder.tvRecipeRatingValue.setText(String.valueOf(recipe.getRating()));
        holder.tvRecipeReviewCount.setText("(" + recipe.getReviewCount() + ")");
        
        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.logo)
                .into(holder.ivRecipeImage);

        holder.ivBookmark.setImageResource(recipe.isBookmarked() ? R.drawable.ic_save : R.drawable.ic_save);
        // Note: For bookmark toggle, you'd need another icon or change tint
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        ImageView ivBookmark;
        TextView tvRecipePrice;
        TextView tvCookingTime;
        TextView tvRecipeName;
        RatingBar rbRecipeRating;
        TextView tvRecipeRatingValue;
        TextView tvRecipeReviewCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            ivBookmark = itemView.findViewById(R.id.ivBookmark);
            tvRecipePrice = itemView.findViewById(R.id.tvRecipePrice);
            tvCookingTime = itemView.findViewById(R.id.tvCookingTime);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            rbRecipeRating = itemView.findViewById(R.id.rbRecipeRating);
            tvRecipeRatingValue = itemView.findViewById(R.id.tvRecipeRatingValue);
            tvRecipeReviewCount = itemView.findViewById(R.id.tvRecipeReviewCount);
        }
    }
}
