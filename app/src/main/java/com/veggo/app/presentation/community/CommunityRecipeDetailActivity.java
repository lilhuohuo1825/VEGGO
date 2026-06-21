package com.veggo.app.presentation.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.veggo.app.data.remote.dto.RecipeDetailDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.repository.RecipeRepository;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.util.List;

public class CommunityRecipeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "community_recipe_detail_recipe_id";
    public static final String EXTRA_INSTRUCTION_ID = "community_recipe_detail_instruction_id";

    private CommunityRepository repository;
    private RecipeRepository recipeRepository;
    private String videoUrl;
    private String recipeId;
    private String instructionId;
    private GridLayout ingredientsGrid;
    private LinearLayout instructionsContainer;
    private LinearLayout galleryRow;
    private LinearLayout commentsContainer;
    private View editButton;
    private View deleteButton;
    private View bookmarkButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipe_detail);

        repository = new CommunityRepository(this);
        recipeRepository = AppModule.provideRecipeRepository();
        ingredientsGrid = findViewById(R.id.recipeIngredientsGrid);
        instructionsContainer = findViewById(R.id.recipeInstructionsContainer);
        galleryRow = findViewById(R.id.recipeGalleryRow);
        commentsContainer = findViewById(R.id.recipeCommentsContainer);
        findViewById(R.id.recipeBackButton).setOnClickListener(v -> finish());

        instructionId = getIntent().getStringExtra(EXTRA_INSTRUCTION_ID);
        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);

        findViewById(R.id.recipeShopButton).setOnClickListener(v -> openIngredients());
        editButton = findViewById(R.id.recipeEditButton);
        editButton.setVisibility(View.GONE);
        deleteButton = findViewById(R.id.recipeDeleteButton);
        deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(v -> showDeleteRecipeDialog());
        bookmarkButton = findViewById(R.id.recipeBookmarkButton);
        ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, false);
        bookmarkButton.setOnClickListener(v -> {
            if (recipeId != null) {
                CommunityUi.showAddToCookbook(this, recipeId, () ->
                        ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, true));
            }
        });
        findViewById(R.id.recipeCommentSend).setOnClickListener(v -> submitComment());

        if (instructionId != null && !instructionId.isEmpty()) {
            loadRemoteRecipeDetail(instructionId);
        } else {
            repository.loadRecipeDetail(recipeId, data -> runOnUiThread(() -> bindDetail(data)));
            loadBookmarkState();
        }
    }

    private void loadRemoteRecipeDetail(String id) {
        recipeRepository.getRecipeDetail(id, new RecipeRepository.Callback<RecipeDetailDto>() {
            @Override
            public void onSuccess(RecipeDetailDto result) {
                runOnUiThread(() -> bindRemoteDetail(result));
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(CommunityRecipeDetailActivity.this::finish);
            }
        });
    }

    private void bindRemoteDetail(RecipeDetailDto data) {
        if (data == null || data.getInstruction() == null) {
            finish();
            return;
        }

        RecipeDetailDto.InstructionSummaryDto instruction = data.getInstruction();
        videoUrl = instruction.getVideo();

        ((TextView) findViewById(R.id.recipeTitle)).setText(instruction.getTitle());
        String meta = buildMeta(instruction.getCookingTime(), data.getDishes());
        ((TextView) findViewById(R.id.recipeMeta)).setText(meta);
        ((TextView) findViewById(R.id.recipeCalories)).setText("—\nCalories");
        ((TextView) findViewById(R.id.recipeSalt)).setText(
                isBlank(instruction.getDifficulty()) ? "—\nĐộ khó" : instruction.getDifficulty() + "\nĐộ khó");
        ((TextView) findViewById(R.id.recipeSugar)).setText(
                isBlank(instruction.getServings()) ? "—\nKhẩu phần" : instruction.getServings() + "\nKhẩu phần");

        findViewById(R.id.recipeAuthorRow).setVisibility(View.GONE);
        Glide.with(this)
                .load(instruction.getImage())
                .transform(new CenterCrop(), new RoundedCorners(dp(10)))
                .into((ImageView) findViewById(R.id.recipeHeroImage));
        findViewById(R.id.recipePlay).setOnClickListener(v -> openVideo());

        bindRemoteIngredients(data.getDishes());
        bindRemoteInstructions(data.getDishes(), instruction.getDescription());
        galleryRow.removeAllViews();
        commentsContainer.removeAllViews();
    }

    private String buildMeta(String cookingTime, List<RecipeDetailDto.DishDetailDto> dishes) {
        int ingredientCount = 0;
        if (dishes != null) {
            for (RecipeDetailDto.DishDetailDto dish : dishes) {
                if (dish.getIngredients() != null) {
                    ingredientCount += dish.getIngredients().size();
                }
            }
        }
        String time = isBlank(cookingTime) ? "—" : cookingTime;
        return "\u25CB  " + time + "   \u25CB  " + ingredientCount + " nguyên liệu";
    }

    private void bindRemoteIngredients(List<RecipeDetailDto.DishDetailDto> dishes) {
        ingredientsGrid.removeAllViews();
        if (dishes == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (RecipeDetailDto.DishDetailDto dish : dishes) {
            if (dish.getIngredients() == null) continue;
            for (String ingredient : dish.getIngredients()) {
                View item = inflater.inflate(R.layout.item_community_recipe_ingredient, ingredientsGrid, false);
                ((TextView) item.findViewById(R.id.ingredientName)).setText(ingredient);
                ((TextView) item.findViewById(R.id.ingredientQuantity)).setText("");
                item.findViewById(R.id.ingredientImage).setVisibility(View.GONE);
                TextView emoji = item.findViewById(R.id.ingredientEmoji);
                emoji.setVisibility(View.VISIBLE);
                emoji.setText("\uD83C\uDF31");

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = dp(122);
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(dp(4), dp(4), dp(4), dp(8));
                ingredientsGrid.addView(item, params);
            }
        }
    }

    private void bindRemoteInstructions(List<RecipeDetailDto.DishDetailDto> dishes, String description) {
        instructionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int stepIndex = 1;

        if (!isBlank(description)) {
            addInstructionStep(inflater, stepIndex++, description);
        }

        if (dishes != null) {
            for (RecipeDetailDto.DishDetailDto dish : dishes) {
                if (isBlank(dish.getSteps())) continue;
                String[] steps = dish.getSteps().split("\\n");
                for (String step : steps) {
                    if (isBlank(step)) continue;
                    addInstructionStep(inflater, stepIndex++, step.trim());
                }
            }
        }

        if (instructionsContainer.getChildCount() == 0) {
            addInstructionStep(inflater, 1, "Sơ chế nguyên liệu.");
            addInstructionStep(inflater, 2, "Nấu món ăn theo khẩu vị.");
        }
    }

    private void addInstructionStep(LayoutInflater inflater, int number, String text) {
        View item = inflater.inflate(R.layout.item_community_recipe_instruction, instructionsContainer, false);
        ((TextView) item.findViewById(R.id.instructionNumber)).setText(String.valueOf(number));
        ((TextView) item.findViewById(R.id.instructionText)).setText(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = instructionsContainer.getChildCount() == 0 ? 0 : dp(10);
        instructionsContainer.addView(item, params);
    }

    private void loadBookmarkState() {
        repository.isRecipeSaved(recipeId, saved -> runOnUiThread(() ->
                ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, saved)));
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
        bindOwnerAction(data);
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
            commentsContainer.addView(commentView(inflater, data.comments.get(i)));
        }
    }

    private void bindOwnerAction(CommunityRepository.RecipeDetailData data) {
        if (data.recipe == null) {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            return;
        }
        boolean isOwner = data.recipe.getChefId() != null
                && data.recipe.getChefId().equals(repository.currentCustomerId());
        editButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        if (isOwner) {
            editButton.setOnClickListener(v -> openEditRecipe(data.recipe.getId()));
        }
    }

    private void openEditRecipe(String editRecipeId) {
        Intent intent = new Intent(this, CommunityPostActivity.class);
        intent.putExtra(CommunityPostActivity.EXTRA_EDIT_RECIPE_ID, editRecipeId);
        startActivity(intent);
    }

    private void showDeleteRecipeDialog() {
        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                "X\u00f3a c\u00f4ng th\u1ee9c?",
                "C\u00f4ng th\u1ee9c n\u00e0y s\u1ebd b\u1ecb x\u00f3a v\u0129nh vi\u1ec5n kh\u1ecfi c\u1ed9ng \u0111\u1ed3ng.",
                "X\u00f3a",
                "H\u1ee7y",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        deleteRecipe();
                    }
                }
        );
    }

    private void deleteRecipe() {
        deleteButton.setEnabled(false);
        repository.deleteRecipe(recipeId, deleted -> runOnUiThread(() -> {
            deleteButton.setEnabled(true);
            if (deleted) {
                Toast.makeText(this, "\u0110\u00e3 x\u00f3a c\u00f4ng th\u1ee9c", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
                return;
            }
            Toast.makeText(this, "Kh\u00f4ng th\u1ec3 x\u00f3a c\u00f4ng th\u1ee9c", Toast.LENGTH_SHORT).show();
        }));
    }

    private void submitComment() {
        EditText input = findViewById(R.id.recipeCommentInput);
        String content = input.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        repository.createComment(recipeId, content, comment -> runOnUiThread(() -> {
            if (comment != null) {
                commentsContainer.addView(commentView(LayoutInflater.from(this), comment), 0);
            }
            input.setText("");
        }));
    }

    private View commentView(LayoutInflater inflater, CommunityRecipeCommentEntity comment) {
        View item = inflater.inflate(R.layout.item_community_recipe_comment, commentsContainer, false);
        ((TextView) item.findViewById(R.id.commentName)).setText(comment.getUserName());
        ((TextView) item.findViewById(R.id.commentContent)).setText(comment.getContent());
        TextView meta = item.findViewById(R.id.commentMeta);
        bindCommentLike(meta, comment);
        Glide.with(this)
                .load(comment.getUserImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(18)))
                .into((ImageView) item.findViewById(R.id.commentAvatar));
        return item;
    }

    private void bindCommentLike(TextView meta, CommunityRecipeCommentEntity comment) {
        meta.setText("\u2665  " + comment.getLikeCount());
        meta.setTextColor(getColor(comment.isLikedByCurrentUser() ? R.color.danger_main : R.color.neutral_60));
        meta.setOnClickListener(v -> {
            meta.setEnabled(false);
            repository.toggleCommentLike(comment.getId(), updated -> runOnUiThread(() -> {
                meta.setEnabled(true);
                if (updated == null) {
                    Toast.makeText(this, "Chưa thả tim được bình luận", Toast.LENGTH_SHORT).show();
                    return;
                }
                comment.setLikeCount(updated.getLikeCount());
                comment.setLikedByCurrentUser(updated.isLikedByCurrentUser());
                bindCommentLike(meta, comment);
            }));
        });
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
        if (instructionId != null && !instructionId.isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, CommunityIngredientsActivity.class);
        intent.putExtra(CommunityIngredientsActivity.EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void openChefProfile(CommunityChefEntity chef) {
        Intent intent = new Intent(this, CommunityProfileActivity.class);
        if (chef.getId().equals(repository.currentCustomerId())) {
            intent.putExtra(CommunityProfileActivity.EXTRA_ACCOUNT_PROFILE, true);
            startActivity(intent);
            return;
        }
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_ID, chef.getId());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_NAME, chef.getName());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_IMAGE_URL, chef.getImageUrl());
        intent.putExtra(CommunityProfileActivity.EXTRA_RECIPE_COUNT, chef.getRecipeCount());
        intent.putExtra(CommunityProfileActivity.EXTRA_LIKES, chef.getLikes());
        startActivity(intent);
    }
}
