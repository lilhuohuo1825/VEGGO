package com.veggo.app.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.domain.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private List<Recipe> recipes = new ArrayList<>();
    private OnRecipeClickListener onRecipeClickListener;

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    public void submitList(List<Recipe> recipes) {
        setRecipes(recipes);
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.onRecipeClickListener = listener;
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

        String cookingTime = recipe.getCookingTime();
        if (cookingTime == null || cookingTime.trim().isEmpty()) {
            holder.tvCookingTime.setVisibility(View.GONE);
        } else {
            holder.tvCookingTime.setVisibility(View.VISIBLE);
            holder.tvCookingTime.setText(
                    holder.itemView.getContext().getString(R.string.recipe_cooking_time_format, cookingTime));
        }

        if (recipe.getRating() <= 0f && recipe.getReviewCount() <= 0) {
            holder.rbRecipeRating.setVisibility(View.GONE);
            holder.tvRecipeRatingValue.setVisibility(View.GONE);
            holder.tvRecipeReviewCount.setVisibility(View.GONE);
        } else {
            holder.rbRecipeRating.setVisibility(View.VISIBLE);
            holder.tvRecipeRatingValue.setVisibility(View.VISIBLE);
            holder.tvRecipeReviewCount.setVisibility(View.VISIBLE);
            holder.rbRecipeRating.setRating(recipe.getRating());
            holder.tvRecipeRatingValue.setText(String.valueOf(recipe.getRating()));
            holder.tvRecipeReviewCount.setText("(" + recipe.getReviewCount() + ")");
        }
        
        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.logo)
                .into(holder.ivRecipeImage);

        FavoriteStore favoriteStore = new FavoriteStore(holder.itemView.getContext());
        boolean selected = favoriteStore.isFavorite(FavoriteStore.TYPE_RECIPE, recipe.getId());
        renderFavoriteIcon(holder, selected);
        holder.ivBookmark.setOnClickListener(v -> {
            boolean nowSelected = favoriteStore.toggle(new FavoriteStore.FavoriteItem(
                    FavoriteStore.TYPE_RECIPE,
                    recipe.getId(),
                    recipe.getName(),
                    favoriteSubtitle(recipe),
                    recipe.getImageUrl()
            ));
            renderFavoriteIcon(holder, nowSelected);
            Toast.makeText(
                    holder.itemView.getContext(),
                    nowSelected ? "Đã thêm vào yêu thích" : "Đã xoá khỏi yêu thích",
                    Toast.LENGTH_SHORT
            ).show();
        });

        holder.itemView.setOnClickListener(v -> {
            if (onRecipeClickListener != null) {
                onRecipeClickListener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    private String favoriteSubtitle(Recipe recipe) {
        StringBuilder subtitle = new StringBuilder();
        if (recipe.getCookingTime() != null && !recipe.getCookingTime().trim().isEmpty()) {
            subtitle.append(recipe.getCookingTime().trim());
        }
        if (recipe.getIngredientCount() > 0) {
            if (subtitle.length() > 0) {
                subtitle.append(" • ");
            }
            subtitle.append(recipe.getIngredientCount()).append(" nguyên liệu");
        }
        return subtitle.toString();
    }

    private void renderFavoriteIcon(ViewHolder holder, boolean selected) {
        holder.ivBookmark.setImageResource(selected
                ? R.drawable.ic_profile_menu_heart_filled
                : R.drawable.ic_heart_outline_green);
        int color = ContextCompat.getColor(holder.itemView.getContext(),
                selected ? R.color.primary_main : R.color.white);
        holder.ivBookmark.setImageTintList(ColorStateList.valueOf(color));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        ImageView ivBookmark;
        TextView tvCookingTime;
        TextView tvRecipeName;
        RatingBar rbRecipeRating;
        TextView tvRecipeRatingValue;
        TextView tvRecipeReviewCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            ivBookmark = itemView.findViewById(R.id.ivBookmark);
            tvCookingTime = itemView.findViewById(R.id.tvCookingTime);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            rbRecipeRating = itemView.findViewById(R.id.rbRecipeRating);
            tvRecipeRatingValue = itemView.findViewById(R.id.tvRecipeRatingValue);
            tvRecipeReviewCount = itemView.findViewById(R.id.tvRecipeReviewCount);
        }
    }
}
