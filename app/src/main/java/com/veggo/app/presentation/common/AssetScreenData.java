package com.veggo.app.presentation.common;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.presentation.auth.UserAssetRepository;
import com.veggo.app.presentation.auth.model.User;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class AssetScreenData {
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    private static final String FALLBACK_CUSTOMER_ID = "CUS000006";
    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("vi", "VN"));

    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private AssetScreenData() {
    }

    public static Snapshot load(@NonNull Context context) {
        AssetRepository repository = new AssetRepository(context);
        List<AssetModels.User> users = mapUsers(new UserAssetRepository(context).getUsers());
        List<AssetModels.Order> orders = repository.getOrders();
        List<AssetModels.OrderDetail> details = repository.getOrderDetails();
        List<AssetModels.Certificate> certificates = repository.getCertificates();
        List<AssetModels.Warehouse> warehouses = repository.getWarehouses();
        List<AssetModels.CommunityPost> communityPosts = repository.getCommunityPosts();
        List<AssetModels.Reminder> reminders = repository.getReminders();
        List<AssetModels.Inventory> inventories = repository.getInventories();
        List<AssetModels.Instruction> instructions = repository.getInstructions();

        AppPreferences appPreferences = new AppPreferences(context);
        AssetModels.User user = findUser(users, orders, appPreferences.getCurrentPhone());

        // Ưu tiên thông tin từ Session nếu đã đăng nhập nhưng không có trong SQLite local
        if (appPreferences.isLoggedIn()) {
            String sessionPhone = appPreferences.getCurrentPhone();
            if (user == null || !safe(user.phone).equals(sessionPhone)) {
                user = new AssetModels.User();
                user.phone = sessionPhone;
                user.customerId = appPreferences.getCustomerId();
                user.fullName = appPreferences.getFullName();
                user.email = appPreferences.getEmail();
                user.carbonPoint = 0; // Khởi tạo mặc định cho user mới
            }
        }

        enrichUserFromOrderDetails(user, orders, details);

        String customerId = user == null ? FALLBACK_CUSTOMER_ID : user.customerId;
        List<AssetModels.Order> customerOrders = new ArrayList<>();
        for (AssetModels.Order order : orders) {
            if (equals(order.customerId, customerId)) {
                customerOrders.add(order);
            }
        }
        customerOrders.sort((left, right) -> compareDates(right.createdAt, left.createdAt));

        Map<String, AssetModels.OrderDetail> detailByOrderId = new HashMap<>();
        for (AssetModels.OrderDetail detail : details) {
            detailByOrderId.put(detail.orderId, detail);
        }

        Map<String, AssetModels.Warehouse> warehouseById = new HashMap<>();
        for (AssetModels.Warehouse warehouse : warehouses) {
            warehouseById.put(warehouse.warehouseId, warehouse);
        }

        certificates.sort(Comparator.comparingInt(certificate -> certificate.requiredCarbonPoint));
        return new Snapshot(
                customerId,
                user,
                customerOrders,
                detailByOrderId,
                certificates,
                warehouseById,
                communityPosts,
                reminders,
                inventories,
                instructions
        );
    }

    @NonNull
    private static List<AssetModels.User> mapUsers(@NonNull List<User> sourceUsers) {
        List<AssetModels.User> mappedUsers = new ArrayList<>();
        for (User sourceUser : sourceUsers) {
            AssetModels.User mappedUser = new AssetModels.User();
            mappedUser.objectId = sourceUser.getObjectId();
            mappedUser.customerId = sourceUser.getCustomerId();
            mappedUser.phone = sourceUser.getPhone();
            mappedUser.password = sourceUser.getPassword();
            mappedUser.fullName = sourceUser.getFullName();
            mappedUser.email = sourceUser.getEmail();
            mappedUser.address = sourceUser.getAddress();
            mappedUser.carbonPoint = sourceUser.getCarbonPoint();
            mappedUsers.add(mappedUser);
        }
        return mappedUsers;
    }

    @Nullable
    private static AssetModels.User findUser(
            @NonNull List<AssetModels.User> users,
            @NonNull List<AssetModels.Order> orders,
            @Nullable String currentPhone
    ) {
        if (hasText(currentPhone)) {
            for (AssetModels.User user : users) {
                if (equals(user.phone, currentPhone)) {
                    return user;
                }
            }
            // Nếu đã truyền phone nhưng không tìm thấy trong list, 
            // trả về null để hàm load() xử lý tạo user từ session
            return null; 
        }

        // Chỉ fallback cho Guest Mode (Chưa đăng nhập)
        for (AssetModels.User user : users) {
            if (equals(user.customerId, FALLBACK_CUSTOMER_ID)) {
                return user;
            }
        }
        for (AssetModels.User user : users) {
            for (AssetModels.Order order : orders) {
                if (equals(user.customerId, order.customerId)) {
                    return user;
                }
            }
        }
        return users.isEmpty() ? null : users.get(0);
    }

    private static void enrichUserFromOrderDetails(
            @Nullable AssetModels.User user,
            @NonNull List<AssetModels.Order> orders,
            @NonNull List<AssetModels.OrderDetail> details
    ) {
        if (user == null) {
            return;
        }

        Map<String, AssetModels.OrderDetail> detailByOrderId = new HashMap<>();
        for (AssetModels.OrderDetail detail : details) {
            detailByOrderId.put(detail.orderId, detail);
        }

        for (AssetModels.Order order : orders) {
            if (!equals(order.customerId, user.customerId)) {
                continue;
            }
            AssetModels.OrderDetail detail = detailByOrderId.get(order.orderId);
            if (detail == null || detail.shippingInfo == null) {
                continue;
            }
            AssetModels.ShippingInfo shippingInfo = detail.shippingInfo;
            if (!hasText(user.fullName) && hasText(shippingInfo.fullName)) {
                user.fullName = shippingInfo.fullName;
            }
            if (!hasText(user.phone) && hasText(shippingInfo.phone)) {
                user.phone = shippingInfo.phone;
            }
            if (!hasText(user.email) && hasText(shippingInfo.email)) {
                user.email = shippingInfo.email;
            }
            if (!hasText(user.address) && shippingInfo.address != null) {
                user.address = fullAddress(shippingInfo);
            }
            if (hasText(user.fullName) && hasText(user.phone) && hasText(user.address)) {
                return;
            }
        }
    }

    public static List<AssetModels.Order> filterOrders(
            @NonNull Snapshot snapshot,
            @Nullable String status
    ) {
        if (status == null) {
            return snapshot.orders;
        }
        List<AssetModels.Order> filtered = new ArrayList<>();
        for (AssetModels.Order order : snapshot.orders) {
            if (equals(order.status, status)) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    public static List<AssetModels.Order> filterReturnOrders(
            @NonNull Snapshot snapshot,
            @NonNull String bucket
    ) {
        List<AssetModels.Order> filtered = new ArrayList<>();
        for (AssetModels.Order order : snapshot.orders) {
            String status = safe(order.status);
            if ("pending".equals(bucket) && hasText(order.returnReason) && "completed".equals(status)) {
                filtered.add(order);
            } else if ("processing".equals(bucket) && ("processing_return".equals(status) || "returning".equals(status))) {
                filtered.add(order);
            } else if ("completed".equals(bucket) && "returned".equals(status)) {
                filtered.add(order);
            } else if ("rejected".equals(bucket) && "cancelled".equals(status)) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    public static List<AssetModels.Order> filterReviewOrders(
            @NonNull Snapshot snapshot,
            boolean done
    ) {
        List<AssetModels.Order> source = filterOrders(snapshot, "completed");
        List<AssetModels.Order> filtered = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            if (done == (i % 2 == 1)) {
                filtered.add(source.get(i));
            }
        }
        return filtered;
    }

    public static List<AssetModels.CommunityPost> postsForCurrentUser(@NonNull Snapshot snapshot) {
        List<AssetModels.CommunityPost> posts = new ArrayList<>();
        for (AssetModels.CommunityPost post : snapshot.communityPosts) {
            if (equals(post.customerId, snapshot.customerId)) {
                posts.add(post);
            }
        }
        posts.sort((left, right) -> safe(right.publishDate).compareTo(safe(left.publishDate)));
        return posts;
    }

    public static List<AssetModels.Reminder> remindersForCurrentUser(@NonNull Snapshot snapshot) {
        List<AssetModels.Reminder> reminders = new ArrayList<>();
        for (AssetModels.Reminder reminder : snapshot.reminders) {
            if (equals(reminder.customerId, snapshot.customerId)) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    public static List<AssetModels.Inventory> availableInventories(@NonNull Snapshot snapshot) {
        List<AssetModels.Inventory> result = new ArrayList<>();
        for (AssetModels.Inventory inventory : snapshot.inventories) {
            result.add(inventory);
            if (result.size() >= 8) {
                break;
            }
        }
        return result;
    }

    public static List<AssetModels.Instruction> recipeSuggestions(@NonNull Snapshot snapshot) {
        List<AssetModels.Instruction> suggestions = new ArrayList<>();
        for (AssetModels.Instruction instruction : snapshot.instructions) {
            if (hasText(instruction.dishName)) {
                suggestions.add(instruction);
            }
            if (suggestions.size() >= 6) {
                break;
            }
        }
        return suggestions;
    }

    public static void bindOrderCard(
            @NonNull Context context,
            @NonNull View card,
            @NonNull Snapshot snapshot,
            @NonNull AssetModels.Order order
    ) {
        setText(card, R.id.orderPendingItemCode, order.orderId);
        setText(card, R.id.orderShippingItemCode, order.orderId);
        setText(card, R.id.orderDeliveredItemCode, order.orderId);
        setText(card, R.id.orderCancelledItemCode, order.orderId);
        setText(card, R.id.returnOrderCode, order.orderId);
        setText(card, R.id.reviewOrderCode, order.orderId);

        setText(card, R.id.orderPendingItemStatus, statusLabel(order.status));
        setText(card, R.id.orderShippingItemStatus, statusLabel(order.status));
        setText(card, R.id.orderDeliveredItemStatus, statusLabel(order.status));
        setText(card, R.id.orderCancelledItemStatus, statusLabel(order.status));
        setText(card, R.id.returnOrderStatus, statusLabel(order.status));
        setText(card, R.id.reviewOrderStatus, statusLabel(order.status));

        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
        bindProductBlock(context, card, detail, order);
    }

    public static void bindProductBlock(
            @NonNull Context context,
            @NonNull View root,
            @Nullable AssetModels.OrderDetail detail,
            @NonNull AssetModels.Order order
    ) {
        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            return;
        }
        AssetModels.OrderDetailItem item = detail.items.get(0);
        setText(root, R.id.orderItemProductName, item.productName);
        setText(root, R.id.orderItemVariant, item.unit);
        setText(root, R.id.orderItemQuantity, "x" + item.quantity);
        setText(root, R.id.orderItemOldPrice, item.originalPrice > item.price ? money(item.originalPrice) : "");
        setText(root, R.id.orderItemPrice, money(item.price));
        setText(root, R.id.orderItemTotal, money(Math.round(order.totalAmount)));
        ImageView image = root.findViewById(R.id.orderItemProductImage);
        if (image != null && hasText(item.image)) {
            Glide.with(context).load(item.image).placeholder(R.drawable.ic_vegetable).into(image);
        }
    }

    public static void bindDetailProduct(
            @NonNull Context context,
            @NonNull View row,
            @NonNull AssetModels.OrderDetailItem item
    ) {
        setText(row, R.id.orderDetailProductName, item.productName);
        setText(row, R.id.orderDetailProductVariant, item.unit);
        setText(row, R.id.orderDetailProductQuantity, "Số lượng: " + item.quantity);
        setText(row, R.id.orderDetailProductPrice, money(item.price));
        setText(row, R.id.orderDetailProductTotal, money(item.price * item.quantity));
        ImageView image = row.findViewById(R.id.orderDetailProductImage);
        if (image != null && hasText(item.image)) {
            Glide.with(context).load(item.image).placeholder(R.drawable.ic_vegetable).into(image);
        }
    }

    public static void setText(@NonNull View root, int id, @Nullable String value) {
        TextView view = root.findViewById(id);
        if (view != null) {
            view.setText(value == null ? "" : value);
        }
    }

    public static String money(long value) {
        return VND_FORMAT.format(value) + "đ";
    }

    public static String date(@Nullable AssetModels.MongoDate mongoDate) {
        if (mongoDate == null || !hasText(mongoDate.date)) {
            return "";
        }
        try {
            return DATE_FORMATTER.format(ISO_FORMAT.parse(mongoDate.date));
        } catch (ParseException exception) {
            return mongoDate.date;
        }
    }

    public static String dateText(@Nullable String value) {
        if (!hasText(value)) {
            return "";
        }
        try {
            return DATE_FORMATTER.format(ISO_FORMAT.parse(value));
        } catch (ParseException exception) {
            return value.length() >= 10 ? value.substring(0, 10) : value;
        }
    }

    public static String statusLabel(@Nullable String status) {
        switch (safe(status)) {
            case "pending":
                return "Chờ xác nhận";
            case "shipping":
                return "Đang giao";
            case "delivered":
                return "Đã giao";
            case "completed":
                return "Hoàn tất";
            case "cancelled":
                return "Đã hủy";
            case "returned":
                return "Đã hoàn trả";
            case "returning":
            case "processing_return":
                return "Đang đổi trả";
            default:
                return safe(status);
        }
    }

    public static String paymentLabel(@Nullable String paymentMethod) {
        if ("cod".equalsIgnoreCase(paymentMethod)) {
            return "Thanh toán khi nhận hàng";
        }
        return hasText(paymentMethod) ? paymentMethod : "Chưa cập nhật";
    }

    public static String fullAddress(@Nullable AssetModels.ShippingInfo shippingInfo) {
        if (shippingInfo == null || shippingInfo.address == null) {
            return "";
        }
        AssetModels.ShippingAddress address = shippingInfo.address;
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, address.detail);
        addIfPresent(parts, address.ward);
        addIfPresent(parts, address.district);
        addIfPresent(parts, address.city);
        return String.join(", ", parts);
    }

    public static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    public static boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static int compareDates(@Nullable AssetModels.MongoDate left, @Nullable AssetModels.MongoDate right) {
        return safe(left == null ? null : left.date).compareTo(safe(right == null ? null : right.date));
    }

    private static boolean equals(@Nullable String left, @Nullable String right) {
        return safe(left).equals(safe(right));
    }

    private static void addIfPresent(@NonNull List<String> parts, @Nullable String value) {
        if (hasText(value)) {
            parts.add(value);
        }
    }

    public static final class Snapshot {
        @NonNull
        public final String customerId;
        @Nullable
        public final AssetModels.User user;
        @NonNull
        public final List<AssetModels.Order> orders;
        @NonNull
        public final Map<String, AssetModels.OrderDetail> detailByOrderId;
        @NonNull
        public final List<AssetModels.Certificate> certificates;
        @NonNull
        public final Map<String, AssetModels.Warehouse> warehouseById;
        @NonNull
        public final List<AssetModels.CommunityPost> communityPosts;
        @NonNull
        public final List<AssetModels.Reminder> reminders;
        @NonNull
        public final List<AssetModels.Inventory> inventories;
        @NonNull
        public final List<AssetModels.Instruction> instructions;

        private Snapshot(
                @NonNull String customerId,
                @Nullable AssetModels.User user,
                @NonNull List<AssetModels.Order> orders,
                @NonNull Map<String, AssetModels.OrderDetail> detailByOrderId,
                @NonNull List<AssetModels.Certificate> certificates,
                @NonNull Map<String, AssetModels.Warehouse> warehouseById,
                @NonNull List<AssetModels.CommunityPost> communityPosts,
                @NonNull List<AssetModels.Reminder> reminders,
                @NonNull List<AssetModels.Inventory> inventories,
                @NonNull List<AssetModels.Instruction> instructions
        ) {
            this.customerId = customerId;
            this.user = user;
            this.orders = Collections.unmodifiableList(orders);
            this.detailByOrderId = detailByOrderId;
            this.certificates = Collections.unmodifiableList(certificates);
            this.warehouseById = warehouseById;
            this.communityPosts = Collections.unmodifiableList(communityPosts);
            this.reminders = Collections.unmodifiableList(reminders);
            this.inventories = Collections.unmodifiableList(inventories);
            this.instructions = Collections.unmodifiableList(instructions);
        }
    }
}
