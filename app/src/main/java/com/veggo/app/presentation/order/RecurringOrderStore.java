package com.veggo.app.presentation.order;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.core.notification.RecurringConfirmationHelper;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.ProductDto;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RecurringOrderStore {
    private static final String PREFS = "recurring_orders";
    private static final String KEY_ORDERS = "orders";
    private static final String KEY_OCCURRENCES = "occurrences";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<RecurringOrder>>() {}.getType();
    private final Type occurrenceListType = new TypeToken<List<OccurrenceState>>() {}.getType();
    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public RecurringOrderStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void add(@NonNull RecurringOrder order) {
        List<RecurringOrder> orders = all();
        orders.add(order);
        prefs.edit().putString(KEY_ORDERS, gson.toJson(orders, listType)).apply();
    }

    public void update(@NonNull RecurringOrder updatedOrder) {
        List<RecurringOrder> orders = all();
        for (int i = 0; i < orders.size(); i++) {
            if (safe(updatedOrder.id).equals(safe(orders.get(i).id))) {
                orders.set(i, updatedOrder);
                prefs.edit().putString(KEY_ORDERS, gson.toJson(orders, listType)).apply();
                return;
            }
        }
    }

    public void delete(String orderId) {
        List<RecurringOrder> orders = all();
        for (int i = orders.size() - 1; i >= 0; i--) {
            if (safe(orderId).equals(safe(orders.get(i).id))) {
                orders.remove(i);
            }
        }
        prefs.edit().putString(KEY_ORDERS, gson.toJson(orders, listType)).apply();
    }

    public RecurringOrder findById(String orderId) {
        for (RecurringOrder order : all()) {
            if (safe(orderId).equals(safe(order.id))) {
                return order;
            }
        }
        return null;
    }

    public List<RecurringOrder> forCustomer(String customerId) {
        List<RecurringOrder> result = new ArrayList<>();
        String cleanCustomerId = safe(customerId);
        for (RecurringOrder order : all()) {
            if (cleanCustomerId.equals(safe(order.customerId))) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparing(order -> safe(order.deliveryDate)));
        return result;
    }

    public List<RecurringOrder> forCustomerOnDate(String customerId, String deliveryDate) {
        List<RecurringOrder> result = new ArrayList<>();
        String cleanDate = safe(deliveryDate);
        for (RecurringOrder order : forCustomer(customerId)) {
            if (cleanDate.equals(safe(order.deliveryDate))) {
                result.add(order);
            }
        }
        return result;
    }

    public List<RecurringOrder> occurrencesForCustomerOnDate(String customerId, String date) {
        List<RecurringOrder> result = new ArrayList<>();
        Calendar target = parseDate(date);
        if (target == null) return result;
        for (RecurringOrder order : forCustomer(customerId)) {
            if (occursOn(order, target) && isVisibleOccurrence(order.id, date)) {
                result.add(order);
            }
        }
        return result;
    }

    public List<String> occurrenceDatesForCustomerInMonth(String customerId, Calendar month) {
        List<String> dates = new ArrayList<>();
        if (month == null) return dates;
        Calendar firstDay = (Calendar) month.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(firstDay);
        Calendar lastDay = (Calendar) firstDay.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, firstDay.getActualMaximum(Calendar.DAY_OF_MONTH));

        for (RecurringOrder order : forCustomer(customerId)) {
            Calendar cursor = (Calendar) firstDay.clone();
            while (!cursor.after(lastDay)) {
                String storageDate = storageFormat.format(cursor.getTime());
                if (occursOn(order, cursor) && isVisibleOccurrence(order.id, storageDate)) {
                    dates.add(storageDate);
                }
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        Collections.sort(dates);
        return dates;
    }

    public OccurrenceState getOccurrence(String orderId, String date) {
        String key = RecurringConfirmationHelper.occurrenceKey(orderId, date);
        for (OccurrenceState state : allOccurrences()) {
            if (key.equals(RecurringConfirmationHelper.occurrenceKey(state.orderId, state.date))) {
                return state;
            }
        }
        return null;
    }

    public void upsertOccurrence(@NonNull OccurrenceState state) {
        List<OccurrenceState> occurrences = allOccurrences();
        String key = RecurringConfirmationHelper.occurrenceKey(state.orderId, state.date);
        for (int i = 0; i < occurrences.size(); i++) {
            OccurrenceState existing = occurrences.get(i);
            if (key.equals(RecurringConfirmationHelper.occurrenceKey(existing.orderId, existing.date))) {
                occurrences.set(i, state);
                saveOccurrences(occurrences);
                return;
            }
        }
        occurrences.add(state);
        saveOccurrences(occurrences);
    }

    public void markOccurrencePending(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        if (state == null) {
            state = new OccurrenceState();
            state.orderId = orderId;
            state.date = date;
        }
        if (RecurringConfirmationHelper.isTerminal(state.status)
                || RecurringConfirmationHelper.hasOrderPlaced(state.status)) {
            return;
        }
        state.status = RecurringConfirmationHelper.STATUS_PENDING;
        upsertOccurrence(state);
    }

    public void markConfirmNotified(String orderId, String date) {
        OccurrenceState state = getOrCreateOccurrence(orderId, date);
        state.confirmNotifiedAt = System.currentTimeMillis();
        if (state.status == null || state.status.isEmpty()) {
            state.status = RecurringConfirmationHelper.STATUS_PENDING;
        }
        upsertOccurrence(state);
    }

    public void markDeliveryReminderNotified(String orderId, String date) {
        OccurrenceState state = getOrCreateOccurrence(orderId, date);
        state.deliveryReminderNotifiedAt = System.currentTimeMillis();
        upsertOccurrence(state);
    }

    public void markOccurrenceSkipped(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        if (state != null && RecurringConfirmationHelper.hasOrderPlaced(state.status)) {
            return;
        }
        if (state == null) {
            state = new OccurrenceState();
            state.orderId = orderId;
            state.date = date;
        }
        state.status = RecurringConfirmationHelper.STATUS_SKIPPED;
        upsertOccurrence(state);
    }

    private OccurrenceState getOrCreateOccurrence(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        if (state == null) {
            state = new OccurrenceState();
            state.orderId = orderId;
            state.date = date;
        }
        return state;
    }

    public void markOccurrenceCancelled(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        if (state == null) {
            state = new OccurrenceState();
            state.orderId = orderId;
            state.date = date;
        }
        state.status = RecurringConfirmationHelper.STATUS_CANCELLED;
        upsertOccurrence(state);
    }

    public void markOccurrenceCompleted(String orderId, String date, String placedOrderId) {
        OccurrenceState state = getOccurrence(orderId, date);
        if (state == null) {
            state = new OccurrenceState();
            state.orderId = orderId;
            state.date = date;
        }
        state.status = RecurringConfirmationHelper.STATUS_COMPLETED;
        state.placedOrderId = placedOrderId;
        upsertOccurrence(state);
    }

    public boolean isVisibleOccurrence(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        return RecurringConfirmationHelper.isVisibleOnCalendar(state == null ? null : state.status);
    }

    public boolean needsConfirmation(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        return RecurringConfirmationHelper.isActionable(state == null ? null : state.status)
                && RecurringConfirmationHelper.isConfirmWindowOpen(date);
    }

    public boolean hasPlacedOrder(String orderId, String date) {
        OccurrenceState state = getOccurrence(orderId, date);
        return state != null && RecurringConfirmationHelper.hasOrderPlaced(state.status);
    }

    public int countPendingOccurrencesForCustomerOnDate(String customerId, String date) {
        int count = 0;
        for (RecurringOrder order : occurrencesForCustomerOnDate(customerId, date)) {
            if (needsConfirmation(order.id, date)) {
                count++;
            }
        }
        return count;
    }

    public boolean orderOccursOnDate(RecurringOrder order, String date) {
        Calendar target = parseDate(date);
        return target != null && occursOn(order, target);
    }

    public CartDto buildCheckoutCart(@NonNull RecurringOrder order) {
        CartDto cart = new CartDto();
        List<CartDto.CartItemDto> items = new ArrayList<>();
        if (order.items != null) {
            for (RecurringProductItem productItem : order.items) {
                boolean hasWeightOptions = productItem.hasWeightOptions
                        || inferHasWeightOptions(productItem);
                double selectedWeight = productItem.selectedWeight > 0 ? productItem.selectedWeight : 1.0;
                long baseUnitPrice = productItem.baseUnitPrice > 0
                        ? productItem.baseUnitPrice
                        : reverseBaseUnitPrice(productItem.unitPrice, selectedWeight, hasWeightOptions);
                double baseCarbon = productItem.baseCarbonSavingPoint;
                double emissionFactor = productItem.emissionFactor;
                if (baseCarbon <= 0 && productItem.carbonPoints > 0 && productItem.quantity > 0) {
                    baseCarbon = productItem.carbonPoints / productItem.quantity;
                }

                ProductDto product = new ProductDto();
                product.setId(productItem.productId);
                product.setSku(productItem.sku);
                product.setProductName(productItem.name);
                product.setPrice(baseUnitPrice);
                product.setOriginalPrice(baseUnitPrice);
                product.setImageUrl(productItem.imageUrl);
                product.setWeight(hasWeightOptions ? formatWeightLabel(selectedWeight) : productItem.unit);
                product.setCarbonSavingPoint(baseCarbon);
                product.setEmissionFactor(emissionFactor);
                if (hasWeightOptions) {
                    List<Double> weights = new ArrayList<>();
                    weights.add(selectedWeight);
                    product.setWeightOptions(weights);
                }

                CartDto.CartItemDto item = new CartDto.CartItemDto();
                item.setSku(safe(productItem.sku));
                item.setProduct(product);
                item.setQuantity(Math.max(1, productItem.quantity));
                item.setSelectedWeight(selectedWeight);
                item.setPrice(variantPrice(baseUnitPrice, selectedWeight, hasWeightOptions));
                item.setOriginalPrice(item.getPrice());
                items.add(item);
            }
        }
        cart.setItems(items);
        return cart;
    }

    private static boolean inferHasWeightOptions(RecurringProductItem productItem) {
        if (productItem == null) {
            return false;
        }
        return productItem.selectedWeight > 0
                && productItem.selectedWeight != 1.0
                && safe(productItem.unit).toLowerCase(Locale.US).contains("kg");
    }

    private static long reverseBaseUnitPrice(long variantPrice, double selectedWeight, boolean hasWeightOptions) {
        if (!hasWeightOptions || selectedWeight <= 0) {
            return variantPrice;
        }
        return Math.round(variantPrice / selectedWeight);
    }

    private static long variantPrice(long basePrice, double selectedWeight, boolean hasWeightOptions) {
        return Math.round(basePrice * (hasWeightOptions ? selectedWeight : 1.0));
    }

    private static String formatWeightLabel(double weight) {
        if (weight >= 1) {
            return String.format(Locale.US, weight == Math.round(weight) ? "%.0fkg" : "%.2fkg", weight);
        }
        return String.format(Locale.US, "%.0fg", weight * 1000);
    }

    private boolean occursOn(RecurringOrder order, Calendar target) {
        Calendar start = parseDate(order.deliveryDate);
        if (start == null) return false;
        clearTime(start);
        Calendar cleanTarget = (Calendar) target.clone();
        clearTime(cleanTarget);
        if (cleanTarget.before(start)) return false;

        String frequency = safe(order.frequency).toLowerCase(new Locale("vi", "VN"));
        if (frequency.contains("ngày")) {
            return true;
        }
        if (frequency.contains("tuần")) {
            if (start.get(Calendar.DAY_OF_WEEK) != cleanTarget.get(Calendar.DAY_OF_WEEK)) {
                return false;
            }
            long diffMs = cleanTarget.getTimeInMillis() - start.getTimeInMillis();
            long diffDays = diffMs / (24L * 60L * 60L * 1000L);
            return diffDays % 7 == 0;
        }
        int field = Calendar.DAY_OF_MONTH;
        int amount = 1;
        if (frequency.contains("tháng")) {
            field = Calendar.MONTH;
            amount = 1;
        } else if (frequency.contains("quý")) {
            field = Calendar.MONTH;
            amount = 3;
        } else if (frequency.contains("năm")) {
            field = Calendar.YEAR;
            amount = 1;
        }

        Calendar occurrence = (Calendar) start.clone();
        while (occurrence.before(cleanTarget)) {
            occurrence.add(field, amount);
        }
        return sameDay(occurrence, cleanTarget);
    }

    private Calendar parseDate(String value) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(storageFormat.parse(value));
            clearTime(calendar);
            return calendar;
        } catch (Exception exception) {
            return null;
        }
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean sameDay(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private List<RecurringOrder> all() {
        String json = prefs.getString(KEY_ORDERS, "[]");
        List<RecurringOrder> orders = gson.fromJson(json, listType);
        return orders == null ? new ArrayList<>() : new ArrayList<>(orders);
    }

    private List<OccurrenceState> allOccurrences() {
        String json = prefs.getString(KEY_OCCURRENCES, "[]");
        List<OccurrenceState> occurrences = gson.fromJson(json, occurrenceListType);
        return occurrences == null ? new ArrayList<>() : new ArrayList<>(occurrences);
    }

    private void saveOccurrences(List<OccurrenceState> occurrences) {
        prefs.edit().putString(KEY_OCCURRENCES, gson.toJson(occurrences, occurrenceListType)).apply();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class RecurringOrder {
        public String id;
        public String customerId;
        public String name;
        public String frequency;
        public String deliveryDate;
        public String deliverySlot;
        public String receiverName;
        public String receiverPhone;
        public String city;
        public String district;
        public String ward;
        public String detailAddress;
        public String itemSummary;
        public long estimatedTotal;
        public double carbonPoints;
        public List<RecurringProductItem> items;
        public String status;
        public String createdAt;

        public String displayAddress() {
            List<String> parts = new ArrayList<>();
            Collections.addAll(parts, detailAddress, ward, district, city);
            List<String> cleanParts = new ArrayList<>();
            for (String part : parts) {
                if (part != null && !part.trim().isEmpty()) {
                    cleanParts.add(part.trim());
                }
            }
            return android.text.TextUtils.join(", ", cleanParts);
        }
    }

    public static class OccurrenceState {
        public String orderId;
        public String date;
        public String status;
        public String placedOrderId;
        public long confirmNotifiedAt;
        public long deliveryReminderNotifiedAt;
        /** @deprecated dùng confirmNotifiedAt */
        public long notifiedAt;
    }

    public static class RecurringProductItem {
        public String productId;
        public String sku;
        public String name;
        public String imageUrl;
        public String unit;
        public int quantity;
        public double selectedWeight;
        public long unitPrice;
        public long baseUnitPrice;
        public double baseCarbonSavingPoint;
        public double emissionFactor;
        public boolean hasWeightOptions;
        public double carbonPoints;

        public long totalPrice() {
            return unitPrice * Math.max(1, quantity);
        }
    }
}
