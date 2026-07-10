package com.veggo.app.presentation.community;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.data.local.entity.CommunityCategoryEntity;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityCookbookEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;
import com.veggo.app.databinding.ComponentBottomNavBinding;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public final class CommunityUi {
    public interface SaveCallback {
        void onSaved();
    }

    private CommunityUi() {
    }

    public static void setupBottomNav(Activity activity, ComponentBottomNavBinding bottomNavigationBinding) {
        applyTopSystemInset(activity);
        BottomNavController.setup(activity, bottomNavigationBinding, R.id.nav_category);
    }

    private static void applyTopSystemInset(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View header = findFixedHeader(activity);
        if (header != null) {
            ViewGroup.LayoutParams params = header.getLayoutParams();
            int baseTopMargin = 0;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                baseTopMargin = ((ViewGroup.MarginLayoutParams) params).topMargin;
            }
            final int baseMargin = baseTopMargin;
            ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
                int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    mlp.topMargin = baseMargin + statusTop;
                    view.setLayoutParams(mlp);
                } else {
                    int left = view.getPaddingLeft();
                    int top = view.getPaddingTop();
                    int right = view.getPaddingRight();
                    int bottom = view.getPaddingBottom();
                    view.setPadding(left, top + statusTop, right, bottom);
                }
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
            return;
        }

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        View root = content.getChildAt(0);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(left, top + statusTop, right, bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private static View findFixedHeader(Activity activity) {
        int[] headerIds = {
                R.id.communityHomeHeader,
                R.id.communityTopHeader,
                R.id.discoveryTopHeader,
                R.id.cookbookTopHeader
        };
        for (int id : headerIds) {
            View header = activity.findViewById(id);
            if (header != null) {
                return header;
            }
        }
        return null;
    }

    public static void setupTopHeader(Activity activity, String title) {
        TextView titleView = activity.findViewById(R.id.communityTitle);
        View backButton = activity.findViewById(R.id.communityBackButton);
        if (titleView != null) {
            titleView.setText(title);
        }
        if (backButton != null) {
            backButton.setOnClickListener(v -> activity.finish());
        }
        View searchButton = activity.findViewById(R.id.communitySearchButton);
        if (searchButton != null) {
            searchButton.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, CommunityDiscoveryActivity.class)));
        }
    }

    public static SwipeRefreshLayout setupPullToRefresh(Activity activity, int contentViewId, Runnable refreshAction) {
        View content = activity.findViewById(contentViewId);
        SwipeRefreshLayout refreshLayout = com.veggo.app.core.ui.PullToRefreshHelper.wrap(content, refreshAction);
        if (refreshLayout != null) {
            refreshLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        return refreshLayout;
    }

    public static void finishRefresh(SwipeRefreshLayout refreshLayout) {
        com.veggo.app.core.ui.PullToRefreshHelper.finish(refreshLayout);
    }

    public static <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>();
        if (items != null) {
            copy.addAll(items);
        }
        Collections.shuffle(copy);
        return copy;
    }

    public static void addCategoryChips(Activity activity, LinearLayout row, List<CommunityCategoryEntity> categories) {
        row.removeAllViews();
        row.addView(chip(activity, row, R.drawable.ic_category2, "", null, v ->
                activity.startActivity(new Intent(activity, CommunityCategoriesActivity.class))
        ));
        int limit = Math.min(categories.size(), 10);
        for (int index = 0; index < limit; index++) {
            CommunityCategoryEntity category = categories.get(index);
            String iconEmoji = isBlank(category.getIconEmoji()) ? categoryEmoji(category.getName()) : category.getIconEmoji();
            row.addView(chip(activity, row, 0, category.getName().toLowerCase(), iconEmoji, v ->
                    openCategoryRecipes(activity, category)
            ));
        }
    }

    public static void addChefPreview(Activity activity, LinearLayout row, List<CommunityChefEntity> chefs) {
        row.removeAllViews();
        int limit = Math.min(chefs.size(), 4);
        for (int index = 0; index < limit; index++) {
            View item = LayoutInflater.from(activity).inflate(R.layout.item_community_chef_preview, row, false);
            CommunityChefEntity chef = chefs.get(index);
            bindChefPreview(activity, item, chef);
            item.setOnClickListener(v -> openChefProfile(activity, chef));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            if (index > 0) {
                params.leftMargin = dp(activity, 10);
            }
            row.addView(item, params);
        }
    }

    public static void addRecipePreview(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityRecipeEntity> recipes
    ) {
        addRecipePreview(activity, leftColumn, rightColumn, recipes, null, null);
    }

    public static void addRecipePreview(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityRecipeEntity> recipes,
            @Nullable CommunityRepository repository,
            @Nullable Set<String> savedRecipeIds
    ) {
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        int limit = Math.min(recipes.size(), 4);
        for (int index = 0; index < limit; index++) {
            View card = recipeCard(activity, recipes.get(index), repository, savedRecipeIds);
            int height = (index == 1 || index == 2) ? dp(activity, 250) : dp(activity, 218);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height
            );
            if (index > 1) {
                params.topMargin = dp(activity, 12);
            }
            if (index % 2 == 0) {
                leftColumn.addView(card, params);
            } else {
                rightColumn.addView(card, params);
            }
        }
    }

    public static void addCategoryGrid(Activity activity, LinearLayout parent, List<CommunityCategoryEntity> categories) {
        parent.removeAllViews();
        LinearLayout row = addMasonryColumns(activity, parent);
        LinearLayout leftColumn = (LinearLayout) row.getChildAt(0);
        LinearLayout rightColumn = (LinearLayout) row.getChildAt(1);

        for (int index = 0; index < categories.size(); index++) {
            CommunityCategoryEntity category = categories.get(index);
            View card = imageCard(activity, category.getImageUrl(), false);
            ((TextView) card.findViewById(R.id.cardMeta)).setText(category.getRecipeCount() + " c\u00f4ng th\u1ee9c");
            ((TextView) card.findViewById(R.id.cardTitle)).setText(category.getName());
            card.setOnClickListener(v -> openCategoryRecipes(activity, category));
            addMasonryCard(activity, index, card, leftColumn, rightColumn);
        }
    }

    public static void addChefGrid(Activity activity, LinearLayout parent, List<CommunityChefEntity> chefs) {
        parent.removeAllViews();
        LinearLayout row = addMasonryColumns(activity, parent);
        LinearLayout leftColumn = (LinearLayout) row.getChildAt(0);
        LinearLayout rightColumn = (LinearLayout) row.getChildAt(1);

        for (int index = 0; index < chefs.size(); index++) {
            CommunityChefEntity chef = chefs.get(index);
            View card = imageCard(activity, chef.getImageUrl(), false);
            TextView title = card.findViewById(R.id.cardTitle);
            TextView subtitle = card.findViewById(R.id.cardSubtitle);
            title.setText(chef.getName());
            title.setGravity(Gravity.CENTER);
            subtitle.setText(chef.getRecipeCount() + " c\u00f4ng th\u1ee9c");
            subtitle.setVisibility(View.VISIBLE);
            card.setOnClickListener(v -> openChefProfile(activity, chef));
            addMasonryCard(activity, index, card, leftColumn, rightColumn);
        }
    }

    public static void addRecipeList(Activity activity, LinearLayout parent, List<CommunityRecipeEntity> recipes) {
        addRecipeList(activity, parent, recipes, null, null);
    }

    public static void addRecipeList(
            Activity activity,
            LinearLayout parent,
            List<CommunityRecipeEntity> recipes,
            @Nullable CommunityRepository repository,
            @Nullable Set<String> savedRecipeIds
    ) {
        parent.removeAllViews();
        for (CommunityRecipeEntity recipe : recipes) {
            View card = recipeCard(activity, recipe, repository, savedRecipeIds);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(activity, 142)
            );
            params.topMargin = dp(activity, 10);
            parent.addView(card, params);
        }
    }

    public static void addRecipeMasonry(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityRecipeEntity> recipes
    ) {
        addRecipeMasonry(activity, leftColumn, rightColumn, recipes, null, null);
    }

    public static void addRecipeMasonry(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityRecipeEntity> recipes,
            @Nullable CommunityRepository repository,
            @Nullable Set<String> savedRecipeIds
    ) {
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        for (int index = 0; index < recipes.size(); index++) {
            View card = recipeCard(activity, recipes.get(index), repository, savedRecipeIds);
            addMasonryCard(activity, index, card, leftColumn, rightColumn);
        }
    }

    public static void addGalleryMasonry(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityRecipeEntity> recipes
    ) {
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        for (int index = 0; index < recipes.size(); index++) {
            View card = imageCard(activity, recipes.get(index).getImageUrl(), false);
            card.findViewById(R.id.cardTextBlock).setVisibility(View.GONE);
            addMasonryCard(activity, index, card, leftColumn, rightColumn);
        }
    }

    public static void addCookbookMasonry(
            Activity activity,
            LinearLayout leftColumn,
            LinearLayout rightColumn,
            List<CommunityCookbookEntity> cookbooks
    ) {
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        for (int index = 0; index < cookbooks.size(); index++) {
            CommunityCookbookEntity cookbook = cookbooks.get(index);
            View card = imageCard(activity, cookbook.getImageUrl(), false);
            ((TextView) card.findViewById(R.id.cardMeta)).setText("");
            ((TextView) card.findViewById(R.id.cardTitle)).setText(cookbook.getTitle());
            TextView subtitle = card.findViewById(R.id.cardSubtitle);
            subtitle.setGravity(Gravity.START);
            subtitle.setText(cookbook.getRecipeCount() + " c\u00f4ng th\u1ee9c");
            subtitle.setVisibility(View.VISIBLE);
            card.setOnClickListener(v -> openCookbook(activity, cookbook));
            addMasonryCard(activity, index, card, leftColumn, rightColumn);
        }
    }

    private static View chip(Activity activity, LinearLayout parent, int icon, String label, String emoji, View.OnClickListener listener) {
        if (icon != 0) {
            View chip = LayoutInflater.from(activity).inflate(R.layout.item_community_chip_icon, parent, false);
            ImageView iconView = chip.findViewById(R.id.chipIcon);
            iconView.setImageResource(icon);
            chip.setOnClickListener(listener);
            return chip;
        }

        View chip = LayoutInflater.from(activity).inflate(R.layout.item_community_chip, parent, false);
        TextView emojiView = chip.findViewById(R.id.chipEmoji);
        TextView titleView = chip.findViewById(R.id.chipTitle);
        emojiView.setText(emoji);
        titleView.setText(label);
        chip.setOnClickListener(listener);
        return chip;
    }

    private static void bindChefPreview(Activity activity, View item, CommunityChefEntity chef) {
        ImageView image = item.findViewById(R.id.chefImage);
        TextView name = item.findViewById(R.id.chefName);
        TextView count = item.findViewById(R.id.chefCount);
        Glide.with(activity).load(chef.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(activity, 12))).into(image);
        name.setText(chef.getName());
        count.setText(chef.getRecipeCount() + " c\u00f4ng th\u1ee9c");
    }

    private static View recipeCard(
            Activity activity,
            CommunityRecipeEntity recipe,
            @Nullable CommunityRepository repository,
            @Nullable Set<String> savedRecipeIds
    ) {
        View card = imageCard(activity, recipe.getImageUrl(), true);
        ((TextView) card.findViewById(R.id.cardMeta)).setText("\u25CF  " + recipe.getTimeMinutes() + " min");
        ((TextView) card.findViewById(R.id.cardTitle)).setText(recipe.getTitle());
        ImageView save = card.findViewById(R.id.cardSave);
        CommunityRepository repo = repository != null ? repository : new CommunityRepository(activity);
        boolean saved = savedRecipeIds != null && savedRecipeIds.contains(recipe.getId());
        renderSaveState(save, saved);
        save.setOnClickListener(v -> {
            Object tag = save.getTag();
            boolean currentlySaved = tag instanceof Boolean && (Boolean) tag;
            if (currentlySaved) {
                repo.removeRecipeFromCookbooks(recipe.getId(), done -> activity.runOnUiThread(() -> {
                    Toast.makeText(activity, done ? "Da bo luu" : "Chua bo luu duoc", Toast.LENGTH_SHORT).show();
                    if (done) {
                        if (savedRecipeIds != null) {
                            savedRecipeIds.remove(recipe.getId());
                        }
                        renderSaveState(save, false);
                    }
                }));
                return;
            }
            showAddToCookbook(activity, recipe.getId(), () -> {
                if (savedRecipeIds != null) {
                    savedRecipeIds.add(recipe.getId());
                }
                renderSaveState(save, true);
            });
        });
        card.setOnClickListener(v -> openRecipeDetail(activity, recipe));
        return card;
    }

    private static void renderSaveState(ImageView save, boolean saved) {
        save.setTag(saved);
        save.setBackgroundResource(saved ? R.drawable.bg_follow_button_green : R.drawable.bg_community_save);
        save.setColorFilter(Color.WHITE);
    }

    private static View imageCard(Activity activity, String imageUrl, boolean showSave) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_community_image_card, null, false);
        FrameLayout root = card.findViewById(R.id.imageCardRoot);
        ImageView image = card.findViewById(R.id.cardImage);
        ImageView save = card.findViewById(R.id.cardSave);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setClipToOutline(true);
        save.setVisibility(showSave ? View.VISIBLE : View.GONE);
        Glide.with(activity).load(imageUrl).transform(new CenterCrop(), new RoundedCorners(dp(activity, 14))).into(image);
        return card;
    }

    public static void showAddToCookbook(Activity activity, String recipeId) {
        showAddToCookbook(activity, recipeId, null);
    }

    public static void showAddToCookbook(Activity activity, String recipeId, SaveCallback callback) {
        Dialog dialog = createDialog(activity, R.layout.dialog_add_to_cookbook);
        LinearLayout optionList = dialog.findViewById(R.id.cookbookOptionList);
        final String[] selectedCookbookId = {null};
        CommunityRepository repository = new CommunityRepository(activity);
        repository.loadCookbooksForSave(null, cookbooks -> activity.runOnUiThread(() -> {
            optionList.removeAllViews();
            if (cookbooks.isEmpty()) {
                TextView empty = new TextView(activity);
                empty.setText("Bạn chưa có cookbook. Bấm Tạo mới để tạo cookbook đầu tiên.");
                empty.setTextColor(Color.DKGRAY);
                empty.setTextSize(13);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(activity, 12), dp(activity, 14), dp(activity, 12), dp(activity, 14));
                empty.setBackgroundResource(R.drawable.bg_community_chip);
                optionList.addView(empty, optionParams(activity, 0));
                return;
            }
            int limit = Math.min(cookbooks.size(), 4);
            for (int index = 0; index < limit; index++) {
                CommunityCookbookEntity cookbook = cookbooks.get(index);
                View option = LayoutInflater.from(activity).inflate(R.layout.item_dialog_cookbook_option, optionList, false);
                ImageView image = option.findViewById(R.id.cookbookOptionImage);
                TextView title = option.findViewById(R.id.cookbookOptionTitle);
                TextView count = option.findViewById(R.id.cookbookOptionCount);
                title.setText(cookbook.getTitle());
                count.setText(cookbook.getRecipeCount() + " c\u00f4ng th\u1ee9c");
                Glide.with(activity).load(cookbook.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(activity, 8))).into(image);
                option.setOnClickListener(v -> {
                    selectedCookbookId[0] = cookbook.getId();
                    for (int child = 0; child < optionList.getChildCount(); child++) {
                        renderCookbookOption(optionList.getChildAt(child), false);
                    }
                    renderCookbookOption(option, true);
                });
                optionList.addView(option, optionParams(activity, index));
                if (index == 0) {
                    selectedCookbookId[0] = cookbook.getId();
                    renderCookbookOption(option, true);
                }
            }
        }));
        dialog.findViewById(R.id.cookbookCreateButton).setOnClickListener(v -> {
            dialog.dismiss();
            showCreateCookbook(activity, recipeId, callback);
        });
        dialog.findViewById(R.id.cookbookSaveButton).setOnClickListener(v -> {
            if (selectedCookbookId[0] == null) {
                Toast.makeText(activity, "Chọn cookbook hoặc bấm Tạo mới", Toast.LENGTH_SHORT).show();
                return;
            }
            repository.addRecipeToCookbook(selectedCookbookId[0], recipeId, (done, errorDetail) -> activity.runOnUiThread(() -> {
                Toast.makeText(activity, done ? "Đã lưu vào cookbook" : cookbookErrorMessage("Chưa lưu được vào cookbook", errorDetail), Toast.LENGTH_LONG).show();
                if (!done) {
                    return;
                }
                if (callback != null) {
                    callback.onSaved();
                }
                dialog.dismiss();
            }));
        });
        dialog.show();
        sizeDialog(activity, dialog);
    }

    private static void showCreateCookbook(Activity activity, String recipeId, SaveCallback callback) {
        Dialog dialog = createDialog(activity, R.layout.dialog_create_cookbook);
        CommunityRepository repository = new CommunityRepository(activity);
        dialog.findViewById(R.id.cookbookBackToListButton).setOnClickListener(v -> {
            dialog.dismiss();
            showAddToCookbook(activity, recipeId, callback);
        });
        dialog.findViewById(R.id.cookbookCreateSaveButton).setOnClickListener(v -> {
            EditText titleInput = dialog.findViewById(R.id.cookbookTitleInput);
            EditText descriptionInput = dialog.findViewById(R.id.cookbookDescriptionInput);
            String title = titleInput.getText().toString().trim();
            if (title.isEmpty()) {
                titleInput.setError("Nh\u1eadp t\u00ean cookbook");
                return;
            }
            repository.createCookbook(title, descriptionInput.getText().toString().trim(), recipeId, (done, errorDetail) -> activity.runOnUiThread(() -> {
                Toast.makeText(activity, done ? "Đã tạo cookbook" : cookbookErrorMessage("Chưa tạo được cookbook", errorDetail), Toast.LENGTH_LONG).show();
                if (!done) {
                    return;
                }
                if (recipeId != null && !recipeId.trim().isEmpty() && callback != null) {
                    callback.onSaved();
                }
                dialog.dismiss();
                if (recipeId != null && !recipeId.trim().isEmpty()) {
                    showAddToCookbook(activity, recipeId, callback);
                }
            }));
        });
        dialog.show();
        sizeDialog(activity, dialog);
    }

    private static Dialog createDialog(Activity activity, int layoutRes) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layoutRes);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
            window.setGravity(Gravity.BOTTOM);
        }
        return dialog;
    }

    private static String cookbookErrorMessage(String fallback, String errorDetail) {
        if (errorDetail == null || errorDetail.trim().isEmpty()) {
            return fallback;
        }
        return fallback + "\n" + errorDetail.trim();
    }

    private static void sizeDialog(Activity activity, Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 56), ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        }
    }

    private static LinearLayout.LayoutParams optionParams(Activity activity, int index) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 58)
        );
        if (index > 0) {
            params.topMargin = dp(activity, 10);
        }
        return params;
    }

    private static void renderCookbookOption(View option, boolean selected) {
        option.setBackgroundResource(selected ? R.drawable.bg_button_primary_large : R.drawable.bg_community_chip);
        ((TextView) option.findViewById(R.id.cookbookOptionTitle)).setTextColor(selected ? Color.WHITE : Color.BLACK);
        ((TextView) option.findViewById(R.id.cookbookOptionCount)).setTextColor(selected ? Color.WHITE : Color.DKGRAY);
    }

    private static LinearLayout addMasonryColumns(Activity activity, LinearLayout parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(activity, 10);
        parent.addView(row, rowParams);

        LinearLayout leftColumn = new LinearLayout(activity);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout rightColumn = new LinearLayout(activity);
        rightColumn.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        rightParams.leftMargin = dp(activity, 12);
        row.addView(leftColumn, leftParams);
        row.addView(rightColumn, rightParams);
        return row;
    }

    private static void addMasonryCard(
            Activity activity,
            int index,
            View card,
            LinearLayout leftColumn,
            LinearLayout rightColumn
    ) {
        int height = (index == 1 || index == 2 || index % 5 == 4) ? dp(activity, 250) : dp(activity, 218);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        if (index > 1) {
            params.topMargin = dp(activity, 12);
        }
        if (index % 2 == 0) {
            leftColumn.addView(card, params);
        } else {
            rightColumn.addView(card, params);
        }
    }

    private static String categoryEmoji(String label) {
        String lower = label.toLowerCase();
        if (lower.contains("soup") || lower.contains("súp")) return "\uD83C\uDF72";
        if (lower.contains("seafood") || lower.contains("hải sản")) return "\uD83E\uDD90";
        if (lower.contains("sushi")) return "\uD83C\uDF63";
        if (lower.contains("cake") || lower.contains("dessert") || lower.contains("bánh") || lower.contains("tráng miệng")) return "\uD83C\uDF70";
        if (lower.contains("breakfast") || lower.contains("bữa sáng")) return "\uD83C\uDF73";
        if (lower.contains("healthy") || lower.contains("lành mạnh") || lower.contains("vegan") || lower.contains("món chay")) return "\uD83E\uDD57";
        if (lower.contains("noodles") || lower.contains("món mì")) return "\uD83C\uDF5C";
        if (lower.contains("drinks") || lower.contains("đồ uống")) return "\uD83E\uDD64";
        if (lower.contains("grill") || lower.contains("món nướng")) return "\uD83C\uDF56";
        return "\uD83C\uDF7D";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void openCategoryRecipes(Activity activity, CommunityCategoryEntity category) {
        Intent intent = new Intent(activity, CommunityRecipesActivity.class);
        intent.putExtra(CommunityRecipesActivity.EXTRA_CATEGORY_ID, category.getId());
        intent.putExtra(CommunityRecipesActivity.EXTRA_CATEGORY_NAME, category.getName());
        activity.startActivity(intent);
    }

    private static void openChefProfile(Activity activity, CommunityChefEntity chef) {
        Intent intent = new Intent(activity, CommunityProfileActivity.class);
        CommunityRepository repository = new CommunityRepository(activity);
        if (chef.getId().equals(repository.currentCustomerId())) {
            intent.putExtra(CommunityProfileActivity.EXTRA_ACCOUNT_PROFILE, true);
            activity.startActivity(intent);
            return;
        }
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_ID, chef.getId());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_NAME, chef.getName());
        intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_IMAGE_URL, chef.getImageUrl());
        intent.putExtra(CommunityProfileActivity.EXTRA_RECIPE_COUNT, chef.getRecipeCount());
        intent.putExtra(CommunityProfileActivity.EXTRA_LIKES, chef.getLikes());
        activity.startActivity(intent);
    }

    private static void openRecipeDetail(Activity activity, CommunityRecipeEntity recipe) {
        Intent intent = new Intent(activity, CommunityRecipeDetailActivity.class);
        intent.putExtra(CommunityRecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        activity.startActivity(intent);
    }

    private static void openCookbook(Activity activity, CommunityCookbookEntity cookbook) {
        Intent intent = new Intent(activity, CommunityCookbookActivity.class);
        intent.putExtra(CommunityCookbookActivity.EXTRA_COOKBOOK_ID, cookbook.getId());
        activity.startActivity(intent);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
