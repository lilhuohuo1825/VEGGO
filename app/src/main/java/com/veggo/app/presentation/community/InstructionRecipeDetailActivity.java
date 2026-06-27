package com.veggo.app.presentation.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.CheckBox;
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
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.favorite.FavoriteStore;
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

public class InstructionRecipeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "community_recipe_detail_recipe_id";
    public static final String EXTRA_INSTRUCTION_ID = "community_recipe_detail_instruction_id";

    private CommunityRepository repository;
    private RecipeRepository recipeRepository;
    private FavoriteStore favoriteStore;
    private String videoUrl;
    private String recipeId;
    private String instructionId;
    private String currentFavoriteId;
    private String currentRecipeTitle;
    private String currentRecipeImage;
    private String currentRecipeSubtitle;
    private LinearLayout ingredientsGrid;
    private LinearLayout instructionsContainer;
    private LinearLayout galleryRow;
    private LinearLayout commentsContainer;
    private View editButton;
    private View deleteButton;
    private View bookmarkButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction_recipe_detail);

        repository = new CommunityRepository(this);
        recipeRepository = AppModule.provideRecipeRepository();
        favoriteStore = new FavoriteStore(this);
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
        ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_heart_outline_green, false);
        bookmarkButton.setOnClickListener(v -> toggleRecipeFavorite());
        findViewById(R.id.recipeShareButton).setOnClickListener(v -> shareRecipe());
        findViewById(R.id.recipeCommentSend).setOnClickListener(v -> submitComment());

        if (instructionId != null && !instructionId.isEmpty()) {
            loadRemoteRecipeDetail(instructionId);
        } else {
            repository.loadRecipeDetail(recipeId, data -> runOnUiThread(() -> bindDetail(data)));
            loadFavoriteState();
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
                runOnUiThread(InstructionRecipeDetailActivity.this::finish);
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
        currentFavoriteId = isBlank(instructionId) ? recipeId : instructionId;
        currentRecipeTitle = instruction.getTitle();
        currentRecipeImage = instruction.getImage();
        currentRecipeSubtitle = recipeFavoriteSubtitle(
                instruction.getCookingTime(),
                countRemoteIngredients(data.getDishes()));

        ((TextView) findViewById(R.id.recipeTitle)).setText(instruction.getTitle());
        String meta = buildMeta(instruction.getCookingTime(), data.getDishes());
        ((TextView) findViewById(R.id.recipeMeta)).setText(meta);

        findViewById(R.id.recipeAuthorRow).setVisibility(View.GONE);
        hideRemoteOnlyChrome();
        Glide.with(this)
                .load(instruction.getImage())
                .transform(new CenterCrop(), new RoundedCorners(dp(10)))
                .into((ImageView) findViewById(R.id.recipeHeroImage));
        findViewById(R.id.recipePlay).setOnClickListener(v -> openVideo());

        bindRemoteDescription(data);
        bindRemoteIngredients(data.getDishes());
        bindRemoteInstructions(data.getDishes(), instruction.getDescription());
        bindRemoteUsage(data.getDishes());
        galleryRow.removeAllViews();
        commentsContainer.removeAllViews();
        loadFavoriteState();
    }

    private void hideRemoteOnlyChrome() {
        int[] ids = {
                R.id.recipeStats,
                R.id.recipeShopButton,
                R.id.recipeGallerySectionHeader,
                R.id.recipeGalleryRow,
                R.id.recipeCommentsSectionHeader,
                R.id.recipeCommentsContainer,
                R.id.recipeDiscussBox,
        };
        for (int id : ids) {
            View view = findViewById(id);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }

    private void bindRemoteDescription(RecipeDetailDto data) {
        String assetDescription = resolveAssetDishDescription(instructionId);
        String description = assetDescription;
        if (data.getDishes() != null) {
            for (RecipeDetailDto.DishDetailDto dish : data.getDishes()) {
                if (isBlank(description) && !isBlank(dish.getDescription())) {
                    description = dish.getDescription();
                    break;
                }
            }
        }
        if (isBlank(description) && data.getInstruction() != null) {
            description = data.getInstruction().getDescription();
        }
        setSectionText(R.id.recipeDescriptionTitle, R.id.recipeDescriptionText, description);
    }

    private String resolveAssetDishDescription(String currentInstructionId) {
        if (isBlank(currentInstructionId)) {
            return "";
        }
        try {
            AssetJsonLoader loader = new AssetJsonLoader(this);
            List<AssetModels.Instruction> instructions = loader.readList(AssetFiles.INSTRUCTIONS, AssetModels.Instruction.class);
            String legacyDishId = "";
            for (AssetModels.Instruction instruction : instructions) {
                if (instruction == null) {
                    continue;
                }
                String objectId = instruction.objectId == null ? "" : instruction.objectId.oid;
                if (currentInstructionId.equals(instruction.id) || currentInstructionId.equals(objectId)) {
                    legacyDishId = instruction.id;
                    break;
                }
            }
            if (isBlank(legacyDishId)) {
                legacyDishId = currentInstructionId;
            }

            List<AssetModels.Dish> dishes = loader.readList(AssetFiles.DISHES, AssetModels.Dish.class);
            for (AssetModels.Dish dish : dishes) {
                if (dish != null && legacyDishId.equals(dish.id) && !isBlank(dish.description)) {
                    return dish.description;
                }
            }
        } catch (Exception ignored) {
            // Remote data remains the primary source; asset lookup only patches legacy recipe details.
        }
        return "";
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
                addIngredientChecklistItem(inflater, ingredient);
            }
        }
    }

    private void addIngredientChecklistItem(LayoutInflater inflater, String ingredientText) {
        if (isBlank(ingredientText)) {
            return;
        }
        View item = inflater.inflate(R.layout.item_recipe_ingredient_check, ingredientsGrid, false);
        CheckBox checkBox = item.findViewById(R.id.ingredientCheckBox);
        ((TextView) item.findViewById(R.id.ingredientCheckText)).setText(ingredientText.trim());
        item.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));
        ingredientsGrid.addView(item);
    }

    private void bindRemoteInstructions(List<RecipeDetailDto.DishDetailDto> dishes, String description) {
        instructionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int stepIndex = 1;

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

    private void bindRemoteUsage(List<RecipeDetailDto.DishDetailDto> dishes) {
        String usage = "";
        if (dishes != null) {
            for (RecipeDetailDto.DishDetailDto dish : dishes) {
                if (!isBlank(dish.getUsage())) {
                    usage = dish.getUsage();
                    break;
                }
            }
        }
        setSectionText(R.id.recipeUsageTitle, R.id.recipeUsageText, usage);
    }

    private void setSectionText(int titleId, int textId, String value) {
        View title = findViewById(titleId);
        TextView text = findViewById(textId);
        boolean hasValue = !isBlank(value);
        if (title != null) {
            title.setVisibility(hasValue ? View.VISIBLE : View.GONE);
        }
        if (text != null) {
            text.setVisibility(hasValue ? View.VISIBLE : View.GONE);
            text.setText(hasValue ? value.trim() : "");
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

    private void loadFavoriteState() {
        ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_heart_outline_green,
                favoriteStore != null
                        && !isBlank(currentFavoriteId)
                        && favoriteStore.isFavorite(FavoriteStore.TYPE_RECIPE, currentFavoriteId));
    }

    private void bindDetail(CommunityRepository.RecipeDetailData data) {
        if (data.recipe == null) {
            finish();
            return;
        }
        CommunityRecipeDetailEntity detail = data.detail;
        videoUrl = detail == null ? null : detail.getVideoUrl();
        currentFavoriteId = data.recipe.getId();
        currentRecipeTitle = data.recipe.getTitle();
        currentRecipeImage = data.recipe.getImageUrl();
        currentRecipeSubtitle = recipeFavoriteSubtitle(
                data.recipe.getTimeMinutes() + " phút",
                data.recipe.getIngredientCount());
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

        setSectionText(R.id.recipeDescriptionTitle, R.id.recipeDescriptionText, "");
        bindIngredients(data);
        bindInstructions(detail == null ? "" : detail.getInstructions());
        setSectionText(R.id.recipeUsageTitle, R.id.recipeUsageText, "");
        bindGallery(data);
        bindComments(data);
        loadFavoriteState();
    }

    private void toggleRecipeFavorite() {
        if (isBlank(currentFavoriteId)) {
            return;
        }
        boolean selected = favoriteStore.toggle(new FavoriteStore.FavoriteItem(
                FavoriteStore.TYPE_RECIPE,
                currentFavoriteId,
                currentRecipeTitle,
                currentRecipeSubtitle,
                currentRecipeImage
        ));
        ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_heart_outline_green, selected);
        Toast.makeText(this,
                selected ? "Đã thêm vào yêu thích" : "Đã xoá khỏi yêu thích",
                Toast.LENGTH_SHORT).show();
    }

    private void shareRecipe() {
        if (isBlank(currentRecipeTitle)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, currentRecipeTitle);
        startActivity(Intent.createChooser(intent, "Chia sẻ công thức"));
    }

    private int countRemoteIngredients(List<RecipeDetailDto.DishDetailDto> dishes) {
        int count = 0;
        if (dishes == null) {
            return count;
        }
        for (RecipeDetailDto.DishDetailDto dish : dishes) {
            if (dish.getIngredients() != null) {
                count += dish.getIngredients().size();
            }
        }
        return count;
    }

    private String recipeFavoriteSubtitle(String cookingTime, int ingredientCount) {
        StringBuilder subtitle = new StringBuilder();
        if (!isBlank(cookingTime)) {
            subtitle.append(cookingTime.trim());
        }
        if (ingredientCount > 0) {
            if (subtitle.length() > 0) {
                subtitle.append(" • ");
            }
            subtitle.append(ingredientCount).append(" nguyên liệu");
        }
        return subtitle.toString();
    }

    private void bindIngredients(CommunityRepository.RecipeDetailData data) {
        ingredientsGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CommunityRecipeIngredientEntity ingredient : data.ingredients) {
            ProductEntity product = data.productsById.get(ingredient.getProductId());
            String name = isBlank(ingredient.getDisplayName())
                    ? (product == null ? "Sản phẩm" : product.getName())
                    : ingredient.getDisplayName();
            String quantity = ingredient.getQuantity();
            addIngredientChecklistItem(inflater, isBlank(quantity) ? name : name + " - " + quantity);
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
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
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
