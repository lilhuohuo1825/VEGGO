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

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.notification.RecurringInAppNotificationStore;
import com.veggo.app.core.preferences.AppPreferences;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.CurvedTabIndicatorHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.remote.dto.OrderNotificationDto;
import com.veggo.app.data.repository.OrderNotificationRepository;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.community.CommunityProfileActivity;
import com.veggo.app.presentation.community.CommunityRecipeDetailActivity;
import com.veggo.app.presentation.order.OrderDetailActivity;
import com.veggo.app.presentation.order.RecurringConfirmOrderActivity;
import com.veggo.app.presentation.order.RecurringOrderStore;
import com.veggo.app.presentation.order.ReviewsActivity;
import com.veggo.app.presentation.product.ProductDetailActivity;
import com.veggo.app.presentation.promotion.PromotionDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostNotificationsActivity extends BaseActivity {
    private static final String CATEGORY_ORDERS = "orders";
    private static final String CATEGORY_COMMUNITY = "community";
    private static final String CATEGORY_QA = "qa";
    private static final String CATEGORY_OTHER = "other";
    private static final String TYPE_CONSULTATION_ANSWER = "consultation_answer";
    private static final String TYPE_CONSULTATION_LIKE = "consultation_like";
    private static final String TYPE_CONSULTATION_REPLY = "consultation_reply";
    private static final String TYPE_COMMUNITY_FOLLOW = "community_follow";
    private static final String TYPE_REVIEW = "review";
    private static final String TYPE_CERTIFICATE_ELIGIBLE = "certificate_eligible";
    private static final String TYPE_CERTIFICATE_APPROVED = "certificate_approved";
    private static final String TYPE_RECURRING_CONFIRM = RecurringInAppNotificationStore.TYPE_CONFIRM;
    private static final String TYPE_RECURRING_DELIVERY = RecurringInAppNotificationStore.TYPE_DELIVERY;
    private static final String TYPE_RECURRING_SKIPPED = RecurringInAppNotificationStore.TYPE_SKIPPED;
    private static final String TARGET_RECURRING_ORDER = "recurring_order";
    private static final String TYPE_PROMOTION_AVAILABLE = "promotion_available";
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("#(ORD\\d+)");
    private static final String LOCAL_NOTIFICATION_PREFS = "local_notification_read_state";
    private static final String KEY_LOCAL_NOTIFICATIONS_READ = "local_notifications_read";

    private final List<PostNotificationItem> notificationItems = new ArrayList<>();
    private final OrderNotificationRepository orderNotificationRepository = new OrderNotificationRepository();
    private LinearLayout notificationList;
    private String selectedCategory = CATEGORY_ORDERS;
    private SwipeRefreshLayout notificationsRefreshLayout;
    private CurvedTabIndicatorHelper tabIndicator;

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
        tabIndicator = CurvedTabIndicatorHelper.attach(
                (android.widget.HorizontalScrollView) findViewById(R.id.postNotificationCategoryScroll),
                new CurvedTabIndicatorHelper.TabItem(
                        findViewById(R.id.postNotificationTabOrders),
                        findViewById(R.id.postNotificationTabOrdersIndicator)
                ),
                new CurvedTabIndicatorHelper.TabItem(
                        findViewById(R.id.postNotificationTabCommunity),
                        findViewById(R.id.postNotificationTabCommunityIndicator)
                ),
                new CurvedTabIndicatorHelper.TabItem(
                        findViewById(R.id.postNotificationTabQa),
                        findViewById(R.id.postNotificationTabQaIndicator)
                ),
                new CurvedTabIndicatorHelper.TabItem(
                        findViewById(R.id.postNotificationTabOther),
                        findViewById(R.id.postNotificationTabOtherIndicator)
                )
        );
        updateSelectedTab(selectedCategory);
        setupPullToRefresh();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void setupPullToRefresh() {
        notificationsRefreshLayout = PullToRefreshHelper.wrap(
                findViewById(R.id.postNotificationsScroll),
                this::loadNotifications
        );
    }

    private void loadNotifications() {
        new Thread(() -> {
            String customerId = new AppPreferences(this).getCustomerId();
            new RecurringOrderStore(this).syncFromRemote(customerId);
            List<OrderNotificationDto> orderNotifications = loadOrderNotifications();
            runOnUiThread(() -> {
                buildNotifications(orderNotifications);
                showNotifications(selectedCategory);
                PullToRefreshHelper.finish(notificationsRefreshLayout);
            });
        }).start();
    }

    private List<OrderNotificationDto> loadOrderNotifications() {
        return orderNotificationRepository.getNotificationsSync(new AppPreferences(this).getCustomerId());
    }

    private void buildNotifications(List<OrderNotificationDto> orderNotifications) {
        notificationItems.clear();
        boolean localUnread = areLocalNotificationsUnread();
        for (OrderNotificationDto notification : orderNotifications) {
            String category = firstNonBlank(notification.getCategory(), CATEGORY_ORDERS);
            String targetType = firstNonBlank(notification.getTargetType(), CATEGORY_ORDERS.equals(category) ? "order" : "");
            String targetId = firstNonBlank(notification.getTargetId(), firstNonBlank(notification.getSku(), ""));
            String type = firstNonBlank(notification.getType(), "");
            
            long ts = System.currentTimeMillis();
            if (notification.getCreatedAtText() != null) {
                try {
                    ts = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                            .parse(notification.getCreatedAtText()).getTime();
                } catch (Exception ignored) {}
            }
            
            notificationItems.add(new PostNotificationItem(
                    notification.getId(),
                    normalizeCategory(category),
                    firstNonBlank(notification.getTitle(), "Cập nhật đơn hàng"),
                    firstNonBlank(notification.getBody(), ""),
                    firstNonBlank(notification.getAction(), "Theo dõi đơn"),
                    AssetScreenData.dateText(notification.getCreatedAtText()),
                    notificationIcon(category, type, notification.getTitle(), notification.getBody()),
                    true,
                    !notification.isRead(),
                    targetType,
                    targetId,
                    type,
                    ts
            ));
        }

        RecurringInAppNotificationStore recurringNotificationStore = new RecurringInAppNotificationStore(this);
        for (RecurringInAppNotificationStore.Entry entry : recurringNotificationStore.all()) {
            notificationItems.add(new PostNotificationItem(
                    entry.id,
                    CATEGORY_ORDERS,
                    entry.title,
                    entry.body,
                    entry.action,
                    RecurringInAppNotificationStore.formatTime(entry.createdAt),
                    R.drawable.ic_order_recurring_option,
                    false,
                    !entry.read,
                    TARGET_RECURRING_ORDER,
                    RecurringInAppNotificationStore.targetId(entry.recurringOrderId, entry.occurrenceDate),
                    entry.type,
                    entry.createdAt
            ));
        }

        // Đọc thông báo nhắc nhở giảm giá từ SharedPreferences để hiện trong tab "Khác"
        SharedPreferences reminderPrefs = getSharedPreferences("price_alert_reminders", MODE_PRIVATE);
        java.util.Map<String, ?> allEntries = reminderPrefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("reminder_") && entry.getValue() instanceof String) {
                String productId = entry.getKey().substring("reminder_".length());
                String valueStr = (String) entry.getValue();
                String[] parts = valueStr.split("\\|");
                if (parts.length >= 4) {
                    String pName = parts[0];
                    String percent = parts[1];
                    String days = parts[2];
                    try {
                        long time = Long.parseLong(parts[3]);
                        // Hiển thị lời nhắc này trong vòng 7 ngày kể từ khi tạo
                        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
                        if (System.currentTimeMillis() - time < sevenDaysMs) {
                            notificationItems.add(new PostNotificationItem(
                                    "",
                                    CATEGORY_OTHER,
                                    "⏰ Lời nhắc giảm giá đã tạo",
                                    "Bạn đã tạo lời nhắc cho \"" + pName + "\" (dự kiến giảm khoảng " + percent + "% vào " + days + " ngày tới).",
                                    "Xem sản phẩm",
                                    "Hôm nay",
                                    R.drawable.ic_order_list_menu,
                                    false,
                                    false,
                                    "product",
                                    productId,
                                    time
                            ));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

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
                        "",
                        expiryTime
                ));
            }
        }

        // Sắp xếp danh sách thông báo theo thời gian giảm dần (mới nhất lên đầu)
        java.util.Collections.sort(notificationItems, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));
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
        CardView card = itemView.findViewById(R.id.postNotificationCard);
        if (card != null) {
            int color = ContextCompat.getColor(
                    this,
                    item.unread ? R.color.primary_bg : R.color.background_main
            );
            card.setCardBackgroundColor(color);
        }
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
        int index = categoryTabIndex(category);
        setTabSelected(
                R.id.postNotificationTabOrdersText,
                R.id.postNotificationTabOrdersBadge,
                CATEGORY_ORDERS.equals(category),
                countUnreadNotifications(CATEGORY_ORDERS)
        );
        setTabSelected(
                R.id.postNotificationTabCommunityText,
                R.id.postNotificationTabCommunityBadge,
                CATEGORY_COMMUNITY.equals(category),
                countUnreadNotifications(CATEGORY_COMMUNITY)
        );
        setTabSelected(
                R.id.postNotificationTabQaText,
                R.id.postNotificationTabQaBadge,
                CATEGORY_QA.equals(category),
                countUnreadNotifications(CATEGORY_QA)
        );
        setTabSelected(
                R.id.postNotificationTabOtherText,
                R.id.postNotificationTabOtherBadge,
                CATEGORY_OTHER.equals(category),
                countUnreadNotifications(CATEGORY_OTHER)
        );
        if (tabIndicator != null) {
            tabIndicator.selectTab(index);
        }
    }

    private int categoryTabIndex(String category) {
        switch (normalizeCategory(category)) {
            case CATEGORY_COMMUNITY:
                return 1;
            case CATEGORY_QA:
                return 2;
            case CATEGORY_OTHER:
                return 3;
            default:
                return 0;
        }
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
        new RecurringInAppNotificationStore(this).markAllRead();
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
        if (TARGET_RECURRING_ORDER.equals(item.targetType)) {
            new RecurringInAppNotificationStore(this).markRead(item.id);
            if (countUnreadLocalNotifications() == 0) {
                getSharedPreferences(LOCAL_NOTIFICATION_PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_LOCAL_NOTIFICATIONS_READ, true)
                        .apply();
            }
            return;
        }
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
        String type = safeLower(item.type);

        if (TYPE_CONSULTATION_ANSWER.equals(type)
                || TYPE_CONSULTATION_LIKE.equals(type)
                || TYPE_CONSULTATION_REPLY.equals(type)
                || CATEGORY_QA.equals(item.category)
                || "support".equals(targetType)) {
            return productDetailIntent(targetId, true);
        }

        if (TYPE_COMMUNITY_FOLLOW.equals(type)) {
            if (!targetId.isEmpty()) {
                Intent intent = new Intent(this, CommunityProfileActivity.class);
                intent.putExtra(CommunityProfileActivity.EXTRA_CHEF_ID, targetId);
                return intent;
            }
            return new Intent(this, CommunityHomeActivity.class);
        }

        if (type.startsWith("community_") || CATEGORY_COMMUNITY.equals(item.category) || "community".equals(targetType)) {
            if (!targetId.isEmpty()) {
                Intent intent = new Intent(this, CommunityRecipeDetailActivity.class);
                intent.putExtra(CommunityRecipeDetailActivity.EXTRA_RECIPE_ID, targetId);
                return intent;
            }
            return new Intent(this, CommunityHomeActivity.class);
        }

        if (TYPE_CERTIFICATE_ELIGIBLE.equals(type)
                || TYPE_CERTIFICATE_APPROVED.equals(type)
                || "certificate".equals(targetType)) {
            return new Intent(this, CarbonCertificateActivity.class);
        }

        if (TYPE_PROMOTION_AVAILABLE.equals(type) || "promotion".equals(targetType)) {
            if (!targetId.isEmpty()) {
                Intent intent = new Intent(this, PromotionDetailActivity.class);
                intent.putExtra(PromotionDetailActivity.EXTRA_PROMOTION_ID, targetId);
                return intent;
            }
            return new Intent(this, CarbonPointsActivity.class);
        }

        if (TYPE_REVIEW.equals(type)) {
            Intent intent = new Intent(this, ReviewsActivity.class);
            intent.putExtra(ReviewsActivity.EXTRA_SHOW_DONE_REVIEWS, true);
            return intent;
        }

        if (TARGET_RECURRING_ORDER.equals(targetType)
                || TYPE_RECURRING_CONFIRM.equals(type)
                || TYPE_RECURRING_DELIVERY.equals(type)
                || TYPE_RECURRING_SKIPPED.equals(type)) {
            return recurringConfirmIntent(targetId);
        }

        if ("product".equals(targetType)) {
            return productDetailIntent(targetId, false);
        }

        if ("order".equals(targetType) || CATEGORY_ORDERS.equals(item.category)) {
            String orderId = resolveOrderId(item);
            if (!orderId.isEmpty()) {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, orderId);
                return intent;
            }
            return mainActivityIntent(R.id.nav_orders);
        }

        if ("fridge".equals(targetType)) {
            return new Intent(this, SmartFridgeActivity.class);
        }

        if ("cart".equals(targetType)) {
            return mainActivityIntent(R.id.nav_cart);
        }

        if (CATEGORY_OTHER.equals(item.category)) {
            return new Intent(this, CarbonPointsActivity.class);
        }
        return null;
    }

    private Intent recurringConfirmIntent(String targetId) {
        if (targetId == null || !targetId.contains("|")) {
            return new Intent(this, com.veggo.app.presentation.order.RecurringOrdersActivity.class);
        }
        String[] parts = targetId.split("\\|", 2);
        Intent intent = new Intent(this, RecurringConfirmOrderActivity.class);
        intent.putExtra(RecurringConfirmOrderActivity.EXTRA_RECURRING_ORDER_ID, parts[0]);
        intent.putExtra(RecurringConfirmOrderActivity.EXTRA_OCCURRENCE_DATE, parts[1]);
        return intent;
    }

    private Intent productDetailIntent(String productOrSku, boolean scrollToConsultation) {
        if (productOrSku == null || productOrSku.trim().isEmpty()) {
            return new Intent(this, SupportCustomersActivity.class);
        }
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productOrSku.trim());
        if (scrollToConsultation) {
            intent.putExtra(ProductDetailActivity.EXTRA_SCROLL_TO_CONSULTATION, true);
        }
        return intent;
    }

    private Intent mainActivityIntent(int navItemId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, navItemId);
        return intent;
    }

    private String resolveOrderId(PostNotificationItem item) {
        if (item.targetId != null && !item.targetId.trim().isEmpty()) {
            return item.targetId.trim();
        }
        Matcher matcher = ORDER_ID_PATTERN.matcher(item.body == null ? "" : item.body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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

    private void setTabSelected(int textId, int badgeId, boolean selected, int count) {
        TextView text = findViewById(textId);
        TextView badge = findViewById(badgeId);
        if (text != null) {
            text.setTextColor(getColor(selected ? R.color.primary_main : R.color.neutral_60));
            text.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (badge != null) {
            BadgeUiHelper.applyTabBadge(badge, count, selected);
        }
    }

    private String normalizeCategory(String category) {
        String value = safeLower(category);
        if (CATEGORY_COMMUNITY.equals(value) || CATEGORY_QA.equals(value) || CATEGORY_OTHER.equals(value)) {
            return value;
        }
        return CATEGORY_ORDERS;
    }

    private int notificationIcon(String category, String type, String title, String body) {
        if (CATEGORY_COMMUNITY.equals(normalizeCategory(category))) {
            return R.drawable.ic_profile_article;
        }
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
        final String type;
        final long timestamp;
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
            this(id, category, title, body, action, time, iconResId, remoteOrder, unread, "", "", "", System.currentTimeMillis());
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
            this(id, category, title, body, action, time, iconResId, remoteOrder, unread, targetType, targetId, "", System.currentTimeMillis());
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
                String targetId,
                long timestamp
        ) {
            this(id, category, title, body, action, time, iconResId, remoteOrder, unread, targetType, targetId, "", timestamp);
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
                String targetId,
                String type,
                long timestamp
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
            this.type = type == null ? "" : type;
            this.timestamp = timestamp;
        }
    }
}
