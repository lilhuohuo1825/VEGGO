package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

public class MyPostsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);
        findViewById(R.id.myPostsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.myPostsAddButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class))
        );
        findViewById(R.id.myPostsNotificationButton).setOnClickListener(v ->
                startActivity(new Intent(this, PostNotificationsActivity.class))
        );
        loadPosts();
    }

    private void loadPosts() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            List<AssetModels.CommunityPost> posts = AssetScreenData.postsForCurrentUser(snapshot);
            runOnUiThread(() -> bindPosts(posts));
        }).start();
    }

    private void bindPosts(List<AssetModels.CommunityPost> posts) {
        int likes = 0;
        int comments = 0;
        int saves = 0;
        for (AssetModels.CommunityPost post : posts) {
            likes += post.likeCount;
            comments += post.commentCount;
            saves += post.saveCount;
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.myPostsCount, posts.size() + " bài");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.myPostsTotalPosts, posts.size() + "\nBài đăng");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.myPostsTotalLikes, likes + "\nTym");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.myPostsTotalComments, comments + "\nBình luận");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.myPostsTotalSaves, saves + "\nLưu");

        LinearLayout list = findViewById(R.id.myPostsList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.CommunityPost post : posts) {
            View item = inflater.inflate(R.layout.item_my_post_published, list, false);
            AssetScreenData.setText(item, R.id.myPostTitle, post.title);
            AssetScreenData.setText(item, R.id.myPostContent, post.content);
            AssetScreenData.setText(item, R.id.myPostStatus, statusText(post.status));
            AssetScreenData.setText(item, R.id.myPostLikes, post.likeCount + " tym");
            AssetScreenData.setText(item, R.id.myPostComments, post.commentCount + " bình luận");
            AssetScreenData.setText(item, R.id.myPostSaves, post.saveCount + " lưu");
            AssetScreenData.setText(item, R.id.myPostDate, AssetScreenData.dateText(post.publishDate));
            ImageView image = item.findViewById(R.id.myPostImage);
            if (image != null && AssetScreenData.hasText(post.image)) {
                Glide.with(this).load(post.image).placeholder(R.drawable.ic_vegetable).into(image);
            }
            list.addView(item);
        }
    }

    private String statusText(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return getString(R.string.posts_status_published);
        }
        return getString(R.string.posts_status_draft);
    }
}
