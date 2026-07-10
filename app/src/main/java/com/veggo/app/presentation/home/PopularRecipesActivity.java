package com.veggo.app.presentation.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.R;
import com.veggo.app.adapter.PopularRecipeAdapter;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.utils.JsonUtils;
import com.veggo.app.core.utils.KeyboardUtils;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.presentation.community.InstructionRecipeDetailActivity;
import com.veggo.app.presentation.profile.FridgeQuickScanHelper;
import com.veggo.app.speech.SearchVoiceInputController;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PopularRecipesActivity extends BaseActivity {

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private ProgressBar progressBar;
    private LinearLayout titleHeader;
    private LinearLayout searchHeader;
    private EditText searchInput;
    private PopularRecipeAdapter recipeAdapter;
    private SearchVoiceInputController voiceInputController;

    private final List<Recipe> allRecipes = new ArrayList<>();
    private boolean searchMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_recipes);

        titleHeader = findViewById(R.id.popularRecipesTitleHeader);
        searchHeader = findViewById(R.id.popularRecipesSearchHeader);
        searchInput = findViewById(R.id.edtSearch);
        refreshLayout = findViewById(R.id.popularRecipesRefresh);
        recyclerView = findViewById(R.id.rvPopularRecipes);
        emptyState = findViewById(R.id.popularRecipesEmptyState);
        emptyTitle = findViewById(R.id.tvPopularRecipesEmpty);
        progressBar = findViewById(R.id.progressPopularRecipes);

        findViewById(R.id.popularRecipesBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.popularRecipesSearchButton).setOnClickListener(v -> showSearchMode());
        findViewById(R.id.popularRecipesSearchBackButton).setOnClickListener(v -> hideSearchMode());

        float density = getResources().getDisplayMetrics().density;
        int cardHeightPx = (int) (142 * density);
        int horizontalMarginPx = (int) (18 * density);
        int bottomMarginPx = (int) (10 * density);
        int cornerRadiusPx = (int) (14 * density);

        recipeAdapter = new PopularRecipeAdapter(
                cardHeightPx, horizontalMarginPx, bottomMarginPx, cornerRadiusPx);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recipeAdapter);
        recipeAdapter.setOnRecipeClickListener(recipe -> {
            Intent intent = new Intent(this, InstructionRecipeDetailActivity.class);
            intent.putExtra(InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID, recipe.getId());
            startActivity(intent);
        });

        View searchBarRoot = findViewById(R.id.layoutSearch);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applySearchFilter();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (searchBarRoot != null && searchInput != null) {
            voiceInputController = SearchVoiceInputController.attach(
                    this,
                    searchBarRoot,
                    searchInput,
                    this::applySearchFilter
            );
            View cameraButton = searchBarRoot.findViewById(R.id.btnCamera);
            if (cameraButton != null) {
                cameraButton.setOnClickListener(v ->
                        startActivity(FridgeQuickScanHelper.createSearchSuggestionCameraIntent(this))
                );
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchMode) {
                    hideSearchMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        PullToRefreshHelper.bind(refreshLayout, recyclerView, this::loadRecipes);
        loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (voiceInputController != null) {
            voiceInputController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (voiceInputController != null) {
            voiceInputController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (voiceInputController != null) {
            voiceInputController.release();
            voiceInputController = null;
        }
        super.onDestroy();
    }

    private void showSearchMode() {
        searchMode = true;
        titleHeader.setVisibility(View.GONE);
        searchHeader.setVisibility(View.VISIBLE);
        if (searchInput != null) {
            searchInput.requestFocus();
            searchInput.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    private void hideSearchMode() {
        searchMode = false;
        titleHeader.setVisibility(View.VISIBLE);
        searchHeader.setVisibility(View.GONE);
        if (searchInput != null) {
            searchInput.setText("");
            KeyboardUtils.hideKeyboard(searchInput);
            searchInput.clearFocus();
        }
        applySearchFilter();
    }

    private void loadRecipes() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.INVISIBLE);

        allRecipes.clear();
        allRecipes.addAll(loadRecipesFromAssets());
        progressBar.setVisibility(View.GONE);
        PullToRefreshHelper.finish(refreshLayout);
        applySearchFilter();
    }

    private void applySearchFilter() {
        String query = searchInput != null ? searchInput.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (query.isEmpty() || matchesQuery(recipe, query)) {
                filtered.add(recipe);
            }
        }
        recipeAdapter.submitList(filtered);
        updateEmptyState(filtered.isEmpty(), query);
    }

    private boolean matchesQuery(Recipe recipe, String query) {
        if (recipe.getName() != null && recipe.getName().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        return recipe.getCookingTime() != null
                && recipe.getCookingTime().toLowerCase(Locale.getDefault()).contains(query);
    }

    private void updateEmptyState(boolean showEmpty, String query) {
        if (showEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            if (emptyTitle != null) {
                emptyTitle.setText(query.isEmpty()
                        ? getString(R.string.popular_recipes_empty)
                        : getString(R.string.popular_recipes_search_empty));
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private List<Recipe> loadRecipesFromAssets() {
        String dishesJson = JsonUtils.loadJSONFromAsset(this, "dishes.json");
        String instructionsJson = JsonUtils.loadJSONFromAsset(this, "instructions.json");
        if (dishesJson == null || instructionsJson == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type dishListType = new TypeToken<List<AssetModels.Dish>>() {}.getType();
        Type instructionListType = new TypeToken<List<AssetModels.Instruction>>() {}.getType();

        List<AssetModels.Dish> dishes = gson.fromJson(dishesJson, dishListType);
        List<AssetModels.Instruction> instructions = gson.fromJson(instructionsJson, instructionListType);

        Map<String, AssetModels.Instruction> instructionMap = new HashMap<>();
        if (instructions != null) {
            for (AssetModels.Instruction inst : instructions) {
                instructionMap.put(inst.id, inst);
            }
        }

        List<Recipe> recipeList = new ArrayList<>();
        if (dishes != null) {
            for (AssetModels.Dish dish : dishes) {
                AssetModels.Instruction inst = instructionMap.get(dish.id);
                if (inst == null) {
                    continue;
                }
                String thumbnailUrl = com.veggo.app.core.utils.RecipeThumbnailUtils
                        .resolveThumbnail(dish.video);
                recipeList.add(new Recipe(
                        dish.id,
                        inst.dishName,
                        inst.cookingTime,
                        0,
                        thumbnailUrl,
                        countIngredients(dish.ingredients)
                ));
            }
        }
        return recipeList;
    }

    private int countIngredients(String ingredients) {
        if (ingredients == null || ingredients.trim().isEmpty()) {
            return 0;
        }
        String normalized = ingredients
                .replace("\r", "\n")
                .replace(";", ",")
                .replace("•", ",");
        String[] parts = normalized.split(",|\\n");
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
