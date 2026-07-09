package com.veggo.app.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.domain.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class PopularRecipeAdapter extends RecyclerView.Adapter<PopularRecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final List<Recipe> recipes = new ArrayList<>();
    private OnRecipeClickListener onRecipeClickListener;
    private final int cardHeightPx;
    private final int horizontalMarginPx;
    private final int bottomMarginPx;
    private final int cornerRadiusPx;

    public PopularRecipeAdapter(int cardHeightPx, int horizontalMarginPx, int bottomMarginPx, int cornerRadiusPx) {
        this.cardHeightPx = cardHeightPx;
        this.horizontalMarginPx = horizontalMarginPx;
        this.bottomMarginPx = bottomMarginPx;
        this.cornerRadiusPx = cornerRadiusPx;
    }

    public void submitList(List<Recipe> items) {
        recipes.clear();
        if (items != null) {
            recipes.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.onRecipeClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_image_card, parent, false);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                cardHeightPx
        );
        params.setMargins(horizontalMarginPx, 0, horizontalMarginPx, bottomMarginPx);
        view.setLayoutParams(params);
        View root = view.findViewById(R.id.imageCardRoot);
        if (root != null) {
            root.setBackgroundColor(Color.TRANSPARENT);
            root.setClipToOutline(true);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.cardTitle.setText(recipe.getName());
        String cookingTime = recipe.getCookingTime();
        if (cookingTime == null || cookingTime.trim().isEmpty()) {
            holder.cardMeta.setVisibility(View.GONE);
        } else {
            holder.cardMeta.setVisibility(View.VISIBLE);
            holder.cardMeta.setText("\u25CF  " + cookingTime.trim());
        }
        holder.cardSubtitle.setVisibility(View.GONE);
        holder.cardSave.setVisibility(View.VISIBLE);

        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(cornerRadiusPx))
                .placeholder(R.drawable.logo)
                .into(holder.cardImage);

        FavoriteStore favoriteStore = new FavoriteStore(holder.itemView.getContext());
        boolean selected = favoriteStore.isFavorite(FavoriteStore.TYPE_RECIPE, recipe.getId());
        renderFavoriteState(holder.cardSave, selected);
        holder.cardSave.setOnClickListener(v -> {
            boolean nowSelected = favoriteStore.toggle(new FavoriteStore.FavoriteItem(
                    FavoriteStore.TYPE_RECIPE,
                    recipe.getId(),
                    recipe.getName(),
                    favoriteSubtitle(recipe),
                    recipe.getImageUrl()
            ));
            renderFavoriteState(holder.cardSave, nowSelected);
            Toast.makeText(
                    holder.itemView.getContext(),
                    nowSelected ? "Đã thêm vào yêu thích" : "Đã xoá khỏi yêu thích",
                    Toast.LENGTH_SHORT
            ).show();
        });

        holder.itemView.setOnClickListener(v -> {
            if (onRecipeClickListener == null) {
                return;
            }
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION
                    || clickedPosition < 0
                    || clickedPosition >= recipes.size()) {
                return;
            }
            onRecipeClickListener.onRecipeClick(recipes.get(clickedPosition));
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

    private void renderFavoriteState(ImageView save, boolean selected) {
        save.setBackgroundResource(selected
                ? R.drawable.bg_follow_button_green
                : R.drawable.bg_community_save);
        save.setImageResource(selected
                ? R.drawable.ic_profile_menu_heart_filled
                : R.drawable.ic_heart_outline_green);
        save.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(save.getContext(), R.color.neutral_10)));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cardImage;
        final ImageView cardSave;
        final TextView cardMeta;
        final TextView cardTitle;
        final TextView cardSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);
            cardSave = itemView.findViewById(R.id.cardSave);
            cardMeta = itemView.findViewById(R.id.cardMeta);
            cardTitle = itemView.findViewById(R.id.cardTitle);
            cardSubtitle = itemView.findViewById(R.id.cardSubtitle);
        }
    }
}
