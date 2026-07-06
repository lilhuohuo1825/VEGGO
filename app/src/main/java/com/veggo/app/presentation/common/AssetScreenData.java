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
import com.veggo.app.presentation.profile.LoginRequiredActivity;
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
    private static final List<AssetModels.Order> guestOrderCache = new ArrayList<>();
    private static final Map<String, AssetModels.OrderDetail> guestOrderDetailCache = new HashMap<>();

    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private AssetScreenData() {
    }

    public static Snapshot load(@NonNull Context context) {
        AppPreferences appPreferences = new AppPreferences(context);
        
        AssetModels.User user = null;
        List<AssetModels.Order> customerOrders = new ArrayList<>();
        Map<String, AssetModels.OrderDetail> detailByOrderId = new HashMap<>();
        com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto latestCertificateRequest = null;

        AssetRepository repository = new AssetRepository(context);
        List<AssetModels.Certificate> certificates = loadCertificates(repository);
        List<AssetModels.Warehouse> warehouses = repository.getWarehouses();
        List<AssetModels.CommunityPost> communityPosts = repository.getCommunityPosts();
        List<AssetModels.Reminder> reminders = repository.getReminders();
        List<AssetModels.Inventory> inventories = repository.getInventories();
        List<AssetModels.Instruction> instructions = repository.getInstructions();

        String customerId = "";

        if (appPreferences.isLoggedIn()) {
            String phone = appPreferences.getCurrentPhone();
            customerId = appPreferences.getCustomerId();

            // 1. Fetch user profile from MongoDB via UserApi
            try {
                com.veggo.app.data.remote.api.UserApi userApi = com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.UserApi.class);
                retrofit2.Response<com.veggo.app.data.remote.dto.UserDto> userResponse = userApi.getUserByPhone(phone).execute();
                if (userResponse.isSuccessful() && userResponse.body() != null) {
                    com.veggo.app.data.remote.dto.UserDto userDto = userResponse.body();
                    user = new AssetModels.User();
                    user.phone = userDto.getPhone();
                    user.customerId = userDto.getCustomerId();
                    if (hasText(user.customerId)) {
                        customerId = user.customerId;
                    }
                    user.fullName = userDto.getFullName();
                    user.email = userDto.getEmail();
                    user.carbonPoint = userDto.getCarbonPoint();
                    user.certificateId = userDto.getCertificateId();
                    user.address = userDto.getAddress();
                    user.avatarUrl = userDto.getAvatarUrl();
                    user.birthDay = userDto.getBirthDay();
                    user.gender = userDto.getGender();
                    if (!hasText(user.birthDay)) {
                        user.birthDay = appPreferences.getBirthday();
                    }
                    if (!hasText(user.gender)) {
                        user.gender = appPreferences.getGender();
                    }
                    // Add addresses from addresses list in UserDto
                    if (userDto.getAddresses() != null && !userDto.getAddresses().isEmpty()) {
                        for (com.veggo.app.data.remote.dto.UserDto.AddressDto addrDto : userDto.getAddresses()) {
                            if (addrDto.isDefault() || user.address == null || user.address.isEmpty()) {
                                List<String> parts = new ArrayList<>();
                                addIfPresent(parts, addrDto.getLine1());
                                addIfPresent(parts, addrDto.getWard());
                                addIfPresent(parts, addrDto.getDistrict());
                                addIfPresent(parts, addrDto.getCity());
                                user.address = String.join(", ", parts);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback user mapping from preferences if network call failed or User not found in MongoDB
            if (user == null) {
                user = new AssetModels.User();
                user.phone = phone;
                user.customerId = customerId;
                user.fullName = appPreferences.getFullName();
                user.email = appPreferences.getEmail();
                user.avatarUrl = appPreferences.getAvatarUrl();
                user.birthDay = appPreferences.getBirthday();
                user.gender = appPreferences.getGender();
                user.carbonPoint = 0;
            } else {
                enrichUserFromPreferences(user, appPreferences);
            }

            if (!hasText(customerId) && user != null && hasText(user.customerId)) {
                customerId = user.customerId;
            }

            // 2. Fetch latest certificate request so the UI can distinguish pending/rejected states.
            try {
                if (hasText(customerId)) {
                    com.veggo.app.data.remote.api.CertificateApi certificateApi =
                            com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.CertificateApi.class);
                    retrofit2.Response<com.veggo.app.data.remote.api.CertificateApi.CertificateRequestResponse> requestResponse =
                            certificateApi.getLatestRequest(customerId).execute();
                    if (requestResponse.isSuccessful() && requestResponse.body() != null) {
                        latestCertificateRequest = requestResponse.body().data;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Fetch orders from MongoDB via OrderApi
            try {
                com.veggo.app.data.remote.api.OrderApi orderApi = com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.OrderApi.class);
                retrofit2.Response<List<com.veggo.app.data.remote.dto.OrderDto>> ordersResponse = orderApi.getOrders(customerId).execute();
                if (ordersResponse.isSuccessful() && ordersResponse.body() != null) {
                    for (com.veggo.app.data.remote.dto.OrderDto orderDto : ordersResponse.body()) {
                        AssetModels.Order order = mapOrder(orderDto);
                        customerOrders.add(order);
                        detailByOrderId.put(order.orderId, mapOrderDetail(orderDto, order.orderId));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            customerOrders.sort((left, right) -> compareDates(right.createdAt, left.createdAt));
        }

        synchronized (AssetScreenData.class) {
            for (AssetModels.Order guestOrder : guestOrderCache) {
                if (!containsOrder(customerOrders, guestOrder.orderId)) {
                    customerOrders.add(guestOrder);
                }
            }
            detailByOrderId.putAll(guestOrderDetailCache);
        }
        customerOrders.sort((left, right) -> compareDates(right.createdAt, left.createdAt));

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
                latestCertificateRequest,
                warehouseById,
                communityPosts,
                reminders,
                inventories,
                instructions
        );
    }

    @NonNull
    private static List<AssetModels.Certificate> loadCertificates(@NonNull AssetRepository repository) {
        try {
            com.veggo.app.data.remote.api.CertificateApi certificateApi =
                    com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.CertificateApi.class);
            retrofit2.Response<com.veggo.app.data.remote.api.CertificateApi.CertificateResponse> response =
                    certificateApi.getCertificates().execute();
            if (response.isSuccessful() && response.body() != null && response.body().data != null && !response.body().data.isEmpty()) {
                List<AssetModels.Certificate> certificates = new ArrayList<>();
                for (com.veggo.app.data.remote.api.CertificateApi.CertificateDto certificateDto : response.body().data) {
                    certificates.add(certificateDto.toAssetModel());
                }
                return certificates;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return repository.getCertificates();
    }

    public static synchronized AssetModels.Order cacheGuestOrder(
            @NonNull com.veggo.app.data.remote.dto.OrderDto orderDto
    ) {
        AssetModels.Order order = mapOrder(orderDto);
        AssetModels.OrderDetail detail = mapOrderDetail(orderDto, order.orderId);
        removeCachedGuestOrder(order.orderId);
        guestOrderCache.add(order);
        guestOrderDetailCache.put(order.orderId, detail);
        guestOrderCache.sort((left, right) -> compareDates(right.createdAt, left.createdAt));
        return order;
    }

    private static void removeCachedGuestOrder(@Nullable String orderId) {
        if (!hasText(orderId)) {
            return;
        }
        for (int index = guestOrderCache.size() - 1; index >= 0; index--) {
            AssetModels.Order cachedOrder = guestOrderCache.get(index);
            if (equals(cachedOrder.orderId, orderId)) {
                guestOrderCache.remove(index);
            }
        }
        guestOrderDetailCache.remove(orderId);
    }

    private static boolean containsOrder(@NonNull List<AssetModels.Order> orders, @Nullable String orderId) {
        if (!hasText(orderId)) {
            return false;
        }
        for (AssetModels.Order order : orders) {
            if (equals(order.orderId, orderId)) {
                return true;
            }
        }
        return false;
    }

    private static AssetModels.Order mapOrder(
            @NonNull com.veggo.app.data.remote.dto.OrderDto orderDto
    ) {
        AssetModels.Order order = new AssetModels.Order();
        order.orderId = hasText(orderDto.getOrderId()) ? orderDto.getOrderId() : orderDto.getId();
        order.customerId = orderDto.getUserId();
        order.paymentMethod = orderDto.getPaymentMethod();
        order.subtotal = orderDto.getSubtotal();
        order.shippingFee = orderDto.getShippingFee();
        order.shippingDiscount = orderDto.getShippingDiscount();
        order.discount = orderDto.getDiscount();
        order.totalAmount = orderDto.getTotal();
        order.status = orderDto.getStatus();
        order.rejectReason = orderDto.getRejectReason();
        order.code = orderDto.getCode();
        order.createdAt = new AssetModels.MongoDate();
        order.createdAt.date = hasText(orderDto.getCreatedAt())
                ? orderDto.getCreatedAt()
                : ISO_FORMAT.format(new java.util.Date());
        return order;
    }

    private static AssetModels.OrderDetail mapOrderDetail(
            @NonNull com.veggo.app.data.remote.dto.OrderDto orderDto,
            @NonNull String orderId
    ) {
        AssetModels.OrderDetail detail = new AssetModels.OrderDetail();
        detail.orderId = orderId;
        detail.promotionId = orderDto.getPromotionId();
        detail.shippingPromotionId = orderDto.getShippingPromotionId();
        detail.carbonPointEarned = orderDto.getCarbonPointEarned();
        detail.totalCarbonEmission = orderDto.getTotalCarbonEmission();
        detail.items = new ArrayList<>();
        if (orderDto.getItems() != null) {
            for (com.veggo.app.data.remote.dto.OrderDto.OrderItemDto itemDto : orderDto.getItems()) {
                AssetModels.OrderDetailItem item = new AssetModels.OrderDetailItem();
                item.productName = itemDto.getName();
                item.price = itemDto.getPrice();
                item.originalPrice = itemDto.getOriginalPrice() > 0
                        ? itemDto.getOriginalPrice() : itemDto.getPrice();
                item.quantity = itemDto.getQuantity();
                item.image = itemDto.getImageUrl();
                item.sku = itemDto.getSku();
                item.unit = hasText(itemDto.getUnit()) ? itemDto.getUnit() : "kg";
                item.carbonPointEarned = itemDto.getCarbonPointEarned();
                item.totalCarbonEmission = itemDto.getTotalCarbonEmission();
                detail.items.add(item);
            }
        }

        Map<String, Object> infoMap = orderDto.getShippingInfo();
        Map<String, Object> addrMap = orderDto.getShippingAddress();
        if (infoMap != null && !infoMap.isEmpty()) {
            AssetModels.ShippingInfo info = new AssetModels.ShippingInfo();
            info.fullName = getMapString(infoMap, "fullName");
            if (!hasText(info.fullName)) {
                info.fullName = getMapString(infoMap, "receiverName");
            }
            info.phone = getMapString(infoMap, "phone");
            info.email = getMapString(infoMap, "email");
            info.warehouseId = getMapString(infoMap, "warehouse_id");
            if (!hasText(info.warehouseId)) {
                info.warehouseId = orderDto.getWarehouseId();
            }

            AssetModels.ShippingAddress addr = new AssetModels.ShippingAddress();
            Object nestedAddress = infoMap.get("address");
            if (nestedAddress instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nestedAddress;
                addr.detail = getMapString(nestedMap, "detail");
                if (!hasText(addr.detail)) {
                    addr.detail = getMapString(nestedMap, "line1");
                }
                addr.ward = getMapString(nestedMap, "ward");
                addr.district = getMapString(nestedMap, "district");
                addr.city = getMapString(nestedMap, "city");
            } else {
                addr.detail = getMapString(infoMap, "line1");
                if (!hasText(addr.detail)) {
                    addr.detail = getMapString(infoMap, "detail");
                }
                addr.ward = getMapString(infoMap, "ward");
                addr.district = getMapString(infoMap, "district");
                addr.city = getMapString(infoMap, "city");
            }

            info.address = addr;
            detail.shippingInfo = info;
        } else if (addrMap != null && !addrMap.isEmpty()) {
            AssetModels.ShippingInfo info = new AssetModels.ShippingInfo();
            info.fullName = getMapString(addrMap, "receiverName");
            if (!hasText(info.fullName)) {
                info.fullName = getMapString(addrMap, "fullName");
            }
            info.phone = getMapString(addrMap, "phone");
            info.email = getMapString(addrMap, "email");
            info.warehouseId = orderDto.getWarehouseId();

            AssetModels.ShippingAddress addr = new AssetModels.ShippingAddress();
            addr.detail = getMapString(addrMap, "line1");
            if (!hasText(addr.detail)) {
                addr.detail = getMapString(addrMap, "detail");
            }
            addr.ward = getMapString(addrMap, "ward");
            addr.district = getMapString(addrMap, "district");
            addr.city = getMapString(addrMap, "city");

            info.address = addr;
            detail.shippingInfo = info;
        }
        return detail;
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

    private static void enrichUserFromPreferences(
            @NonNull AssetModels.User user,
            @NonNull AppPreferences appPreferences
    ) {
        if (hasText(appPreferences.getFullName())) {
            user.fullName = appPreferences.getFullName();
        }
        if (hasText(appPreferences.getCurrentPhone())) {
            user.phone = appPreferences.getCurrentPhone();
        }
        if (hasText(appPreferences.getEmail())) {
            user.email = appPreferences.getEmail();
        }
        if (!hasText(user.avatarUrl) && hasText(appPreferences.getAvatarUrl())) {
            user.avatarUrl = appPreferences.getAvatarUrl();
        }
        if (!hasText(user.birthDay) && hasText(appPreferences.getBirthday())) {
            user.birthDay = appPreferences.getBirthday();
        }
        if (!hasText(user.gender) && hasText(appPreferences.getGender())) {
            user.gender = appPreferences.getGender();
        }
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
            String orderStatus = safe(order.status);
            if (equals(orderStatus, status)
                    || ("shipping".equals(status) && "delivered".equals(orderStatus))
                    || ("delivered".equals(status) && (
                    "unreview".equals(orderStatus)
                            || "reviewed".equals(orderStatus)
                            || "completed".equals(orderStatus)
                            || "rejected".equals(orderStatus)))) {
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
            if ("pending".equals(bucket) && "processing_return".equals(status)) {
                filtered.add(order);
            } else if ("processing".equals(bucket) && "returning".equals(status)) {
                filtered.add(order);
            } else if ("completed".equals(bucket) && "returned".equals(status)) {
                filtered.add(order);
            } else if ("rejected".equals(bucket) && "rejected".equals(status)) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    public static List<AssetModels.Order> filterReviewOrders(
            @NonNull Snapshot snapshot,
            boolean done
    ) {
        List<AssetModels.Order> filtered = new ArrayList<>();
        for (AssetModels.Order order : snapshot.orders) {
            String status = safe(order.status);
            if (!done && "unreview".equals(status)) {
                filtered.add(order);
            } else if (done && "reviewed".equals(status)) {
                filtered.add(order);
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
        String orderTitle = orderTitle(order);
        setText(card, R.id.orderPendingItemCode, orderTitle);
        setText(card, R.id.orderShippingItemCode, orderTitle);
        setText(card, R.id.orderDeliveredItemCode, orderTitle);
        setText(card, R.id.orderCancelledItemCode, orderTitle);
        setText(card, R.id.orderPendingItemId, order.orderId);
        setText(card, R.id.orderShippingItemId, order.orderId);
        setText(card, R.id.orderDeliveredItemId, order.orderId);
        setText(card, R.id.orderCancelledItemId, order.orderId);
        setText(card, R.id.returnOrderCode, order.orderId);
        setText(card, R.id.reviewOrderCode, order.orderId);

        setText(card, R.id.orderPendingItemStatus, statusLabel(order.status));
        setText(card, R.id.orderShippingItemStatus, statusLabel(order.status));
        setText(card, R.id.orderDeliveredItemStatus, statusLabel(order.status));
        setText(card, R.id.orderCancelledItemStatus, statusLabel(order.status));
        setText(card, R.id.returnOrderStatus, statusLabel(order.status));
        setText(card, R.id.reviewOrderStatus, statusLabel(order.status));
        applyStatusChipStyle(context, card, R.id.orderPendingItemStatus, order.status);
        applyStatusChipStyle(context, card, R.id.orderShippingItemStatus, order.status);
        applyStatusChipStyle(context, card, R.id.orderDeliveredItemStatus, order.status);
        applyStatusChipStyle(context, card, R.id.orderCancelledItemStatus, order.status);
        applyStatusChipStyle(context, card, R.id.returnOrderStatus, order.status);
        applyStatusChipStyle(context, card, R.id.reviewOrderStatus, order.status);

        AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
        bindProductBlock(context, card, detail, order);
    }

    private static void applyStatusChipStyle(
            @NonNull Context context,
            @NonNull View root,
            int statusViewId,
            @Nullable String status
    ) {
        TextView view = root.findViewById(statusViewId);
        if (view == null) {
            return;
        }
        String cleanStatus = safe(status);
        int backgroundRes;
        int textColorRes;
        if (isReturnStatus(cleanStatus) || "cancelled".equals(cleanStatus)) {
            backgroundRes = R.drawable.bg_order_cancelled_chip;
            textColorRes = R.color.order_status_cancelled;
        } else if ("pending".equals(cleanStatus)) {
            backgroundRes = R.drawable.bg_order_pending_chip;
            textColorRes = R.color.order_status_pending;
        } else if ("shipping".equals(cleanStatus)) {
            backgroundRes = R.drawable.bg_order_shipping_chip;
            textColorRes = R.color.order_status_shipping;
        } else if ("unreview".equals(cleanStatus)) {
            backgroundRes = R.drawable.bg_order_cancelled_chip;
            textColorRes = R.color.order_status_unreview;
        } else {
            backgroundRes = R.drawable.bg_order_delivered_chip;
            textColorRes = R.color.order_status_delivered;
        }
        view.setBackgroundResource(backgroundRes);
        view.setTextColor(context.getColor(textColorRes));
    }

    private static boolean isReturnStatus(@Nullable String status) {
        String cleanStatus = safe(status);
        return "processing_return".equals(cleanStatus)
                || "returning".equals(cleanStatus)
                || "returned".equals(cleanStatus)
                || "rejected".equals(cleanStatus);
    }

    private static String orderTitle(@NonNull AssetModels.Order order) {
        String orderDate = date(order.createdAt);
        int commaIndex = orderDate.indexOf(',');
        if (commaIndex >= 0) {
            orderDate = orderDate.substring(0, commaIndex).trim();
        }
        return hasText(orderDate) ? "Đơn hàng ngày " + orderDate : "Đơn hàng";
    }

    public static void bindProductBlock(
            @NonNull Context context,
            @NonNull View root,
            @Nullable AssetModels.OrderDetail detail,
            @NonNull AssetModels.Order order
    ) {
        if (detail == null || detail.items == null || detail.items.isEmpty()) {
            clearProductBlock(root);
            return;
        }
        AssetModels.OrderDetailItem item = detail.items.get(0);
        setText(root, R.id.orderItemProductName, item.productName);
        setText(root, R.id.orderItemVariant, item.unit);
        setText(root, R.id.orderItemQuantity, "x" + item.quantity);
        setText(root, R.id.orderItemOldPrice, item.originalPrice > item.price ? money(item.originalPrice) : "");
        setText(root, R.id.orderItemPrice, money(item.price));
        setText(root, R.id.orderItemTotalLabel, "Thành tiền (" + orderProductCount(detail) + " sản phẩm):");
        setText(root, R.id.orderItemTotal, money(Math.round(order.totalAmount)));
        ImageView image = root.findViewById(R.id.orderItemProductImage);
        if (image != null && hasText(item.image)) {
            Glide.with(context).load(item.image).placeholder(R.drawable.ic_vegetable).into(image);
        }
    }

    private static void clearProductBlock(@NonNull View root) {
        setText(root, R.id.orderItemProductName, "");
        setText(root, R.id.orderItemVariant, "");
        setText(root, R.id.orderItemQuantity, "");
        setText(root, R.id.orderItemOldPrice, "");
        setText(root, R.id.orderItemPrice, "");
        setText(root, R.id.orderItemTotalLabel, "Thành tiền (0 sản phẩm):");
        setText(root, R.id.orderItemTotal, "");
    }

    private static int orderProductCount(@NonNull AssetModels.OrderDetail detail) {
        if (detail.items == null || detail.items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (AssetModels.OrderDetailItem detailItem : detail.items) {
            count += Math.max(1, detailItem.quantity);
        }
        return count;
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
            case "unreview":
                return "Chưa đánh giá";
            case "reviewed":
                return "Đã đánh giá";
            case "completed":
                return "Hoàn tất";
            case "cancelled":
                return "Đã hủy";
            case "returned":
                return "Đã hoàn trả";
            case "rejected":
                return "Từ chối hoàn/trả";
            case "returning":
                return "Đang hoàn/trả";
            case "processing_return":
                return "Đang xử lý hoàn/trả";
            default:
                return safe(status);
        }
    }

    public static String paymentLabel(@Nullable String paymentMethod) {
        if ("cod".equalsIgnoreCase(paymentMethod)) {
            return "Thanh toán khi nhận hàng";
        }
        if ("bank".equalsIgnoreCase(paymentMethod)
                || "card".equalsIgnoreCase(paymentMethod)
                || "bank_transfer".equalsIgnoreCase(paymentMethod)) {
            return "Chuyển khoản qua ngân hàng";
        }
        if ("momo".equalsIgnoreCase(paymentMethod)) {
            return "Ví MoMo";
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

    private static String getMapString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "" : String.valueOf(val);
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
        @Nullable
        public final com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto latestCertificateRequest;
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
                @Nullable com.veggo.app.data.remote.api.CertificateApi.CertificateRequestDto latestCertificateRequest,
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
            this.latestCertificateRequest = latestCertificateRequest;
            this.warehouseById = warehouseById;
            this.communityPosts = Collections.unmodifiableList(communityPosts);
            this.reminders = Collections.unmodifiableList(reminders);
            this.inventories = Collections.unmodifiableList(inventories);
            this.instructions = Collections.unmodifiableList(instructions);
        }
    }

    public static void showOrderOptions(Context context) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        android.view.View sheet = android.view.LayoutInflater.from(context)
                .inflate(R.layout.layout_order_history_options, null, false);
        
        sheet.findViewById(R.id.orderOptionHistory).setOnClickListener(v -> {
            dialog.dismiss();
            android.content.Intent intent = new android.content.Intent(context, com.veggo.app.presentation.order.OrderHistoryActivity.class);
            context.startActivity(intent);
        });
        sheet.findViewById(R.id.orderOptionRecurring).setOnClickListener(v -> {
            dialog.dismiss();
            if (!new AppPreferences(context).isLoggedIn()) {
                LoginRequiredActivity.open(context, "đơn hàng định kỳ");
                return;
            }
            if (!(context instanceof com.veggo.app.presentation.order.RecurringOrdersActivity)) {
                android.content.Intent intent = new android.content.Intent(context, com.veggo.app.presentation.order.RecurringOrdersActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
        sheet.findViewById(R.id.orderOptionReviews).setOnClickListener(v -> {
            dialog.dismiss();
            if (!(context instanceof com.veggo.app.presentation.order.ReviewsActivity)) {
                android.content.Intent intent = new android.content.Intent(context, com.veggo.app.presentation.order.ReviewsActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
        sheet.findViewById(R.id.orderOptionReturns).setOnClickListener(v -> {
            dialog.dismiss();
            if (!(context instanceof com.veggo.app.presentation.order.ReturnsActivity)) {
                android.content.Intent intent = new android.content.Intent(context, com.veggo.app.presentation.order.ReturnsActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
        dialog.setContentView(sheet);
        dialog.show();
    }
}
