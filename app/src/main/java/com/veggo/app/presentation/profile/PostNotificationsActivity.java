package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.ArrayList;
import java.util.List;

public class PostNotificationsActivity extends BaseActivity {
    private static final String CATEGORY_ORDERS = "orders";
    private static final String CATEGORY_COMMUNITY = "community";
    private static final String CATEGORY_QA = "qa";
    private static final String CATEGORY_OTHER = "other";

    private final List<PostNotificationItem> notificationItems = new ArrayList<>();
    private LinearLayout notificationList;
    private String selectedCategory = CATEGORY_QA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notifications);
        findViewById(R.id.postNotificationsBackButton).setOnClickListener(v -> finish());
        notificationList = findViewById(R.id.postNotificationsList);
        findViewById(R.id.postNotificationTabOrders).setOnClickListener(v -> showNotifications(CATEGORY_ORDERS));
        findViewById(R.id.postNotificationTabCommunity).setOnClickListener(v -> showNotifications(CATEGORY_COMMUNITY));
        findViewById(R.id.postNotificationTabQa).setOnClickListener(v -> showNotifications(CATEGORY_QA));
        findViewById(R.id.postNotificationTabOther).setOnClickListener(v -> showNotifications(CATEGORY_OTHER));
        loadNotifications();
    }

    private void loadNotifications() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            List<AssetModels.CommunityPost> posts = AssetScreenData.postsForCurrentUser(snapshot);
            runOnUiThread(() -> {
                buildNotifications(posts);
                showNotifications(selectedCategory);
            });
        }).start();
    }

    private void buildNotifications(List<AssetModels.CommunityPost> posts) {
        notificationItems.clear();
        for (AssetModels.CommunityPost post : posts) {
            if (post.likeCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        CATEGORY_COMMUNITY,
                        post.likeCount + " người vừa thả tym",
                        "Bài \"" + post.title + "\" đang được quan tâm.",
                        "Xem bài viết",
                        "Hôm nay",
                        "♥",
                        false
                ));
            }
            if (post.commentCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        CATEGORY_COMMUNITY,
                        post.commentCount + " bình luận mới",
                        latestCommentText(post),
                        "Xem bình luận",
                        "Hôm nay",
                        "!",
                        false
                ));
            }
            if (post.saveCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        CATEGORY_COMMUNITY,
                        "Bài viết được lưu " + post.saveCount + " lần",
                        "Cộng đồng đang lưu bài \"" + post.title + "\".",
                        "Xem thống kê",
                        "Tuần này",
                        "✓",
                        false
                ));
            }
        }
    }

    private void showNotifications(String category) {
        selectedCategory = category;
        updateSelectedTab(category);
        if (notificationList == null) {
            return;
        }
        notificationList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PostNotificationItem item : notificationItems) {
            if (item.category.equals(category)) {
                addNotification(inflater, notificationList, item);
            }
        }
    }

    private void addNotification(LayoutInflater inflater, LinearLayout list, PostNotificationItem item) {
        View itemView = inflater.inflate(R.layout.item_post_notification_like, list, false);
        itemView.setBackgroundResource(item.featured
                ? R.drawable.bg_post_notification_featured_card
                : R.drawable.bg_order_card);
        AssetScreenData.setText(itemView, R.id.postNotificationIconText, item.iconText);
        AssetScreenData.setText(itemView, R.id.postNotificationTitle, item.title);
        AssetScreenData.setText(itemView, R.id.postNotificationBody, item.body);
        AssetScreenData.setText(itemView, R.id.postNotificationAction, item.action);
        AssetScreenData.setText(itemView, R.id.postNotificationTime, item.time);
        list.addView(itemView);
    }

    private void updateSelectedTab(String category) {
        setTabSelected(
                R.id.postNotificationTabOrdersText,
                R.id.postNotificationTabOrdersBadge,
                R.id.postNotificationTabOrdersIndicator,
                CATEGORY_ORDERS.equals(category),
                countNotifications(CATEGORY_ORDERS)
        );
        setTabSelected(
                R.id.postNotificationTabCommunityText,
                R.id.postNotificationTabCommunityBadge,
                R.id.postNotificationTabCommunityIndicator,
                CATEGORY_COMMUNITY.equals(category),
                countNotifications(CATEGORY_COMMUNITY)
        );
        setTabSelected(
                R.id.postNotificationTabQaText,
                R.id.postNotificationTabQaBadge,
                R.id.postNotificationTabQaIndicator,
                CATEGORY_QA.equals(category),
                countNotifications(CATEGORY_QA)
        );
        setTabSelected(
                R.id.postNotificationTabOtherText,
                R.id.postNotificationTabOtherBadge,
                R.id.postNotificationTabOtherIndicator,
                CATEGORY_OTHER.equals(category),
                countNotifications(CATEGORY_OTHER)
        );
    }

    private int countNotifications(String category) {
        int count = 0;
        for (PostNotificationItem item : notificationItems) {
            if (item.category.equals(category)) {
                count++;
            }
        }
        return count;
    }

    private void setTabSelected(int textId, int badgeId, int indicatorId, boolean selected, int count) {
        TextView text = findViewById(textId);
        TextView badge = findViewById(badgeId);
        View indicator = findViewById(indicatorId);
        if (text != null) {
            text.setTextColor(getColor(selected ? R.color.primary_main : R.color.neutral_60));
            text.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (badge != null) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            badge.setBackgroundResource(selected
                    ? R.drawable.bg_notification_badge_alert
                    : R.drawable.bg_notification_badge_dark);
        }
        if (indicator != null) {
            indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private String latestCommentText(AssetModels.CommunityPost post) {
        if (post.commentsDetail == null || post.commentsDetail.isEmpty()) {
            return "Có bình luận mới trong bài \"" + post.title + "\".";
        }
        return "\"" + post.commentsDetail.get(post.commentsDetail.size() - 1).content + "\"";
    }

    private static class PostNotificationItem {
        final String category;
        final String title;
        final String body;
        final String action;
        final String time;
        final String iconText;
        final boolean featured;

        PostNotificationItem(
                String category,
                String title,
                String body,
                String action,
                String time,
                String iconText,
                boolean featured
        ) {
            this.category = category;
            this.title = title;
            this.body = body;
            this.action = action;
            this.time = time;
            this.iconText = iconText;
            this.featured = featured;
        }
    }
}
