package com.veggo.app.adapter;

import android.text.TextUtils;
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
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.data.remote.dto.ChatSuggestedProductDto;
import com.veggo.app.data.remote.dto.ChatSuggestedRecipeDto;
import com.veggo.app.databinding.ItemProductGridBinding;
import com.veggo.app.domain.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final float HORIZONTAL_PRODUCT_CARD_WIDTH_DP = 168f;
    private static final float HORIZONTAL_PRODUCT_CARD_MARGIN_END_DP = 12f;

    public interface OnSuggestionClickListener {
        void onRecipeClick(String instructionId);
        void onProductClick(String productId, boolean openAddToCart);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private OnSuggestionClickListener suggestionClickListener;

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.suggestionClickListener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeLastMessage() {
        if (messages.isEmpty()) {
            return;
        }
        int lastIndex = messages.size() - 1;
        messages.remove(lastIndex);
        notifyItemRemoved(lastIndex);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_WELCOME) {
            return new WelcomeViewHolder(inflater.inflate(R.layout.item_chat_welcome, parent, false));
        }
        if (viewType == ChatMessage.TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
        }
        if (viewType == ChatMessage.TYPE_PRODUCT_LIST) {
            return new ProductListViewHolder(inflater.inflate(R.layout.item_chat_product_list, parent, false));
        }
        if (viewType == ChatMessage.TYPE_RECIPE_LIST) {
            return new RecipeListViewHolder(inflater.inflate(R.layout.item_chat_recipe_list, parent, false));
        }
        return new BotViewHolder(inflater.inflate(R.layout.item_chat_bot, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof WelcomeViewHolder) {
            ((WelcomeViewHolder) holder).tvMessage.setText(message.getMessage());
            return;
        }
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(message.getMessage());
            return;
        }
        if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvMessage.setText(message.getMessage());
            return;
        }
        if (holder instanceof ProductListViewHolder) {
            bindProductListViewHolder((ProductListViewHolder) holder, message.getProducts());
            return;
        }
        if (holder instanceof RecipeListViewHolder) {
            bindRecipeListViewHolder((RecipeListViewHolder) holder, message.getRecipes());
        }
    }

    private void bindProductListViewHolder(ProductListViewHolder holder, List<ChatSuggestedProductDto> products) {
        List<ChatSuggestedProductDto> sourceProducts = filterProductSuggestions(products);
        if (sourceProducts.isEmpty()) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        if (holder.productCardContainer == null) {
            return;
        }
        holder.productCardContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        int cardWidth = Math.round(HORIZONTAL_PRODUCT_CARD_WIDTH_DP * density);
        int marginEnd = Math.round(HORIZONTAL_PRODUCT_CARD_MARGIN_END_DP * density);

        for (ChatSuggestedProductDto product : sourceProducts) {
            ItemProductGridBinding cardBinding = ItemProductGridBinding.inflate(
                    inflater,
                    holder.productCardContainer,
                    false
            );
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    cardWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(marginEnd);
            cardBinding.getRoot().setLayoutParams(params);
            bindChatProductCard(cardBinding, product);
            holder.productCardContainer.addView(cardBinding.getRoot());
        }
    }

    private void bindChatProductCard(ItemProductGridBinding binding, ChatSuggestedProductDto product) {
        final String productId = product.getProductId();
        binding.tvProductName.setText(product.getName());
        binding.tvProductPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
        binding.tvRating.setText("5.0");
        binding.tvDiscountBadge.setVisibility(View.GONE);
        binding.tvOriginalPrice.setVisibility(View.GONE);
        binding.tvTasteTag.setVisibility(View.GONE);

        Glide.with(binding.getRoot().getContext())
                .load(product.getImage())
                .placeholder(R.drawable.ic_leaf)
                .error(R.drawable.ic_leaf)
                .into(binding.imgProduct);

        binding.getRoot().setOnClickListener(v -> {
            if (suggestionClickListener == null || TextUtils.isEmpty(productId)) {
                return;
            }
            suggestionClickListener.onProductClick(productId, false);
        });
        binding.btnAddProduct.getRoot().setOnClickListener(v -> {
            if (suggestionClickListener == null || TextUtils.isEmpty(productId)) {
                return;
            }
            suggestionClickListener.onProductClick(productId, true);
        });
    }

    private List<ChatSuggestedProductDto> filterProductSuggestions(List<ChatSuggestedProductDto> products) {
        List<ChatSuggestedProductDto> filtered = new ArrayList<>();
        if (products == null) {
            return filtered;
        }
        for (ChatSuggestedProductDto product : products) {
            if (product == null || TextUtils.isEmpty(product.getName()) || TextUtils.isEmpty(product.getProductId())) {
                continue;
            }
            filtered.add(product);
        }
        return filtered;
    }

    private void bindRecipeListViewHolder(RecipeListViewHolder holder, List<ChatSuggestedRecipeDto> recipes) {
        List<ChatSuggestedRecipeDto> sourceRecipes = filterRecipeSuggestions(recipes);
        if (sourceRecipes.isEmpty()) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        if (holder.recipeCardContainer == null) {
            return;
        }
        holder.recipeCardContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (ChatSuggestedRecipeDto recipe : sourceRecipes) {
            View card = inflater.inflate(R.layout.item_recipe, holder.recipeCardContainer, false);
            bindChatRecipeCard(card, recipe);
            holder.recipeCardContainer.addView(card);
        }
    }

    private void bindChatRecipeCard(View card, ChatSuggestedRecipeDto recipe) {
        final String instructionId = recipe.resolveInstructionId();

        TextView tvRecipeName = card.findViewById(R.id.tvRecipeName);
        TextView tvCookingTime = card.findViewById(R.id.tvCookingTime);
        ImageView ivRecipeImage = card.findViewById(R.id.ivRecipeImage);
        RatingBar rbRecipeRating = card.findViewById(R.id.rbRecipeRating);
        TextView tvRecipeRatingValue = card.findViewById(R.id.tvRecipeRatingValue);
        TextView tvRecipeReviewCount = card.findViewById(R.id.tvRecipeReviewCount);
        View ivBookmark = card.findViewById(R.id.ivBookmark);

        tvRecipeName.setText(recipe.getTitle());
        rbRecipeRating.setVisibility(View.GONE);
        tvRecipeRatingValue.setVisibility(View.GONE);
        tvRecipeReviewCount.setVisibility(View.GONE);
        if (ivBookmark != null) {
            ivBookmark.setVisibility(View.GONE);
        }

        String cookingTime = recipe.getCookingTime() != null ? recipe.getCookingTime().trim() : "";
        if (TextUtils.isEmpty(cookingTime)) {
            tvCookingTime.setVisibility(View.GONE);
        } else {
            tvCookingTime.setVisibility(View.VISIBLE);
            tvCookingTime.setText(card.getContext().getString(R.string.recipe_cooking_time_format, cookingTime));
        }

        Glide.with(card.getContext())
                .load(recipe.getImage())
                .placeholder(R.drawable.logo)
                .into(ivRecipeImage);

        card.setOnClickListener(v -> {
            if (suggestionClickListener == null || TextUtils.isEmpty(instructionId)) {
                return;
            }
            suggestionClickListener.onRecipeClick(instructionId);
        });
    }

    private List<ChatSuggestedRecipeDto> filterRecipeSuggestions(List<ChatSuggestedRecipeDto> recipes) {
        List<ChatSuggestedRecipeDto> filtered = new ArrayList<>();
        if (recipes == null) {
            return filtered;
        }

        for (ChatSuggestedRecipeDto recipe : recipes) {
            if (recipe == null || TextUtils.isEmpty(recipe.resolveInstructionId()) || TextUtils.isEmpty(recipe.getTitle())) {
                continue;
            }
            filtered.add(recipe);
        }
        return filtered;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class WelcomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        WelcomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class ProductListViewHolder extends RecyclerView.ViewHolder {
        ViewGroup productCardContainer;

        ProductListViewHolder(@NonNull View itemView) {
            super(itemView);
            productCardContainer = itemView.findViewById(R.id.productCardContainer);
        }
    }

    static class RecipeListViewHolder extends RecyclerView.ViewHolder {
        ViewGroup recipeCardContainer;

        RecipeListViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeCardContainer = itemView.findViewById(R.id.recipeCardContainer);
        }
    }
}
