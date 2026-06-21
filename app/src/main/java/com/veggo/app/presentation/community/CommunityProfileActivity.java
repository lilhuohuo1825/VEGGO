package com.veggo.app.presentation.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.local.entity.CommunityChefEntity;
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
    private boolean suggestionsExpanded;
    private String profileCustomerId;
    private String accountCustomerId;
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
        AppPreferences preferences = new AppPreferences(this);
        String accountId = firstNonBlank(preferences.getCustomerId(), CommunityRepository.ACCOUNT_ID);
        accountCustomerId = accountId;
        String accountName = firstNonBlank(preferences.getFullName(), "Tài khoản của bạn");
        String accountSubtitle = firstNonBlank(preferences.getEmail(), firstNonBlank(preferences.getCurrentPhone(), "Tài khoản Veggo"));
        String chefId = accountProfile ? accountId : getIntent().getStringExtra(EXTRA_CHEF_ID);
        profileCustomerId = chefId;
        String chefName = accountProfile ? accountName : getIntent().getStringExtra(EXTRA_CHEF_NAME);
        String chefImageUrl = accountProfile ? null : getIntent().getStringExtra(EXTRA_CHEF_IMAGE_URL);
        int recipeCount = getIntent().getIntExtra(EXTRA_RECIPE_COUNT, 0);
        int likes = getIntent().getIntExtra(EXTRA_LIKES, 0);

        bindHeader(chefName, accountSubtitle, chefImageUrl, recipeCount, likes);
        bindFollowNavigation(chefId);
        bindFollowAction(chefId);
        bindTabs();

        if (accountProfile) {
            bindFreshAccountUser(accountId);
        }

        repository.loadFollowCounts(chefId, counts -> runOnUiThread(() -> {
            ((TextView) findViewById(R.id.profileFollowingCount)).setText(String.valueOf(counts.following));
            ((TextView) findViewById(R.id.profileFollowerCount)).setText(String.valueOf(counts.followers));
            if (!accountProfile) {
                renderProfileFollow(counts.isFollowing);
            }
        }));

        loadProfileContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null && profileCustomerId != null) {
            loadProfileContent();
        }
    }

    private void loadProfileContent() {
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
            repository.loadRecipesByChef(accountCustomerId, recipeCallback);
            repository.loadCookbooks(accountCustomerId, items -> runOnUiThread(() -> {
                cookbooks.clear();
                cookbooks.addAll(items);
                if (savedSelected) {
                    showCookbooks();
                }
            }));
        } else {
            repository.loadRecipesByChef(profileCustomerId, recipeCallback);
        }
    }

    private void bindFreshAccountUser(String accountId) {
        repository.loadUser(accountId, user -> runOnUiThread(() -> {
            if (user == null || isBlank(user.getName())) {
                return;
            }
            ((TextView) findViewById(R.id.profileName)).setText(user.getName());
            ((TextView) findViewById(R.id.profileRecipeCount)).setText(String.valueOf(user.getRecipeCount()));
            if (!isBlank(user.getImageUrl())) {
                ImageView avatar = findViewById(R.id.profileAvatar);
                avatar.setPadding(0, 0, 0, 0);
                Glide.with(this)
                        .load(user.getImageUrl())
                        .transform(new CenterCrop(), new RoundedCorners(dp(12)))
                        .into(avatar);
            }
        }));
    }

    private void bindHeader(String chefName, String accountSubtitle, String chefImageUrl, int recipeCount, int likes) {
        View back = findViewById(R.id.profileBackButton);
        back.setOnClickListener(v -> finish());
        View followButton = findViewById(R.id.profileFollowButton);
        if (accountProfile) {
            followButton.setVisibility(View.GONE);
            findViewById(R.id.profileFollowActions).setVisibility(View.GONE);
            findViewById(R.id.profileSuggestionsSection).setVisibility(View.GONE);
            findViewById(R.id.profileShareButton).setVisibility(View.GONE);
            findViewById(R.id.profileAccountActions).setVisibility(View.VISIBLE);
            findViewById(R.id.profileAddButton).setOnClickListener(v ->
                    startActivity(new Intent(this, CommunityPostActivity.class)));
        } else {
            findViewById(R.id.profileFollowActions).setVisibility(View.VISIBLE);
            renderProfileFollow(false);
            bindSuggestedToggle();
            findViewById(R.id.profileShareButton).setVisibility(View.VISIBLE);
            findViewById(R.id.profileAccountActions).setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.profileName)).setText(chefName == null ? "" : chefName);
        TextView location = findViewById(R.id.profileLocation);
        if (accountProfile) {
            location.setVisibility(View.GONE);
        } else {
            location.setVisibility(View.VISIBLE);
            location.setText("Florida, US");
        }
        ((TextView) findViewById(R.id.profileRecipeCount)).setText(String.valueOf(recipeCount));
        ((TextView) findViewById(R.id.profileFollowingCount)).setText("0");
        ((TextView) findViewById(R.id.profileFollowerCount)).setText("0");

        ImageView avatar = findViewById(R.id.profileAvatar);
        ImageView hero = findViewById(R.id.profileHeroImage);
        if (accountProfile) {
            avatar.setPadding(dp(28), dp(28), dp(28), dp(28));
            avatar.setImageResource(R.drawable.ic_user_full);
            hero.setImageResource(R.drawable.bg_auth);
        } else {
            Glide.with(this)
                    .load(chefImageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(dp(12)))
                    .into(avatar);
            Glide.with(this)
                    .load(chefImageUrl)
                    .transform(new CenterCrop(), bottomRoundedCorners(18))
                    .into(hero);
        }
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

    private void bindFollowAction(String chefId) {
        if (accountProfile || isBlank(chefId) || chefId.equals(repository.currentCustomerId())) {
            return;
        }
        View followButton = findViewById(R.id.profileFollowButton);
        followButton.setOnClickListener(v -> repository.toggleFollow(chefId, counts -> runOnUiThread(() -> {
            renderProfileFollow(counts.isFollowing);
            ((TextView) findViewById(R.id.profileFollowerCount)).setText(String.valueOf(counts.followers));
        })));
    }

    private void renderProfileFollow(boolean following) {
        View button = findViewById(R.id.profileFollowButton);
        ImageView icon = findViewById(R.id.profileFollowIcon);
        TextView text = findViewById(R.id.profileFollowText);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.width = dp(following ? 44 : 148);
        button.setLayoutParams(params);
        button.setBackgroundResource(following ? R.drawable.bg_community_circle_green : R.drawable.bg_follow_button_green);
        icon.setImageResource(following ? R.drawable.ic_usercheck : R.drawable.ic_userplus);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.neutral_10));
        text.setVisibility(following ? View.GONE : View.VISIBLE);
    }

    private void bindSuggestedToggle() {
        View toggle = findViewById(R.id.profileSuggestToggleButton);
        ImageView icon = findViewById(R.id.profileSuggestToggleIcon);
        toggle.setOnClickListener(v -> {
            suggestionsExpanded = !suggestionsExpanded;
            findViewById(R.id.profileSuggestionsSection).setVisibility(suggestionsExpanded ? View.VISIBLE : View.GONE);
            icon.setImageResource(suggestionsExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
            if (suggestionsExpanded) {
                loadSuggestedUsers();
            }
        });
    }

    private void loadSuggestedUsers() {
        repository.loadChefs(users -> runOnUiThread(() -> {
            LinearLayout row = findViewById(R.id.profileSuggestionsRow);
            row.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
            int added = 0;
            for (CommunityChefEntity user : users) {
                if (user.getId().equals(profileCustomerId) || user.getId().equals(repository.currentCustomerId())) {
                    continue;
                }
                row.addView(suggestedUserCard(inflater, user));
                added++;
                if (added >= 8) {
                    break;
                }
            }
        }));
    }

    private View suggestedUserCard(LayoutInflater inflater, CommunityChefEntity user) {
        View card = inflater.inflate(R.layout.item_community_suggested_user, findViewById(R.id.profileSuggestionsRow), false);
        ImageView avatar = card.findViewById(R.id.suggestedAvatar);
        TextView name = card.findViewById(R.id.suggestedName);
        TextView meta = card.findViewById(R.id.suggestedMeta);
        TextView follow = card.findViewById(R.id.suggestedFollowButton);
        name.setText(user.getName());
        meta.setText(user.getLikes() + " người theo dõi");
        Glide.with(this)
                .load(user.getImageUrl())
                .placeholder(R.drawable.ic_user_full)
                .error(R.drawable.ic_user_full)
                .transform(new CenterCrop(), new CircleCrop())
                .into(avatar);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileActivity.class);
            intent.putExtra(EXTRA_CHEF_ID, user.getId());
            intent.putExtra(EXTRA_CHEF_NAME, user.getName());
            intent.putExtra(EXTRA_CHEF_IMAGE_URL, user.getImageUrl());
            intent.putExtra(EXTRA_RECIPE_COUNT, user.getRecipeCount());
            intent.putExtra(EXTRA_LIKES, user.getLikes());
            startActivity(intent);
        });
        follow.setOnClickListener(v -> repository.toggleFollow(user.getId(), counts -> runOnUiThread(() -> {
            follow.setText(counts.isFollowing ? "Đã theo dõi" : "Theo dõi");
            follow.setBackgroundResource(counts.isFollowing ? R.drawable.bg_community_circle : R.drawable.bg_follow_button_green);
            follow.setTextColor(ContextCompat.getColor(this, counts.isFollowing ? R.color.primary_main : R.color.neutral_10));
        })));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(190));
        params.setMarginEnd(dp(12));
        card.setLayoutParams(params);
        return card;
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

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
