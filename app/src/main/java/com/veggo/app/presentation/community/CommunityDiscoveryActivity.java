package com.veggo.app.presentation.community;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;
import com.veggo.app.databinding.ComponentBottomNavBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityDiscoveryActivity extends AppCompatActivity {
    private static final int TAB_RECIPES = 0;
    private static final int TAB_CHEFS = 1;

    private CommunityRepository repository;
    private LinearLayout container;
    private EditText searchInput;
    private TextView recipesText;
    private TextView chefsText;
    private View recipesIndicator;
    private View chefsIndicator;
    private int activeTab = TAB_RECIPES;
    private final List<CommunityRecipeEntity> recipes = new ArrayList<>();
    private final List<CommunityChefEntity> chefs = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_discovery);

        repository = new CommunityRepository(this);
        container = findViewById(R.id.discoveryContainer);
        searchInput = findViewById(R.id.discoverySearchInput);
        recipesText = findViewById(R.id.discoveryRecipesText);
        chefsText = findViewById(R.id.discoveryChefsText);
        recipesIndicator = findViewById(R.id.discoveryRecipesIndicator);
        chefsIndicator = findViewById(R.id.discoveryChefsIndicator);
        CommunityUi.setupBottomNav(this, ComponentBottomNavBinding.bind(findViewById(R.id.communityBottomNavHost)));

        findViewById(R.id.discoveryBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.discoveryRecipesTab).setOnClickListener(v -> showRecipes());
        findViewById(R.id.discoveryChefsTab).setOnClickListener(v -> showChefs());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderActiveTab();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        repository.loadRecipes(result -> runOnUiThread(() -> {
            recipes.clear();
            recipes.addAll(result);
            renderActiveTab();
        }));
        repository.loadChefs(result -> runOnUiThread(() -> {
            chefs.clear();
            chefs.addAll(result);
            renderActiveTab();
        }));
    }

    private void showRecipes() {
        activeTab = TAB_RECIPES;
        renderActiveTab();
    }

    private void showChefs() {
        activeTab = TAB_CHEFS;
        renderActiveTab();
    }

    private void renderActiveTab() {
        if (activeTab == TAB_RECIPES) {
            bindTabs(true);
            renderRecipes();
        } else {
            bindTabs(false);
            renderChefs();
        }
    }

    private void renderRecipes() {
        LinearLayout row = masonryRow();
        CommunityUi.addRecipeMasonry(this, (LinearLayout) row.getChildAt(0), (LinearLayout) row.getChildAt(1), filteredRecipes());
    }

    private void renderChefs() {
        CommunityUi.addChefGrid(this, container, filteredChefs());
    }

    private List<CommunityRecipeEntity> filteredRecipes() {
        String query = query();
        if (query.isEmpty()) {
            return recipes;
        }
        List<CommunityRecipeEntity> filtered = new ArrayList<>();
        for (CommunityRecipeEntity recipe : recipes) {
            if (recipe.getTitle() != null && recipe.getTitle().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(recipe);
            }
        }
        return filtered;
    }

    private List<CommunityChefEntity> filteredChefs() {
        String query = query();
        if (query.isEmpty()) {
            return chefs;
        }
        List<CommunityChefEntity> filtered = new ArrayList<>();
        for (CommunityChefEntity chef : chefs) {
            if (chef.getName() != null && chef.getName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(chef);
            }
        }
        return filtered;
    }

    private String query() {
        return searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private LinearLayout masonryRow() {
        container.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout leftColumn = new LinearLayout(this);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout rightColumn = new LinearLayout(this);
        rightColumn.setOrientation(LinearLayout.VERTICAL);

        row.addView(leftColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        rightParams.leftMargin = dp(12);
        row.addView(rightColumn, rightParams);
        return row;
    }

    private void bindTabs(boolean recipesActive) {
        int green = ContextCompat.getColor(this, R.color.primary_main);
        int muted = ContextCompat.getColor(this, R.color.neutral_60);
        recipesText.setTextColor(recipesActive ? green : muted);
        recipesText.setTypeface(null, recipesActive ? Typeface.BOLD : Typeface.NORMAL);
        chefsText.setTextColor(recipesActive ? muted : green);
        chefsText.setTypeface(null, recipesActive ? Typeface.NORMAL : Typeface.BOLD);
        recipesIndicator.setVisibility(recipesActive ? View.VISIBLE : View.INVISIBLE);
        chefsIndicator.setVisibility(recipesActive ? View.INVISIBLE : View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
