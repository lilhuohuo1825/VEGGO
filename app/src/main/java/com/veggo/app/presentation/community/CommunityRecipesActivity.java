package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.veggo.app.R;

public class CommunityRecipesActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "community_category_id";
    public static final String EXTRA_CATEGORY_NAME = "community_category_name";
    public static final String EXTRA_CHEF_ID = "community_chef_id";
    public static final String EXTRA_CHEF_NAME = "community_chef_name";

    private LinearLayout container;
    private BottomNavigationView bottomNavigationView;
    private CommunityRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipes);
        container = findViewById(R.id.communityHomeContainer);
        bottomNavigationView = findViewById(R.id.communityBottomNavigation);
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(bottomNavigationView);
        String categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String chefId = getIntent().getStringExtra(EXTRA_CHEF_ID);
        String chefName = getIntent().getStringExtra(EXTRA_CHEF_NAME);
        String title = categoryName == null || categoryName.isEmpty()
                ? "C\u00f4ng th\u1ee9c g\u1ee3i \u00fd h\u00f4m nay"
                : categoryName;
        if (chefName != null && !chefName.isEmpty()) {
            title = chefName;
        }
        CommunityUi.setupTopHeader(this, title);
        if (categoryId != null && !categoryId.isEmpty()) {
            repository.loadRecipesByCategory(categoryId, recipes -> runOnUiThread(() -> CommunityUi.addRecipeList(this, container, recipes)));
        } else if (chefId != null && !chefId.isEmpty()) {
            repository.loadRecipesByChef(chefId, recipes -> runOnUiThread(() -> CommunityUi.addRecipeList(this, container, recipes)));
        } else {
            repository.loadRecipes(recipes -> runOnUiThread(() -> CommunityUi.addRecipeList(this, container, recipes)));
        }
    }
}
