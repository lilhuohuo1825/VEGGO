package com.veggo.app.presentation.community;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.databinding.ActivityCommunityHomeBinding;

public class CommunityHomeActivity extends AppCompatActivity {
    private ActivityCommunityHomeBinding binding;
    private CommunityRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(binding.communityBottomNavigation);
        binding.communityAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileActivity.class);
            intent.putExtra(CommunityProfileActivity.EXTRA_ACCOUNT_PROFILE, true);
            startActivity(intent);
        });
        binding.communitySearchButton.setOnClickListener(v -> startActivity(new Intent(this, CommunityDiscoveryActivity.class)));
        binding.communityAddButton.setOnClickListener(v -> startActivity(new Intent(this, CommunityPostActivity.class)));
        binding.topChefSeeMore.setOnClickListener(v -> startActivity(new Intent(this, CommunityChefsActivity.class)));
        binding.recipesSeeMore.setOnClickListener(v -> startActivity(new Intent(this, CommunityRecipesActivity.class)));
        repository.loadHome(data -> runOnUiThread(() -> {
            CommunityUi.addCategoryChips(this, binding.communityChipRow, data.categories);
            CommunityUi.addChefPreview(this, binding.chefPreviewRow, data.chefs);
            CommunityUi.addRecipePreview(this, binding.recipeLeftColumn, binding.recipeRightColumn, data.recipes);
        }));
    }
}
