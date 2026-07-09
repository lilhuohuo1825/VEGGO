package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;

import java.util.HashSet;
import java.util.Set;

public class CommunityCookbookActivity extends AppCompatActivity {
    public static final String EXTRA_COOKBOOK_ID = "community_cookbook_id";

    private CommunityRepository repository;
    private LinearLayout leftColumn;
    private LinearLayout rightColumn;
    private SwipeRefreshLayout refreshLayout;
    private String cookbookId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_cookbook);

        repository = new CommunityRepository(this);
        leftColumn = findViewById(R.id.cookbookLeftColumn);
        rightColumn = findViewById(R.id.cookbookRightColumn);
        findViewById(R.id.cookbookBackButton).setOnClickListener(v -> finish());

        cookbookId = getIntent().getStringExtra(EXTRA_COOKBOOK_ID);
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.cookbookScroll, this::loadCookbook);
        loadCookbook();
    }

    private void loadCookbook() {
        repository.loadCookbookRecipes(cookbookId, data -> runOnUiThread(() -> {
            if (data.cookbook != null) {
                ((TextView) findViewById(R.id.cookbookTitle)).setText(data.cookbook.getTitle());
                ((TextView) findViewById(R.id.cookbookCount)).setText(data.recipes.size() + " công thức");
            }
            Set<String> savedIds = new HashSet<>();
            for (com.veggo.app.data.local.entity.CommunityRecipeEntity recipe : data.recipes) {
                if (recipe != null && recipe.getId() != null) {
                    savedIds.add(recipe.getId());
                }
            }
            CommunityUi.addRecipeMasonry(
                    this,
                    leftColumn,
                    rightColumn,
                    CommunityUi.shuffled(data.recipes),
                    repository,
                    savedIds
            );
            CommunityUi.finishRefresh(refreshLayout);
        }));
    }
}
