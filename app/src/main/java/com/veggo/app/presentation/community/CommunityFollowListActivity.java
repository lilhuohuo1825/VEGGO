package com.veggo.app.presentation.community;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityFollowEntity;

import java.util.List;

public class CommunityFollowListActivity extends AppCompatActivity {
    public static final String EXTRA_CHEF_ID = "community_follow_chef_id";
    public static final String EXTRA_RELATION_TYPE = "community_follow_relation_type";

    private CommunityRepository repository;
    private LinearLayout listContainer;
    private String relationType;
    private String chefId;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_follow_list);

        repository = new CommunityRepository(this);
        listContainer = findViewById(R.id.followListContainer);
        relationType = getIntent().getStringExtra(EXTRA_RELATION_TYPE);
        if (relationType == null) {
            relationType = CommunityRepository.RELATION_FOLLOWER;
        }

        findViewById(R.id.followBackButton).setOnClickListener(v -> finish());
        bindTitle(0);

        chefId = getIntent().getStringExtra(EXTRA_CHEF_ID);
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.followScroll, this::loadFollows);
        loadFollows();
    }

    private void loadFollows() {
        repository.loadFollows(chefId, relationType, follows -> runOnUiThread(() -> {
            renderFollows(CommunityUi.shuffled(follows));
            CommunityUi.finishRefresh(refreshLayout);
        }));
    }

    private void renderFollows(List<CommunityFollowEntity> follows) {
        bindTitle(follows.size());
        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CommunityFollowEntity follow : follows) {
            View row = inflater.inflate(R.layout.item_community_follow_user, listContainer, false);
            bindRow(row, follow);
            listContainer.addView(row);
        }
    }

    private void bindTitle(int count) {
        boolean followingPage = CommunityRepository.RELATION_FOLLOWING.equals(relationType);
        String title = followingPage ? "Đang theo dõi" : "Người theo dõi";
        ((TextView) findViewById(R.id.followTitle)).setText(title);
        ((TextView) findViewById(R.id.followCountPill)).setText(count + (followingPage ? " Đang theo dõi" : " Người theo dõi"));
    }

    private void bindRow(View row, CommunityFollowEntity follow) {
        ((TextView) row.findViewById(R.id.followName)).setText(follow.getName());
        ((TextView) row.findViewById(R.id.followLocation)).setText(follow.getLocation());

        ImageView avatar = row.findViewById(R.id.followAvatar);
        Glide.with(this)
                .load(follow.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(27)))
                .into(avatar);

        ImageButton action = row.findViewById(R.id.followAction);
        bindFollowAction(action, follow.isFollowing());
    }

    private void bindFollowAction(ImageButton action, boolean following) {
        action.setTag(following);
        renderFollowAction(action, following);
        action.setOnClickListener(v -> {
            boolean next = !(Boolean) v.getTag();
            v.setTag(next);
            renderFollowAction((ImageButton) v, next);
        });
    }

    private void renderFollowAction(ImageButton action, boolean following) {
        if (following) {
            action.setBackgroundResource(R.drawable.bg_follow_button_green);
            action.setImageResource(R.drawable.ic_usercheck);
            action.setColorFilter(Color.WHITE);
        } else {
            action.setBackgroundResource(R.drawable.bg_community_circle);
            action.setImageResource(R.drawable.ic_userplus);
            action.setColorFilter(ContextCompat.getColor(this, R.color.neutral_100));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
