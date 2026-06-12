package com.veggo.app.presentation.community;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityRecipeCommentEntity;
import com.veggo.app.data.local.entity.CommunityRecipeDetailEntity;
import com.veggo.app.data.local.entity.CommunityRecipeGalleryEntity;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;
import com.veggo.app.data.local.entity.ProductEntity;

public class CommunityRecipeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "community_recipe_detail_recipe_id";

    private CommunityRepository repository;
    private String videoUrl;
    private String recipeId;
    private GridLayout ingredientsGrid;
    private LinearLayout instructionsContainer;
    private LinearLayout galleryRow;
    private LinearLayout commentsContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipe_detail);

        repository = new CommunityRepository(this);
        ingredientsGrid = findViewById(R.id.recipeIngredientsGrid);
        instructionsContainer = findViewById(R.id.recipeInstructionsContainer);
        galleryRow = findViewById(R.id.recipeGalleryRow);
        commentsContainer = findViewById(R.id.recipeCommentsContainer);
        findViewById(R.id.recipeBackButton).setOnClickListener(v -> finish());

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        findViewById(R.id.recipeShopButton).setOnClickListener(v -> openIngredients());
        ToggleUi.bindToggle((ImageButton) findViewById(R.id.recipeHeartButton), R.drawable.ic_heart_green, R.drawable.ic_heart_full, false);
        ImageButton bookmarkButton = findViewById(R.id.recipeBookmarkButton);
        bookmarkButton.setOnClickListener(v -> {
            bookmarkButton.setBackgroundResource(R.drawable.bg_follow_button_green);
            bookmarkButton.setColorFilter(Color.WHITE);
            CommunityUi.showAddToCookbook(this, recipeId);
        });
        findViewById(R.id.recipeCommentSend).setOnClickListener(v -> submitComment());
        repository.loadRecipeDetail(recipeId, data -> runOnUiThread(() -> bindDetail(data)));
    }

    private void bindDetail(CommunityRepository.RecipeDetailData data) {
        if (data.recipe == null) {
            finish();
            return;
        }
        CommunityRecipeDetailEntity detail = data.detail;
        videoUrl = detail == null ? null : detail.getVideoUrl();
        ((TextView) findViewById(R.id.recipeTitle)).setText(data.recipe.getTitle());
        ((TextView) findViewById(R.id.recipeMeta)).setText("\u25CB  " + data.recipe.getTimeMinutes() + " min   \u25CB  " + data.recipe.getIngredientCount() + " nguyên liệu");
        ((TextView) findViewById(R.id.recipeCalories)).setText((detail == null ? 230 : detail.getCalories()) + "\nCalories");
        ((TextView) findViewById(R.id.recipeSalt)).setText((detail == null ? "Thấp" : detail.getSaltLevel()) + "\nMuối");
        ((TextView) findViewById(R.id.recipeSugar)).setText((detail == null ? "Thấp" : detail.getSugarLevel()) + "\nĐường");

        if (data.chef != null) {
            ((TextView) findViewById(R.id.recipeChefName)).setText(data.chef.getName());
            ((TextView) findViewById(R.id.recipeChefCount)).setText(data.chef.getRecipeCount() + " công thức");
            Glide.with(this).load(data.chef.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(20))).into((ImageView) findViewById(R.id.recipeChefAvatar));
            findViewById(R.id.recipeAuthorRow).setOnClickListener(v -> openChefProfile(data.chef));
        }
        Glide.with(this).load(data.recipe.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(10))).into((ImageView) findViewById(R.id.recipeHeroImage));
        findViewById(R.id.recipePlay).setOnClickListener(v -> openVideo());

        bindIngredients(data);
        bindInstructions(detail == null ? "" : detail.getInstructions());
        bindGallery(data);
        bindComments(data);
    }

    private void bindIngredients(CommunityRepository.RecipeDetailData data) {
        ingredientsGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CommunityRecipeIngredientEntity ingredient : data.ingredients) {
            ProductEntity product = data.productsById.get(ingredient.getProductId());
            View item = inflater.inflate(R.layout.item_community_recipe_ingredient, ingredientsGrid, false);
            String name = isBlank(ingredient.getDisplayName())
                    ? (product == null ? "Sản phẩm" : product.getName())
                    : ingredient.getDisplayName();
            ((TextView) item.findViewById(R.id.ingredientName)).setText(name);
            ((TextView) item.findViewById(R.id.ingredientQuantity)).setText(ingredient.getQuantity());
            ImageView image = item.findViewById(R.id.ingredientImage);
            TextView emoji = item.findViewById(R.id.ingredientEmoji);
            if (!isBlank(ingredient.getIconUrl())) {
                image.setVisibility(View.VISIBLE);
                emoji.setVisibility(View.GONE);
                Glide.with(this).load(ingredient.getIconUrl()).transform(new CenterCrop(), new RoundedCorners(dp(12))).into(image);
            } else if (!isBlank(ingredient.getIconEmoji())) {
                image.setVisibility(View.GONE);
                emoji.setVisibility(View.VISIBLE);
                emoji.setText(ingredient.getIconEmoji());
            } else if (product != null) {
                image.setVisibility(View.VISIBLE);
                emoji.setVisibility(View.GONE);
                Glide.with(this).load(product.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(12))).into(image);
            }
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(122);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(8));
            ingredientsGrid.addView(item, params);
        }
    }

    private void bindInstructions(String instructions) {
        instructionsContainer.removeAllViews();
        String[] steps = instructions == null || instructions.isEmpty()
                ? new String[]{"Sơ chế nguyên liệu.", "Nấu món ăn theo khẩu vị."}
                : instructions.split("\\n");
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < steps.length; i++) {
            View item = inflater.inflate(R.layout.item_community_recipe_instruction, instructionsContainer, false);
            ((TextView) item.findViewById(R.id.instructionNumber)).setText(String.valueOf(i + 1));
            ((TextView) item.findViewById(R.id.instructionText)).setText(steps[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
            params.topMargin = i == 0 ? 0 : dp(10);
            instructionsContainer.addView(item, params);
        }
    }

    private void bindGallery(CommunityRepository.RecipeDetailData data) {
        galleryRow.removeAllViews();
        for (CommunityRecipeGalleryEntity gallery : data.galleries) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            if (galleryRow.getChildCount() > 0) {
                params.leftMargin = dp(10);
            }
            galleryRow.addView(image, params);
            Glide.with(this).load(gallery.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(10))).into(image);
        }
    }

    private void bindComments(CommunityRepository.RecipeDetailData data) {
        commentsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int limit = Math.min(5, data.comments.size());
        for (int i = 0; i < limit; i++) {
            CommunityRecipeCommentEntity comment = data.comments.get(i);
            View item = inflater.inflate(R.layout.item_community_recipe_comment, commentsContainer, false);
            ((TextView) item.findViewById(R.id.commentName)).setText(comment.getUserName());
            ((TextView) item.findViewById(R.id.commentContent)).setText(comment.getContent());
            ((TextView) item.findViewById(R.id.commentMeta)).setText("\u2665  " + comment.getLikeCount() + "     Reply");
            Glide.with(this).load(comment.getUserImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(18))).into((ImageView) item.findViewById(R.id.commentAvatar));
            commentsContainer.addView(item);
        }
    }

    private void submitComment() {
        EditText input = findViewById(R.id.recipeCommentInput);
        String content = input.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        View item = LayoutInflater.from(this).inflate(R.layout.item_community_recipe_comment, commentsContainer, false);
        ((TextView) item.findViewById(R.id.commentName)).setText(CommunityRepository.ACCOUNT_NAME);
        ((TextView) item.findViewById(R.id.commentContent)).setText(content);
        ((TextView) item.findViewById(R.id.commentMeta)).setText("\u2665  0     Reply");
        Glide.with(this)
                .load(CommunityRepository.ACCOUNT_AVATAR_URL)
                .transform(new CenterCrop(), new RoundedCorners(dp(18)))
                .into((ImageView) item.findViewById(R.id.commentAvatar));
        commentsContainer.addView(item, 0);
        input.setText("");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void openVideo() {
        if (isBlank(videoUrl)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
        startActivity(intent);
    }

    private void openIngredients() {
        Intent intent = new Intent(this, CommunityIngredientsActivity.class);
        intent.putExtra(CommunityIngredientsActivity.EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void openChefProfile(CommunityChefEntity chef) {
        Intent intent = new Intent(this, CommunityProfileActivity.class);
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_ID, chef.getId());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_NAME, chef.getName());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_IMAGE_URL, chef.getImageUrl());
        intent.putExtra(CommunityProfileActivity.EXTRA_RECIPE_COUNT, chef.getRecipeCount());
        intent.putExtra(CommunityProfileActivity.EXTRA_LIKES, chef.getLikes());
        startActivity(intent);
    }
}
