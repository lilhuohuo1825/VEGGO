package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.CarbonHistoryReadState;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.remote.api.CertificateApi;
import com.veggo.app.data.remote.api.ReviewApi;
import com.veggo.app.data.remote.api.WalletApi;
import com.veggo.app.data.remote.dto.WalletTransactionDto;
import com.veggo.app.data.remote.dto.WalletTransactionsResponseDto;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.order.OrderDetailActivity;
import com.veggo.app.presentation.order.ReviewOrderActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

public class CarbonHistoryActivity extends BaseActivity {
    private TextView ordersTabText;
    private TextView reviewsTabText;
    private TextView redeemedTabText;
    private View ordersIndicator;
    private View reviewsIndicator;
    private View redeemedIndicator;
    private View ordersList;
    private View reviewsList;
    private View redeemedList;
    private SwipeRefreshLayout carbonHistoryRefreshLayout;
    private final List<String> latestItemIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "điểm carbon")) {
            return;
        }
        setContentView(R.layout.activity_carbon_history);
        findViewById(R.id.carbonHistoryBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.carbonHistoryMarkAllRead).setOnClickListener(v -> markAllRead());

        ordersTabText = findViewById(R.id.carbonTabAllText);
        reviewsTabText = findViewById(R.id.carbonTabReceivedText);
        redeemedTabText = findViewById(R.id.carbonTabRedeemedText);
        ordersIndicator = findViewById(R.id.carbonTabAllIndicator);
        reviewsIndicator = findViewById(R.id.carbonTabReceivedIndicator);
        redeemedIndicator = findViewById(R.id.carbonTabRedeemedIndicator);
        ordersList = findViewById(R.id.carbonHistoryAllList);
        reviewsList = findViewById(R.id.carbonHistoryReceivedList);
        redeemedList = findViewById(R.id.carbonHistoryRedeemedList);

        findViewById(R.id.carbonTabAll).setOnClickListener(v -> showState(ordersTabText, ordersIndicator, ordersList));
        findViewById(R.id.carbonTabReceived).setOnClickListener(v -> showState(reviewsTabText, reviewsIndicator, reviewsList));
        findViewById(R.id.carbonTabRedeemed).setOnClickListener(v -> showState(redeemedTabText, redeemedIndicator, redeemedList));
        showState(ordersTabText, ordersIndicator, ordersList);
        setupPullToRefresh();
        loadHistory();
    }

    private void setupPullToRefresh() {
        carbonHistoryRefreshLayout = PullToRefreshHelper.wrap(
                findViewById(R.id.carbonHistoryScroll),
                this::loadHistory
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void showState(TextView activeText, View activeIndicator, View activeList) {
        setActive(ordersTabText, ordersIndicator, ordersTabText == activeText);
        setActive(reviewsTabText, reviewsIndicator, reviewsTabText == activeText);
        setActive(redeemedTabText, redeemedIndicator, redeemedTabText == activeText);

        ordersList.setVisibility(ordersList == activeList ? View.VISIBLE : View.GONE);
        reviewsList.setVisibility(reviewsList == activeList ? View.VISIBLE : View.GONE);
        redeemedList.setVisibility(redeemedList == activeList ? View.VISIBLE : View.GONE);
    }

    private void setActive(TextView textView, View indicator, boolean active) {
        textView.setTextColor(ContextCompat.getColor(this, active ? R.color.primary_main : R.color.neutral_60));
        textView.setTypeface(textView.getTypeface(), active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadHistory() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            String customerId = new AppPreferences(this).getCustomerId();
            List<ReviewCarbonEntry> reviewEntries = loadReviewCarbonEntries(snapshot, customerId);
            List<WalletTransactionDto> walletTransactions = loadWalletTransactions(customerId);
            runOnUiThread(() -> {
                bindHistory(snapshot, reviewEntries, walletTransactions);
                PullToRefreshHelper.finish(carbonHistoryRefreshLayout);
            });
        }).start();
    }

    private void bindHistory(AssetScreenData.Snapshot snapshot, List<ReviewCarbonEntry> reviewEntries,
                             List<WalletTransactionDto> walletTransactions) {
        List<String> allItemIds = new ArrayList<>();
        for (AssetModels.Order order : snapshot.orders) {
            if (!isOrderCarbonEarned(order)) {
                continue;
            }
            AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
            if (purchaseCarbonPoints(detail) > 0) {
                allItemIds.add(orderItemId(order.orderId));
            }
        }
        for (ReviewCarbonEntry entry : reviewEntries) {
            allItemIds.add(entry.itemId);
        }
        List<RedeemedEvent> redeemedEvents = collectRedeemedEvents(snapshot, walletTransactions);
        for (RedeemedEvent event : redeemedEvents) {
            allItemIds.add(event.eventKey);
        }
        CarbonHistoryReadState.ensureBaselineIfNeeded(this, allItemIds);
        latestItemIds.clear();
        latestItemIds.addAll(allItemIds);

        bindOrderList((LinearLayout) ordersList, snapshot);
        bindReviewList((LinearLayout) reviewsList, reviewEntries);
        bindRedeemedList((LinearLayout) redeemedList, redeemedEvents);
    }

    private void bindOrderList(LinearLayout container, AssetScreenData.Snapshot snapshot) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<AssetModels.Order> orders = new ArrayList<>(snapshot.orders);
        Collections.sort(orders, (left, right) -> Long.compare(
                parseTimestamp(right.createdAt == null ? null : right.createdAt.date),
                parseTimestamp(left.createdAt == null ? null : left.createdAt.date)
        ));

        for (AssetModels.Order order : orders) {
            if (!isOrderCarbonEarned(order)) {
                continue;
            }
            AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
            int points = purchaseCarbonPoints(detail);
            if (points <= 0) {
                continue;
            }
            View item = inflater.inflate(R.layout.item_carbon_history_earned_order, container, false);
            String itemId = orderItemId(order.orderId);
            applyReadState(item, itemId);
            setIcon(item, R.drawable.ic_order_delivered_box);
            AssetScreenData.setText(item, R.id.carbonHistoryTitle, "Mua hàng xanh\n#" + order.orderId);
            AssetScreenData.setText(item, R.id.carbonHistoryDate, AssetScreenData.date(order.createdAt));
            AssetScreenData.setText(item, R.id.carbonHistoryPoints, "+" + points + " C");
            item.setOnClickListener(v -> {
                CarbonHistoryReadState.markRead(this, itemId);
                applyReadState(item, itemId);
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, order.orderId);
                startActivity(intent);
            });
            container.addView(item);
        }
        if (container.getChildCount() == 0) {
            container.addView(createEmptyState("Bạn chưa có điểm carbon từ đơn hàng."));
        }
    }

    private void bindReviewList(LinearLayout container, List<ReviewCarbonEntry> reviewEntries) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ReviewCarbonEntry entry : reviewEntries) {
            View item = inflater.inflate(R.layout.item_carbon_history_earned_review, container, false);
            applyReadState(item, entry.itemId);
            setIcon(item, R.drawable.ic_order_review_option);
            AssetScreenData.setText(item, R.id.carbonHistoryTitle, entry.title);
            AssetScreenData.setText(item, R.id.carbonHistoryDate, entry.dateText);
            AssetScreenData.setText(item, R.id.carbonHistoryPoints, "+" + entry.points + " C");
            item.setOnClickListener(v -> {
                CarbonHistoryReadState.markRead(this, entry.itemId);
                applyReadState(item, entry.itemId);
                Intent intent = new Intent(this, ReviewOrderActivity.class);
                intent.putExtra(AssetScreenData.EXTRA_ORDER_ID, entry.orderId);
                startActivity(intent);
            });
            container.addView(item);
        }
        if (container.getChildCount() == 0) {
            container.addView(createEmptyState("Bạn chưa có điểm carbon từ đánh giá."));
        }
    }

    private void bindRedeemedList(LinearLayout container, List<RedeemedEvent> events) {
        container.removeAllViews();
        Collections.sort(events, Comparator.comparingLong((RedeemedEvent event) -> event.timestamp).reversed());

        LayoutInflater inflater = LayoutInflater.from(this);
        for (RedeemedEvent event : events) {
            View item = inflater.inflate(R.layout.item_carbon_history_redeemed, container, false);
            applyReadState(item, event.eventKey);
            setIcon(item, R.drawable.ic_voucher);
            AssetScreenData.setText(item, R.id.carbonHistoryTitle, event.title);
            AssetScreenData.setText(item, R.id.carbonHistoryDate, event.dateText);
            TextView pointsView = item.findViewById(R.id.carbonHistoryPoints);
            if (pointsView != null) {
                pointsView.setText(event.statusText);
                pointsView.setTextColor(ContextCompat.getColor(this, event.statusColorRes));
            }
            item.setOnClickListener(v -> {
                CarbonHistoryReadState.markRead(this, event.eventKey);
                applyReadState(item, event.eventKey);
            });
            container.addView(item);
        }

        if (container.getChildCount() == 0) {
            container.addView(createEmptyState("Bạn chưa có lịch sử đổi điểm carbon."));
        }
    }

    private List<ReviewCarbonEntry> loadReviewCarbonEntries(
            AssetScreenData.Snapshot snapshot,
            String customerId
    ) {
        List<ReviewCarbonEntry> entries = new ArrayList<>();
        if (customerId == null || customerId.trim().isEmpty()) {
            return entries;
        }

        List<AssetModels.Order> reviewedOrders = AssetScreenData.filterReviewOrders(snapshot, true);
        ReviewApi reviewApi = ApiClient.createService(ReviewApi.class);
        for (AssetModels.Order order : reviewedOrders) {
            try {
                Response<Map<String, Object>> response = reviewApi
                        .getOrderReviews(order.orderId, customerId)
                        .execute();
                if (!response.isSuccessful() || response.body() == null) {
                    continue;
                }
                entries.addAll(buildReviewEntriesForOrder(snapshot, order, response.body()));
            } catch (Exception ignored) {
            }
        }

        Collections.sort(entries, Comparator.comparingLong(entry -> entry.timestamp));
        Collections.reverse(entries);
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<ReviewCarbonEntry> buildReviewEntriesForOrder(
            AssetScreenData.Snapshot snapshot,
            AssetModels.Order order,
            Map<String, Object> body
    ) {
        List<ReviewCarbonEntry> entries = new ArrayList<>();
        Object reviewsBySku = body.get("reviewsBySku");
        if (!(reviewsBySku instanceof Map)) {
            return entries;
        }
        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
        for (java.util.Map.Entry<String, Object> entry : ((Map<String, Object>) reviewsBySku).entrySet()) {
            String sku = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> review = (Map<String, Object>) value;
            int points = Math.min(2, extractCarbonPoints(review));
            if (points <= 0) {
                continue;
            }
            String time = String.valueOf(review.get("time"));
            long timestamp = parseTimestamp(time);
            String dateText = time != null && !"null".equals(time)
                    ? AssetScreenData.dateText(time)
                    : AssetScreenData.date(order.createdAt);
            String productName = resolveOrderProductName(detail, sku);
            entries.add(new ReviewCarbonEntry(
                    "review:" + order.orderId + ":" + sku,
                    order.orderId,
                    "Đánh giá " + productName + "\n#" + order.orderId,
                    dateText,
                    points,
                    timestamp
            ));
        }
        return entries;
    }

    private String resolveOrderProductName(@Nullable AssetModels.OrderDetail detail, @Nullable String sku) {
        if (detail != null && detail.items != null && sku != null) {
            for (AssetModels.OrderDetailItem item : detail.items) {
                if (item == null) continue;
                if (sku.equals(String.valueOf(item.sku))) {
                    String name = item.productName;
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }
        return "sản phẩm";
    }

    @SuppressWarnings("unchecked")
    private int sumReviewCarbonPoints(Map<String, Object> body) {
        Object reviewsBySku = body.get("reviewsBySku");
        if (!(reviewsBySku instanceof Map)) {
            return 0;
        }
        int total = 0;
        for (Object value : ((Map<String, Object>) reviewsBySku).values()) {
            if (value instanceof Map) {
                total += Math.min(2, extractCarbonPoints((Map<String, Object>) value));
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private String reviewDateText(Map<String, Object> body, AssetModels.Order order) {
        Object reviewsBySku = body.get("reviewsBySku");
        if (reviewsBySku instanceof Map) {
            for (Object value : ((Map<String, Object>) reviewsBySku).values()) {
                if (value instanceof Map) {
                    Object time = ((Map<String, Object>) value).get("time");
                    if (time != null) {
                        return AssetScreenData.dateText(String.valueOf(time));
                    }
                }
            }
        }
        return AssetScreenData.date(order.createdAt);
    }

    @SuppressWarnings("unchecked")
    private String reviewTimestamp(Map<String, Object> body, AssetModels.Order order) {
        Object reviewsBySku = body.get("reviewsBySku");
        if (reviewsBySku instanceof Map) {
            for (Object value : ((Map<String, Object>) reviewsBySku).values()) {
                if (value instanceof Map) {
                    Object time = ((Map<String, Object>) value).get("time");
                    if (time != null) {
                        return String.valueOf(time);
                    }
                }
            }
        }
        return order.createdAt == null ? null : order.createdAt.date;
    }

    private String reviewProductLabel(AssetScreenData.Snapshot snapshot, String orderId) {
        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(orderId);
        if (detail != null && detail.items != null && !detail.items.isEmpty()) {
            String name = detail.items.get(0).productName;
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
        }
        return "sản phẩm";
    }

    private int extractCarbonPoints(Map<String, Object> review) {
        Object value = review.get("carbonPoints");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void setIcon(View item, int iconResId) {
        ImageView icon = item.findViewById(R.id.carbonHistoryIcon);
        if (icon != null) {
            icon.setImageResource(iconResId);
        }
    }

    private List<RedeemedEvent> collectRedeemedEvents(AssetScreenData.Snapshot snapshot,
                                                      List<WalletTransactionDto> walletTransactions) {
        List<RedeemedEvent> events = new ArrayList<>();
        int points = snapshot.user == null ? 0 : snapshot.user.carbonPoint;
        String approvedCertificateId = snapshot.user == null ? null : snapshot.user.certificateId;
        CertificateApi.CertificateRequestDto request = snapshot.latestCertificateRequest;
        Set<String> addedKeys = new HashSet<>();

        if (approvedCertificateId != null && !approvedCertificateId.trim().isEmpty()) {
            AssetModels.Certificate approved = findCertificateById(snapshot.certificates, approvedCertificateId);
            if (approved != null) {
                String dateText = request != null && "approved".equalsIgnoreCase(request.status)
                        ? formatRequestDate(request.reviewedAt, request.createdAt)
                        : "Đã nhận";
                addRedeemedEvent(events, addedKeys,
                        "approved:" + approved.certificateId,
                        "Nhận ưu đãi\n" + approved.certificateName,
                        dateText,
                        "Đã nhận",
                        R.color.primary_hover,
                        parseTimestamp(request != null ? firstNonBlank(request.reviewedAt, request.createdAt) : null));
            }
        }

        if (request != null && "pending".equalsIgnoreCase(request.status)) {
            String certificateName = firstNonBlank(
                    request.getRequestedCertificateName(),
                    "chứng nhận"
            );
            addRedeemedEvent(events, addedKeys,
                    "pending:" + request.getRequestedCertificateId(),
                    "Chờ duyệt ưu đãi\n" + certificateName,
                    formatRequestDate(request.createdAt, null),
                    "Chờ duyệt",
                    R.color.secondary_hover,
                    parseTimestamp(request.createdAt));
        }

        for (AssetModels.Certificate certificate : snapshot.certificates) {
            if (certificate == null || points < certificate.requiredCarbonPoint) {
                continue;
            }
            if (approvedCertificateId != null && approvedCertificateId.equals(certificate.certificateId)) {
                continue;
            }
            if (request != null
                    && certificate.certificateId != null
                    && certificate.certificateId.equals(request.getRequestedCertificateId())) {
                continue;
            }
            addRedeemedEvent(events, addedKeys,
                    "milestone:" + certificate.certificateId,
                    "Đạt mốc đổi ưu đãi\n" + certificate.certificateName,
                    "Từ " + certificate.requiredCarbonPoint + " C",
                    "Đủ mốc",
                    R.color.info_main,
                    0L);
        }

        for (AssetModels.Order order : snapshot.orders) {
            for (String promoCode : collectCertificatePromoCodes(snapshot, order)) {
                String offerName = resolveOfferName(promoCode, snapshot.certificates);
                addRedeemedEvent(events, addedKeys,
                        "used:" + order.orderId + ":" + promoCode,
                        "Sử dụng ưu đãi\n" + offerName,
                        AssetScreenData.date(order.createdAt),
                        "Đã dùng",
                        R.color.primary_hover,
                        parseTimestamp(order.createdAt == null ? null : order.createdAt.date));
            }
        }

        // VeggoPay: watering tree consumes carbon points.
        if (walletTransactions != null) {
            for (WalletTransactionDto tx : walletTransactions) {
                if (tx == null) continue;
                if (!"carbon_watering".equalsIgnoreCase(tx.getType())) continue;
                int carbon = tx.getCarbonPoints() != null ? tx.getCarbonPoints() : 0;
                if (carbon == 0) {
                    // Fallback: assume -5C if server did not include carbonPoints.
                    carbon = -5;
                }
                String dateText = tx.getCreatedAt() != null ? AssetScreenData.dateText(tx.getCreatedAt()) : "";
                addRedeemedEvent(events, addedKeys,
                        "water:" + (tx.getTransactionId() == null ? "" : tx.getTransactionId()),
                        "Tưới nước cho cây\nVeggoPay",
                        dateText,
                        (carbon < 0 ? String.valueOf(carbon) : "-" + carbon) + " C",
                        R.color.danger_main,
                        parseTimestamp(tx.getCreatedAt()));
            }
        }

        return events;
    }

    private List<WalletTransactionDto> loadWalletTransactions(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            WalletApi walletApi = ApiClient.createService(WalletApi.class);
            Response<WalletTransactionsResponseDto> response = walletApi
                    .getTransactions(customerId, "carbon_watering")
                    .execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                    && response.body().getData() != null) {
                return response.body().getData();
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private List<String> collectCertificatePromoCodes(
            AssetScreenData.Snapshot snapshot,
            AssetModels.Order order
    ) {
        List<String> promoCodes = new ArrayList<>();
        addCertificatePromoCode(promoCodes, order.code);
        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
        if (detail != null) {
            addCertificatePromoCode(promoCodes, detail.promotionId);
            addCertificatePromoCode(promoCodes, detail.shippingPromotionId);
        }
        return promoCodes;
    }

    private void addCertificatePromoCode(List<String> promoCodes, @Nullable String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return;
        }
        String promoCode = rawCode.trim().toUpperCase(Locale.US);
        if (!isCertificatePromotionCode(promoCode) || promoCodes.contains(promoCode)) {
            return;
        }
        promoCodes.add(promoCode);
    }

    private void addRedeemedEvent(
            List<RedeemedEvent> events,
            Set<String> addedKeys,
            String key,
            String title,
            String dateText,
            String statusText,
            int statusColorRes,
            long timestamp
    ) {
        if (!addedKeys.add(key)) {
            return;
        }
        events.add(new RedeemedEvent(key, title, dateText, statusText, statusColorRes, timestamp));
    }

    private AssetModels.Certificate findCertificateById(List<AssetModels.Certificate> certificates, String certificateId) {
        if (certificateId == null) {
            return null;
        }
        for (AssetModels.Certificate certificate : certificates) {
            if (certificateId.equals(certificate.certificateId)) {
                return certificate;
            }
        }
        return null;
    }

    private String resolveOfferName(String promoCode, List<AssetModels.Certificate> certificates) {
        String certificateId = promoCodeToCertificateId(promoCode);
        AssetModels.Certificate certificate = findCertificateById(certificates, certificateId);
        if (certificate != null && certificate.certificateName != null && !certificate.certificateName.isEmpty()) {
            return certificate.certificateName;
        }
        return promoCode;
    }

    private String promoCodeToCertificateId(String promoCode) {
        if (promoCode == null) {
            return null;
        }
        switch (promoCode.toUpperCase(Locale.US)) {
            case "PROMO017": return "CER001";
            case "PROMO018": return "CER002";
            case "PROMO019": return "CER003";
            case "PROMO020": return "CER004";
            case "PROMO021": return "CER005";
            default:
                return promoCode.startsWith("CER") ? promoCode : null;
        }
    }

    private boolean isCertificatePromotionCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.matches("CER00[1-5]") || code.matches("PROMO01[7-9]|PROMO02[0-1]");
    }

    private String formatRequestDate(String primary, String fallback) {
        String value = firstNonBlank(primary, fallback);
        if (value == null || value.isEmpty()) {
            return "Gần đây";
        }
        return AssetScreenData.dateText(value);
    }

    private long parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    .parse(value.trim())
                    .getTime();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return "";
    }

    private int purchaseCarbonPoints(AssetModels.OrderDetail detail) {
        if (detail == null) return 0;
        if (detail.carbonPointEarned > 0) return detail.carbonPointEarned;
        int total = 0;
        if (detail.items != null) {
            for (AssetModels.OrderDetailItem item : detail.items) {
                total += item.carbonPointEarned;
            }
        }
        return total;
    }

    private boolean isOrderCarbonEarned(@Nullable AssetModels.Order order) {
        if (order == null || order.status == null) {
            return false;
        }
        String status = order.status.trim().toLowerCase(Locale.US);
        return "completed".equals(status)
                || "unreview".equals(status)
                || "reviewed".equals(status);
    }

    private TextView createEmptyState(String message) {
        TextView emptyView = new TextView(this);
        emptyView.setText(message);
        emptyView.setTextColor(ContextCompat.getColor(this, R.color.neutral_60));
        emptyView.setTextSize(14);
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(dp(16), dp(24), dp(16), dp(24));
        emptyView.setBackgroundResource(R.drawable.bg_profile_card);
        emptyView.setFontFeatureSettings("kern");
        return emptyView;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyReadState(View item, String itemId) {
        boolean unread = CarbonHistoryReadState.isUnread(this, itemId);
        item.setBackgroundResource(unread
                ? R.drawable.bg_carbon_history_item_unread
                : R.drawable.bg_profile_card);
    }

    private void markAllRead() {
        CarbonHistoryReadState.markAllRead(this, latestItemIds);
        loadHistory();
    }

    private static String orderItemId(String orderId) {
        return "order:" + orderId;
    }

    private static class ReviewCarbonEntry {
        final String itemId;
        final String orderId;
        final String title;
        final String dateText;
        final int points;
        final long timestamp;

        ReviewCarbonEntry(String itemId, String orderId, String title, String dateText, int points, long timestamp) {
            this.itemId = itemId;
            this.orderId = orderId;
            this.title = title;
            this.dateText = dateText;
            this.points = points;
            this.timestamp = timestamp;
        }
    }

    private static class RedeemedEvent {
        final String eventKey;
        final String title;
        final String dateText;
        final String statusText;
        final int statusColorRes;
        final long timestamp;

        RedeemedEvent(String eventKey, String title, String dateText, String statusText, int statusColorRes, long timestamp) {
            this.eventKey = eventKey;
            this.title = title;
            this.dateText = dateText;
            this.statusText = statusText;
            this.statusColorRes = statusColorRes;
            this.timestamp = timestamp;
        }
    }
}
