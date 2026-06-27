package com.veggo.app.presentation.order;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<RecurringOrder>>() {}.getType();
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
            if (occursOn(order, target)) {
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
                if (occursOn(order, cursor)) {
                    dates.add(storageFormat.format(cursor.getTime()));
                }
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        Collections.sort(dates);
        return dates;
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
        int field = Calendar.DAY_OF_MONTH;
        int amount = 7;
        if (frequency.contains("tháng")) {
            field = Calendar.MONTH;
            amount = 1;
        } else if (frequency.contains("quý")) {
            field = Calendar.MONTH;
            amount = 3;
        } else if (frequency.contains("năm")) {
            field = Calendar.YEAR;
            amount = 1;
        } else {
            field = Calendar.DAY_OF_MONTH;
            amount = 7;
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

    public static class RecurringProductItem {
        public String productId;
        public String sku;
        public String name;
        public String imageUrl;
        public String unit;
        public int quantity;
        public double selectedWeight;
        public long unitPrice;
        public double carbonPoints;

        public long totalPrice() {
            return unitPrice * Math.max(1, quantity);
        }
    }
}
