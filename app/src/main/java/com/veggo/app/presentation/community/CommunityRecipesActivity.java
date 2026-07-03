package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;
import com.veggo.app.databinding.ComponentBottomNavBinding;

public class CommunityRecipesActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "community_category_id";
    public static final String EXTRA_CATEGORY_NAME = "community_category_name";
    public static final String EXTRA_CHEF_ID = "community_chef_id";
    public static final String EXTRA_CHEF_NAME = "community_chef_name";

    private LinearLayout container;
    private CommunityRepository repository;
    private SwipeRefreshLayout refreshLayout;
    private String categoryId;
    private String chefId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipes);
        container = findViewById(R.id.communityHomeContainer);
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(this, ComponentBottomNavBinding.bind(findViewById(R.id.communityBottomNavHost)));
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        chefId = getIntent().getStringExtra(EXTRA_CHEF_ID);
        String chefName = getIntent().getStringExtra(EXTRA_CHEF_NAME);
        String title = categoryName == null || categoryName.isEmpty()
                ? "Công thức gợi ý hôm nay"
                : categoryName;
        if (chefName != null && !chefName.isEmpty()) {
            title = chefName;
        }
        CommunityUi.setupTopHeader(this, title);
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.communityListScroll, this::loadRecipes);
        loadRecipes();
    }

    private void loadRecipes() {
        if (categoryId != null && !categoryId.isEmpty()) {
            repository.loadRecipesByCategory(categoryId, recipes -> runOnUiThread(() -> renderRecipes(recipes)));
        } else if (chefId != null && !chefId.isEmpty()) {
            repository.loadRecipesByChef(chefId, recipes -> runOnUiThread(() -> renderRecipes(recipes)));
        } else {
            repository.loadRecipes(recipes -> runOnUiThread(() -> renderRecipes(recipes)));
        }
    }

    private void renderRecipes(java.util.List<com.veggo.app.data.local.entity.CommunityRecipeEntity> recipes) {
        CommunityUi.addRecipeList(this, container, CommunityUi.shuffled(recipes));
        CommunityUi.finishRefresh(refreshLayout);
    }
}
