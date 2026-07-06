package com.veggo.app.presentation.order;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.veggo.app.data.remote.dto.CartDto;

public class RecurringCheckoutStore {
    private static final String PREF_NAME = "veggo_recurring_checkout";
    private static final String KEY_CART_JSON = "cart_json";
    private static final String KEY_ORDER_ID = "order_id";
    private static final String KEY_OCCURRENCE_DATE = "occurrence_date";
    private static final String KEY_RECEIVER_NAME = "receiver_name";
    private static final String KEY_RECEIVER_PHONE = "receiver_phone";
    private static final String KEY_DETAIL_ADDRESS = "detail_address";
    private static final String KEY_WARD = "ward";
    private static final String KEY_DISTRICT = "district";
    private static final String KEY_CITY = "city";
    private static final String KEY_DELIVERY_SLOT = "delivery_slot";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public RecurringCheckoutStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(
            @NonNull CartDto cart,
            @NonNull String recurringOrderId,
            @NonNull String occurrenceDate,
            @NonNull RecurringOrderStore.RecurringOrder order
    ) {
        prefs.edit()
                .putString(KEY_CART_JSON, gson.toJson(cart))
                .putString(KEY_ORDER_ID, recurringOrderId)
                .putString(KEY_OCCURRENCE_DATE, occurrenceDate)
                .putString(KEY_RECEIVER_NAME, safe(order.receiverName))
                .putString(KEY_RECEIVER_PHONE, safe(order.receiverPhone))
                .putString(KEY_DETAIL_ADDRESS, safe(order.detailAddress))
                .putString(KEY_WARD, safe(order.ward))
                .putString(KEY_DISTRICT, safe(order.district))
                .putString(KEY_CITY, safe(order.city))
                .putString(KEY_DELIVERY_SLOT, safe(order.deliverySlot))
                .apply();
    }

    public boolean hasPending() {
        return prefs.contains(KEY_CART_JSON);
    }

    public static final class CheckoutSession {
        public CartDto cart;
        public CheckoutContext context;
    }

    @Nullable
    public CheckoutSession consumeSession() {
        String json = prefs.getString(KEY_CART_JSON, null);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        CheckoutSession session = new CheckoutSession();
        session.cart = gson.fromJson(json, CartDto.class);
        session.context = new CheckoutContext();
        session.context.recurringOrderId = prefs.getString(KEY_ORDER_ID, "");
        session.context.occurrenceDate = prefs.getString(KEY_OCCURRENCE_DATE, "");
        session.context.receiverName = prefs.getString(KEY_RECEIVER_NAME, "");
        session.context.receiverPhone = prefs.getString(KEY_RECEIVER_PHONE, "");
        session.context.detailAddress = prefs.getString(KEY_DETAIL_ADDRESS, "");
        session.context.ward = prefs.getString(KEY_WARD, "");
        session.context.district = prefs.getString(KEY_DISTRICT, "");
        session.context.city = prefs.getString(KEY_CITY, "");
        session.context.deliverySlot = prefs.getString(KEY_DELIVERY_SLOT, "");
        clear();
        return session;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class CheckoutContext {
        public String recurringOrderId;
        public String occurrenceDate;
        public String receiverName;
        public String receiverPhone;
        public String detailAddress;
        public String ward;
        public String district;
        public String city;
        public String deliverySlot;

        public String fullAddressLine() {
            StringBuilder builder = new StringBuilder();
            appendPart(builder, detailAddress);
            appendPart(builder, ward);
            appendPart(builder, district);
            appendPart(builder, city);
            return builder.toString();
        }

        private static void appendPart(StringBuilder builder, String value) {
            if (value == null || value.trim().isEmpty()) {
                return;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value.trim());
        }
    }
}
