package com.veggo.app.presentation.community;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;
import com.veggo.app.data.local.entity.ProductEntity;

public class CommunityIngredientsActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "community_ingredients_recipe_id";

    private CommunityRepository repository;
    private LinearLayout list;
    private View bookmarkButton;
    private String recipeId;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_ingredients);

        repository = new CommunityRepository(this);
        list = findViewById(R.id.ingredientsList);
        findViewById(R.id.ingredientsBackButton).setOnClickListener(v -> finish());
        ToggleUi.bindToggle(findViewById(R.id.ingredientsHeartButton), R.drawable.ic_heart_green, R.drawable.ic_heart_full, false);
        bookmarkButton = findViewById(R.id.ingredientsBookmarkButton);
        bookmarkButton.setOnClickListener(v -> toggleBookmark());

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.ingredientsScroll, this::loadData);
        loadData();
        loadBookmarkState();
    }

    private void loadData() {
        repository.loadRecipeDetail(recipeId, data -> runOnUiThread(() -> {
            bindData(data);
            CommunityUi.finishRefresh(refreshLayout);
        }));
    }

    private void loadBookmarkState() {
        repository.isRecipeSaved(recipeId, saved -> runOnUiThread(() ->
                ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, saved)));
    }

    private void toggleBookmark() {
        Object tag = bookmarkButton.getTag();
        boolean saved = tag instanceof Boolean && (Boolean) tag;
        if (saved) {
            repository.removeRecipeFromCookbooks(recipeId, done -> runOnUiThread(() -> {
                if (done) {
                    ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, false);
                }
            }));
            return;
        }
        CommunityUi.showAddToCookbook(this, recipeId, () ->
                ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, true));
    }

    private void bindData(CommunityRepository.RecipeDetailData data) {
        if (data.recipe == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.ingredientsHeroTitle)).setText(data.recipe.getTitle());
        ((TextView) findViewById(R.id.ingredientsHeroMeta)).setText("\u25CB  " + data.recipe.getTimeMinutes() + " min   \u25CB  " + data.recipe.getIngredientCount() + " nguyên liệu");
        Glide.with(this)
                .load(data.recipe.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(8)))
                .into((ImageView) findViewById(R.id.ingredientsHeroImage));

        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < data.ingredients.size(); index++) {
            CommunityRecipeIngredientEntity ingredient = data.ingredients.get(index);
            ProductEntity product = data.productsById.get(ingredient.getProductId());
            View row = inflater.inflate(R.layout.item_community_ingredient_check, list, false);
            bindIngredient(row, ingredient, product);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            );
            if (index > 0) {
                params.topMargin = dp(8);
            }
            list.addView(row, params);
        }
    }

    private void bindIngredient(View row, CommunityRecipeIngredientEntity ingredient, ProductEntity product) {
        String name = isBlank(ingredient.getDisplayName())
                ? (product == null ? "Sản phẩm" : product.getName())
                : ingredient.getDisplayName();
        ((TextView) row.findViewById(R.id.ingredientCheckName)).setText(name);
        ((TextView) row.findViewById(R.id.ingredientCheckQuantity)).setText(ingredient.getQuantity());

        ImageView image = row.findViewById(R.id.ingredientCheckImage);
        TextView emoji = row.findViewById(R.id.ingredientCheckEmoji);
        if (!isBlank(ingredient.getIconUrl())) {
            image.setVisibility(View.VISIBLE);
            emoji.setVisibility(View.GONE);
            Glide.with(this).load(ingredient.getIconUrl()).transform(new CenterCrop(), new RoundedCorners(dp(8))).into(image);
        } else if (!isBlank(ingredient.getIconEmoji())) {
            image.setVisibility(View.GONE);
            emoji.setVisibility(View.VISIBLE);
            emoji.setText(ingredient.getIconEmoji());
        } else if (product != null) {
            image.setVisibility(View.VISIBLE);
            emoji.setVisibility(View.GONE);
            Glide.with(this).load(product.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(8))).into(image);
        }

        ImageButton checkbox = row.findViewById(R.id.ingredientCheckButton);
        bindCheckbox(checkbox, false);
    }

    private void bindCheckbox(ImageButton checkbox, boolean checked) {
        checkbox.setTag(checked);
        renderCheckbox(checkbox, checked);
        checkbox.setOnClickListener(v -> {
            boolean next = !(Boolean) v.getTag();
            v.setTag(next);
            renderCheckbox((ImageButton) v, next);
        });
    }

    private void renderCheckbox(ImageButton checkbox, boolean checked) {
        checkbox.setBackgroundResource(checked ? R.drawable.bg_checkbox_checked : R.drawable.bg_checkbox_unchecked);
        checkbox.setImageResource(checked ? R.drawable.ic_check : 0);
        checkbox.setColorFilter(checked ? Color.WHITE : Color.TRANSPARENT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
