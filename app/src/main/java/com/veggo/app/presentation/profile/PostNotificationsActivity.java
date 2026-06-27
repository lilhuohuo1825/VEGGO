package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.dto.OrderNotificationDto;
import com.veggo.app.data.repository.OrderNotificationRepository;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.order.OrderDetailActivity;
import com.veggo.app.presentation.promotion.PromotionDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostNotificationsActivity extends BaseActivity {
    private static final String CATEGORY_ORDERS = "orders";
    private static final String CATEGORY_COMMUNITY = "community";
    private static final String CATEGORY_QA = "qa";
    private static final String CATEGORY_OTHER = "other";
    private static final String LOCAL_NOTIFICATION_PREFS = "local_notification_read_state";
    private static final String KEY_LOCAL_NOTIFICATIONS_READ = "local_notifications_read";

    private final List<PostNotificationItem> notificationItems = new ArrayList<>();
    private final OrderNotificationRepository orderNotificationRepository = new OrderNotificationRepository();
    private LinearLayout notificationList;
    private String selectedCategory = CATEGORY_ORDERS;

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
        findViewById(R.id.postNotificationsMarkAllRead).setOnClickListener(v -> markAllNotificationsRead());
        updateSelectedTab(selectedCategory);
        loadNotifications();
    }

    private void loadNotifications() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            List<AssetModels.CommunityPost> posts = AssetScreenData.postsForCurrentUser(snapshot);
            List<OrderNotificationDto> orderNotifications = loadOrderNotifications();
            runOnUiThread(() -> {
                buildNotifications(posts, orderNotifications);
                showNotifications(selectedCategory);
            });
        }).start();
    }

    private List<OrderNotificationDto> loadOrderNotifications() {
        return orderNotificationRepository.getNotificationsSync(new AppPreferences(this).getCustomerId());
    }

    private void buildNotifications(List<AssetModels.CommunityPost> posts, List<OrderNotificationDto> orderNotifications) {
        notificationItems.clear();
        boolean localUnread = areLocalNotificationsUnread();
        for (OrderNotificationDto notification : orderNotifications) {
            String category = firstNonBlank(notification.getCategory(), CATEGORY_ORDERS);
            String targetType = firstNonBlank(notification.getTargetType(), CATEGORY_ORDERS.equals(category) ? "order" : "");
            String targetId = firstNonBlank(notification.getTargetId(), "");
            notificationItems.add(new PostNotificationItem(
                    notification.getId(),
                    normalizeCategory(category),
                    firstNonBlank(notification.getTitle(), "Cập nhật đơn hàng"),
                    firstNonBlank(notification.getBody(), ""),
                    firstNonBlank(notification.getAction(), "Theo dõi đơn"),
                    AssetScreenData.dateText(notification.getCreatedAtText()),
                    notificationIcon(category, notification.getType(), notification.getTitle(), notification.getBody()),
                    true,
                    !notification.isRead(),
                    targetType,
                    targetId
            ));
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.CommunityPost post : posts) {
            if (post.likeCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        "",
                        CATEGORY_COMMUNITY,
                        post.likeCount + " người vừa thả tym",
                        "Bài \"" + post.title + "\" đang được quan tâm.",
                        "Xem bài viết",
                        "Hôm nay",
                        R.drawable.ic_profile_article,
                        false,
                        false,
                        "community",
                        ""
                ));
            }
            if (post.commentCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        "",
                        CATEGORY_COMMUNITY,
                        post.commentCount + " bình luận mới",
                        latestCommentText(post),
                        "Xem bình luận",
                        "Hôm nay",
                        R.drawable.ic_profile_article,
                        false,
                        false,
                        "community",
                        ""
                ));
            }
            if (post.saveCount > 0) {
                notificationItems.add(new PostNotificationItem(
                        "",
                        CATEGORY_COMMUNITY,
                        "Bài viết được lưu " + post.saveCount + " lần",
                        "Cộng đồng đang lưu bài \"" + post.title + "\".",
                        "Xem thống kê",
                        "Tuần này",
                        R.drawable.ic_profile_article,
                        false,
                        false,
                        "community",
                        ""
                ));
            }
        }
        notificationItems.add(new PostNotificationItem(
                "",
                CATEGORY_OTHER,
                "Ưu đãi cá nhân mới",
                "Bạn có voucher freeship cho đơn rau củ từ 199.000đ.",
                "Xem ưu đãi",
                "Hôm nay",
                R.drawable.ic_order_payment_receipt,
                false,
                false,
                "promotion",
                ""
        ));

        // Đọc thông báo nguyên liệu sắp hết hạn từ SharedPreferences
        SharedPreferences expiryPrefs = getSharedPreferences("fridge_notifications", MODE_PRIVATE);
        String expiryItems = expiryPrefs.getString("fridge_expiry_items_json", null);
        long expiryTime = expiryPrefs.getLong("fridge_expiry_time", 0);
        if (expiryItems != null && !expiryItems.isEmpty()) {
            // Chỉ hiển thị trong vòng 2 ngày
            long twoDaysMs = 2L * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() - expiryTime < twoDaysMs) {
                String[] names = expiryItems.split(", ");
                String title = "⚠️ Nguyên liệu sắp hết hạn!";
                String body;
                if (names.length == 1) {
                    body = names[0] + " sẽ hết hạn vào ngày mai. Hãy sử dụng sớm!";
                } else {
                    body = names.length + " nguyên liệu sắp hết hạn: " + expiryItems;
                }
                notificationItems.add(new PostNotificationItem(
                        "",
                        CATEGORY_OTHER,
                        title,
                        body,
                        "Xem tủ lạnh",
                        "Hôm nay",
                        R.drawable.ic_order_list_menu,
                        false,
                        localUnread,
                        "fridge",
                        ""
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
        itemView.setBackgroundResource(item.unread
                ? R.drawable.bg_post_notification_featured_card
                : R.drawable.bg_post_notification_order_card);
        ImageView icon = itemView.findViewById(R.id.postNotificationIcon);
        if (icon != null) {
            icon.setImageResource(item.iconResId);
        }
        AssetScreenData.setText(itemView, R.id.postNotificationTitle, item.title);
        AssetScreenData.setText(itemView, R.id.postNotificationBody, item.body);
        AssetScreenData.setText(itemView, R.id.postNotificationAction, item.action);
        AssetScreenData.setText(itemView, R.id.postNotificationTime, item.time);
        itemView.setOnClickListener(v -> openNotification(item));
        list.addView(itemView);
    }

    private void updateSelectedTab(String category) {
        setTabSelected(
                R.id.postNotificationTabOrdersText,
                R.id.postNotificationTabOrdersBadge,
                R.id.postNotificationTabOrdersIndicator,
                CATEGORY_ORDERS.equals(category),
                countUnreadNotifications(CATEGORY_ORDERS)
        );
        setTabSelected(
                R.id.postNotificationTabCommunityText,
                R.id.postNotificationTabCommunityBadge,
                R.id.postNotificationTabCommunityIndicator,
                CATEGORY_COMMUNITY.equals(category),
                countUnreadNotifications(CATEGORY_COMMUNITY)
        );
        setTabSelected(
                R.id.postNotificationTabQaText,
                R.id.postNotificationTabQaBadge,
                R.id.postNotificationTabQaIndicator,
                CATEGORY_QA.equals(category),
                countUnreadNotifications(CATEGORY_QA)
        );
        setTabSelected(
                R.id.postNotificationTabOtherText,
                R.id.postNotificationTabOtherBadge,
                R.id.postNotificationTabOtherIndicator,
                CATEGORY_OTHER.equals(category),
                countUnreadNotifications(CATEGORY_OTHER)
        );
    }

    private int countUnreadNotifications(String category) {
        int count = 0;
        for (PostNotificationItem item : notificationItems) {
            if (item.category.equals(category) && item.unread) {
                count++;
            }
        }
        return count;
    }

    private void markAllNotificationsRead() {
        boolean hadUnread = false;
        for (PostNotificationItem item : notificationItems) {
            if (item.unread) {
                hadUnread = true;
            }
            item.unread = false;
        }
        showNotifications(selectedCategory);
        if (!hadUnread) {
            Toast.makeText(this, "Không có thông báo chưa đọc", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
        getSharedPreferences(LOCAL_NOTIFICATION_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOCAL_NOTIFICATIONS_READ, true)
                .apply();

        new Thread(() -> {
            try {
                String customerId = new AppPreferences(this).getCustomerId();
                if (customerId == null || customerId.trim().isEmpty()) {
                    return;
                }
                orderNotificationRepository.markAllReadSync(customerId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean areLocalNotificationsUnread() {
        return !getSharedPreferences(LOCAL_NOTIFICATION_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_LOCAL_NOTIFICATIONS_READ, true);
    }

    private void markNotificationRead(PostNotificationItem item) {
        if (!item.unread) {
            return;
        }
        item.unread = false;
        showNotifications(selectedCategory);
        if (!item.remoteOrder || item.id == null || item.id.trim().isEmpty()) {
            if (countUnreadLocalNotifications() == 0) {
                getSharedPreferences(LOCAL_NOTIFICATION_PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_LOCAL_NOTIFICATIONS_READ, true)
                        .apply();
            }
            return;
        }
        new Thread(() -> {
            try {
                orderNotificationRepository.markReadSync(item.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openNotification(PostNotificationItem item) {
        markNotificationRead(item);
        Intent intent = intentForNotification(item);
        if (intent != null) {
            startActivity(intent);
        }
    }

    private Intent intentForNotification(PostNotificationItem item) {
        String targetType = safeLower(item.targetType);
        String targetId = item.targetId == null ? "" : item.targetId.trim();
        if ("order".equals(targetType) || CATEGORY_ORDERS.equals(item.category)) {
            if (targetId.isEmpty()) {
                targetId = item.id;
            }
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, targetId);
            return intent;
        }
        if ("certificate".equals(targetType)) {
            return new Intent(this, CarbonCertificateActivity.class);
        }
        if ("fridge".equals(targetType)) {
            return new Intent(this, SmartFridgeActivity.class);
        }
        if ("promotion".equals(targetType)) {
            Intent intent = new Intent(this, PromotionDetailActivity.class);
            if (!targetId.isEmpty()) {
                intent.putExtra(PromotionDetailActivity.EXTRA_PROMOTION_ID, targetId);
                return intent;
            }
            return new Intent(this, com.veggo.app.MainActivity.class);
        }
        if ("community".equals(targetType) || CATEGORY_COMMUNITY.equals(item.category)) {
            return new Intent(this, CommunityHomeActivity.class);
        }
        if ("support".equals(targetType) || CATEGORY_QA.equals(item.category)) {
            return new Intent(this, SupportCustomersActivity.class);
        }
        if (CATEGORY_OTHER.equals(item.category)) {
            return new Intent(this, CarbonPointsActivity.class);
        }
        return null;
    }

    private int countUnreadLocalNotifications() {
        int count = 0;
        for (PostNotificationItem notification : notificationItems) {
            if (!notification.remoteOrder && notification.unread) {
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

    private String mapText(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Map) {
            Object date = ((Map<?, ?>) value).get("$date");
            if (date != null) {
                return String.valueOf(date);
            }
            Object oid = ((Map<?, ?>) value).get("$oid");
            return oid == null ? fallback : String.valueOf(oid);
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String mapNotificationId(Map<String, Object> map) {
        return mapText(map, "_id", "");
    }

    private String normalizeCategory(String category) {
        String value = safeLower(category);
        if (CATEGORY_COMMUNITY.equals(value) || CATEGORY_QA.equals(value) || CATEGORY_OTHER.equals(value)) {
            return value;
        }
        return CATEGORY_ORDERS;
    }

    private int notificationIcon(String category, String type, String title, String body) {
        if (CATEGORY_QA.equals(normalizeCategory(category))) {
            return R.drawable.ic_order_list_menu;
        }

        String text = (safeLower(type) + " " + safeLower(title) + " " + safeLower(body)).trim();
        if (text.contains("payment") || text.contains("thanh toán") || text.contains("paid")) {
            return R.drawable.ic_order_payment_receipt;
        }
        if (text.contains("certificate") || text.contains("chứng nhận") || text.contains("carbon")) {
            return R.drawable.ic_profile_leaf;
        }
        if (text.contains("return") || text.contains("hoàn") || text.contains("trả hàng") || text.contains("đổi trả")) {
            return R.drawable.ic_order_return_option;
        }
        if (text.contains("review") || text.contains("đánh giá")) {
            return R.drawable.ic_order_review_option;
        }
        if (text.contains("recurring") || text.contains("định kỳ")) {
            return R.drawable.ic_order_recurring_option;
        }
        if (text.contains("delivered") || text.contains("đã giao") || text.contains("đã nhận")) {
            return R.drawable.ic_order_delivered_box;
        }
        if (text.contains("shipping") || text.contains("đang giao") || text.contains("giao hàng")) {
            return R.drawable.ic_order_shipping_truck;
        }
        return R.drawable.ic_order_pending_box;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static class PostNotificationItem {
        final String id;
        final String category;
        final String title;
        final String body;
        final String action;
        final String time;
        final int iconResId;
        final boolean remoteOrder;
        final String targetType;
        final String targetId;
        boolean unread;

        PostNotificationItem(
                String id,
                String category,
                String title,
                String body,
                String action,
                String time,
                int iconResId,
                boolean remoteOrder,
                boolean unread
        ) {
            this(id, category, title, body, action, time, iconResId, remoteOrder, unread, "", "");
        }

        PostNotificationItem(
                String id,
                String category,
                String title,
                String body,
                String action,
                String time,
                int iconResId,
                boolean remoteOrder,
                boolean unread,
                String targetType,
                String targetId
        ) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.body = body;
            this.action = action;
            this.time = time;
            this.iconResId = iconResId;
            this.remoteOrder = remoteOrder;
            this.unread = unread;
            this.targetType = targetType;
            this.targetId = targetId;
        }
    }
}
