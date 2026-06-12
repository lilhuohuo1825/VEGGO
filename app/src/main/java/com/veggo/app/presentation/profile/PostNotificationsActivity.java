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
        notificationItems.add(new PostNotificationItem(
                CATEGORY_QA,
                "Admin đã phản hồi câu hỏi",
                "Câu hỏi về cách bảo quản cải kale đã được trả lời.",
                "Xem trả lời",
                "Hôm nay",
                "?",
                true
        ));
        notificationItems.add(new PostNotificationItem(
                CATEGORY_QA,
                "Câu hỏi có phản hồi mới",
                "Một người dùng khác bổ sung kinh nghiệm chọn cà chua bi.",
                "Xem Q&A",
                "Hôm qua",
                "!",
                false
        ));
        notificationItems.add(new PostNotificationItem(
                CATEGORY_ORDERS,
                "Đơn hàng đang được giao",
                "Đơn rau củ tươi của bạn dự kiến đến trong khung 16:00 - 18:00.",
                "Theo dõi đơn",
                "Hôm nay",
                "✓",
                true
        ));
        notificationItems.add(new PostNotificationItem(
                CATEGORY_ORDERS,
                "Nhắc đánh giá đơn hàng",
                "Đơn cải kale organic đã hoàn tất. Chia sẻ nhận xét để nhận điểm carbon.",
                "Đánh giá",
                "2 ngày trước",
                "!",
                false
        ));
        LayoutInflater inflater = LayoutInflater.from(this);
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
        notificationItems.add(new PostNotificationItem(
                CATEGORY_OTHER,
                "Ưu đãi cá nhân mới",
                "Bạn có voucher freeship cho đơn rau củ từ 199.000đ.",
                "Xem ưu đãi",
                "Hôm nay",
                "%",
                true
        ));
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
                R.id.postNotificationTabOrdersIndicator,
                CATEGORY_ORDERS.equals(category)
        );
        setTabSelected(
                R.id.postNotificationTabCommunityText,
                R.id.postNotificationTabCommunityIndicator,
                CATEGORY_COMMUNITY.equals(category)
        );
        setTabSelected(
                R.id.postNotificationTabQaText,
                R.id.postNotificationTabQaIndicator,
                CATEGORY_QA.equals(category)
        );
        setTabSelected(
                R.id.postNotificationTabOtherText,
                R.id.postNotificationTabOtherIndicator,
                CATEGORY_OTHER.equals(category)
        );
    }

    private void setTabSelected(int textId, int indicatorId, boolean selected) {
        TextView text = findViewById(textId);
        View indicator = findViewById(indicatorId);
        if (text != null) {
            text.setTextColor(getColor(selected ? R.color.primary_main : R.color.neutral_60));
            text.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
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
