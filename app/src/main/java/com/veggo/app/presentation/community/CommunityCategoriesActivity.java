package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;
import com.veggo.app.databinding.ComponentBottomNavBinding;

public class CommunityCategoriesActivity extends AppCompatActivity {
    private LinearLayout container;
    private CommunityRepository repository;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_categories);
        container = findViewById(R.id.communityHomeContainer);
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(this, ComponentBottomNavBinding.bind(findViewById(R.id.communityBottomNavHost)));
        CommunityUi.setupTopHeader(this, "Danh mục");
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.communityListScroll, this::loadCategories);
        loadCategories();
    }

    private void loadCategories() {
        repository.loadCategories(categories -> runOnUiThread(() -> {
            CommunityUi.addCategoryGrid(this, container, CommunityUi.shuffled(categories));
            CommunityUi.finishRefresh(refreshLayout);
        }));
    }
}
