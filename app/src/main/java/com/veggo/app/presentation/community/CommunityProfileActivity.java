package com.veggo.app.presentation.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityCookbookEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;

import java.util.ArrayList;
import java.util.List;

public class CommunityProfileActivity extends AppCompatActivity {
    public static final String EXTRA_CHEF_ID = "community_profile_chef_id";
    public static final String EXTRA_CHEF_NAME = "community_profile_chef_name";
    public static final String EXTRA_CHEF_IMAGE_URL = "community_profile_chef_image_url";
    public static final String EXTRA_RECIPE_COUNT = "community_profile_recipe_count";
    public static final String EXTRA_LIKES = "community_profile_likes";
    public static final String EXTRA_ACCOUNT_PROFILE = "community_profile_account";

    private CommunityRepository repository;
    private LinearLayout leftColumn;
    private LinearLayout rightColumn;
    private TextView recipesTab;
    private TextView galleriesTab;
    private boolean accountProfile;
    private boolean savedSelected;
    private final List<CommunityRecipeEntity> chefRecipes = new ArrayList<>();
    private final List<CommunityCookbookEntity> cookbooks = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_profile);

        repository = new CommunityRepository(this);
        leftColumn = findViewById(R.id.profileLeftColumn);
        rightColumn = findViewById(R.id.profileRightColumn);
        recipesTab = findViewById(R.id.profileRecipesTab);
        galleriesTab = findViewById(R.id.profileGalleriesTab);

        accountProfile = getIntent().getBooleanExtra(EXTRA_ACCOUNT_PROFILE, false);
        String chefId = accountProfile ? CommunityRepository.ACCOUNT_ID : getIntent().getStringExtra(EXTRA_CHEF_ID);
        String chefName = accountProfile ? CommunityRepository.ACCOUNT_NAME : getIntent().getStringExtra(EXTRA_CHEF_NAME);
        String chefImageUrl = accountProfile ? CommunityRepository.ACCOUNT_AVATAR_URL : getIntent().getStringExtra(EXTRA_CHEF_IMAGE_URL);
        int recipeCount = getIntent().getIntExtra(EXTRA_RECIPE_COUNT, 0);
        int likes = getIntent().getIntExtra(EXTRA_LIKES, 0);

        bindHeader(chefName, chefImageUrl, recipeCount, likes);
        bindFollowNavigation(chefId);
        bindTabs();

        repository.loadFollowCounts(chefId, counts -> runOnUiThread(() -> {
            ((TextView) findViewById(R.id.profileFollowingCount)).setText(String.valueOf(counts.following));
            ((TextView) findViewById(R.id.profileFollowerCount)).setText(String.valueOf(counts.followers));
        }));

        CommunityRepository.Callback<List<CommunityRecipeEntity>> recipeCallback = recipes -> runOnUiThread(() -> {
            chefRecipes.clear();
            chefRecipes.addAll(recipes);
            ((TextView) findViewById(R.id.profileRecipeCount)).setText(String.valueOf(recipes.size()));
            if (!recipes.isEmpty()) {
                ImageView hero = findViewById(R.id.profileHeroImage);
                Glide.with(this)
                        .load(recipes.get(0).getImageUrl())
                        .transform(new CenterCrop(), bottomRoundedCorners(18))
                        .into(hero);
            }
            showRecipes();
        });
        if (accountProfile) {
            repository.loadAccountRecipes(recipeCallback);
            repository.loadCookbooks(CommunityRepository.ACCOUNT_ID, items -> runOnUiThread(() -> {
                cookbooks.clear();
                cookbooks.addAll(items);
                if (savedSelected) {
                    showCookbooks();
                }
            }));
        } else {
            repository.loadRecipesByChef(chefId, recipeCallback);
        }
    }

    private void bindHeader(String chefName, String chefImageUrl, int recipeCount, int likes) {
        View back = findViewById(R.id.profileBackButton);
        back.setOnClickListener(v -> finish());
        View followButton = findViewById(R.id.profileFollowButton);
        if (accountProfile) {
            followButton.setVisibility(View.GONE);
            findViewById(R.id.profileShareButton).setVisibility(View.GONE);
            findViewById(R.id.profileAccountActions).setVisibility(View.VISIBLE);
            findViewById(R.id.profileAddButton).setOnClickListener(v ->
                    startActivity(new Intent(this, CommunityPostActivity.class)));
        } else {
            ToggleUi.bindToggle(followButton, R.drawable.ic_userplus, R.drawable.ic_usercheck, false);
            findViewById(R.id.profileShareButton).setVisibility(View.VISIBLE);
            findViewById(R.id.profileAccountActions).setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.profileName)).setText(chefName == null ? "" : chefName);
        ((TextView) findViewById(R.id.profileLocation)).setText(accountProfile ? CommunityRepository.ACCOUNT_LOCATION : "Florida, US");
        ((TextView) findViewById(R.id.profileRecipeCount)).setText(String.valueOf(recipeCount));
        ((TextView) findViewById(R.id.profileFollowingCount)).setText("0");
        ((TextView) findViewById(R.id.profileFollowerCount)).setText("0");

        ImageView avatar = findViewById(R.id.profileAvatar);
        ImageView hero = findViewById(R.id.profileHeroImage);
        Glide.with(this)
                .load(chefImageUrl)
                .transform(new CenterCrop(), new RoundedCorners(dp(12)))
                .into(avatar);
        Glide.with(this)
                .load(accountProfile ? CommunityRepository.ACCOUNT_HERO_URL : chefImageUrl)
                .transform(new CenterCrop(), bottomRoundedCorners(18))
                .into(hero);
    }

    private GranularRoundedCorners bottomRoundedCorners(int radiusDp) {
        float radius = dp(radiusDp);
        return new GranularRoundedCorners(0f, 0f, radius, radius);
    }

    private void bindFollowNavigation(String chefId) {
        findViewById(R.id.profileFollowingStat).setOnClickListener(v ->
                openFollowList(chefId, CommunityRepository.RELATION_FOLLOWING));
        findViewById(R.id.profileFollowerStat).setOnClickListener(v ->
                openFollowList(chefId, CommunityRepository.RELATION_FOLLOWER));
    }

    private void openFollowList(String chefId, String relationType) {
        Intent intent = new Intent(this, CommunityFollowListActivity.class);
        intent.putExtra(CommunityFollowListActivity.EXTRA_CHEF_ID, chefId);
        intent.putExtra(CommunityFollowListActivity.EXTRA_RELATION_TYPE, relationType);
        startActivity(intent);
    }

    private void bindTabs() {
        if (accountProfile) {
            recipesTab.setText("Posts");
            galleriesTab.setText("Saved");
        }
        recipesTab.setOnClickListener(v -> showRecipes());
        galleriesTab.setOnClickListener(v -> {
            if (accountProfile) {
                showCookbooks();
            } else {
                showGalleries();
            }
        });
    }

    private void showRecipes() {
        savedSelected = false;
        recipesTab.setTextColor(ContextCompat.getColor(this, R.color.primary_main));
        recipesTab.setTypeface(null, android.graphics.Typeface.BOLD);
        galleriesTab.setTextColor(ContextCompat.getColor(this, R.color.neutral_60));
        galleriesTab.setTypeface(null, android.graphics.Typeface.NORMAL);
        CommunityUi.addRecipeMasonry(this, leftColumn, rightColumn, chefRecipes);
    }

    private void showGalleries() {
        savedSelected = false;
        galleriesTab.setTextColor(ContextCompat.getColor(this, R.color.primary_main));
        galleriesTab.setTypeface(null, android.graphics.Typeface.BOLD);
        recipesTab.setTextColor(ContextCompat.getColor(this, R.color.neutral_60));
        recipesTab.setTypeface(null, android.graphics.Typeface.NORMAL);
        CommunityUi.addGalleryMasonry(this, leftColumn, rightColumn, chefRecipes);
    }

    private void showCookbooks() {
        savedSelected = true;
        galleriesTab.setTextColor(ContextCompat.getColor(this, R.color.primary_main));
        galleriesTab.setTypeface(null, android.graphics.Typeface.BOLD);
        recipesTab.setTextColor(ContextCompat.getColor(this, R.color.neutral_60));
        recipesTab.setTypeface(null, android.graphics.Typeface.NORMAL);
        CommunityUi.addCookbookMasonry(this, leftColumn, rightColumn, cookbooks);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
