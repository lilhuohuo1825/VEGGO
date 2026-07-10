package com.veggo.app.presentation.community;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.veggo.app.databinding.ActivityCommunityHomeBinding;
import com.veggo.app.R;

public class CommunityHomeActivity extends AppCompatActivity {
    private ActivityCommunityHomeBinding binding;
    private CommunityRepository repository;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(this, binding.communityBottomNavHost);
        binding.communityAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileActivity.class);
            intent.putExtra(CommunityProfileActivity.EXTRA_ACCOUNT_PROFILE, true);
            startActivity(intent);
        });
        binding.communitySearchButton.setOnClickListener(v -> startActivity(new Intent(this, CommunityDiscoveryActivity.class)));
        binding.communityAddButton.setOnClickListener(v -> startActivity(new Intent(this, CommunityPostActivity.class)));
        binding.topChefSeeMore.setOnClickListener(v -> startActivity(new Intent(this, CommunityChefsActivity.class)));
        binding.recipesSeeMore.setOnClickListener(v -> startActivity(new Intent(this, CommunityRecipesActivity.class)));
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.communityHomeScroll, this::loadHome);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindAccountAvatar();
        loadHome();
    }

    private void loadHome() {
        repository.loadHome(data -> repository.loadSavedRecipeIds(savedIds -> runOnUiThread(() -> {
            CommunityUi.addCategoryChips(this, binding.communityChipRow, CommunityUi.shuffled(data.categories));
            CommunityUi.addChefPreview(this, binding.chefPreviewRow, CommunityUi.shuffled(data.chefs));
            CommunityUi.addRecipePreview(
                    this,
                    binding.recipeLeftColumn,
                    binding.recipeRightColumn,
                    CommunityUi.shuffled(data.recipes),
                    repository,
                    savedIds
            );
            CommunityUi.finishRefresh(refreshLayout);
        })));
    }

    private void bindAccountAvatar() {
        binding.communityAvatar.setImageResource(R.drawable.ic_user_full);
        repository.loadUser(repository.currentCustomerId(), user -> runOnUiThread(() -> {
            if (user == null) {
                return;
            }
            bindAvatarUrl(user.getImageUrl());
        }));
    }

    private void bindAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            binding.communityAvatar.setImageResource(R.drawable.ic_user_full);
            return;
        }
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_full)
                .error(R.drawable.ic_user_full)
                .transform(new CircleCrop())
                .into(binding.communityAvatar);
    }
}
