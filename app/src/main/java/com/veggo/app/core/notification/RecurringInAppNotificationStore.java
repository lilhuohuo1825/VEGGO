package com.veggo.app.core.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.api.RecurringOrderApi;
import com.veggo.app.data.remote.dto.RecurringNotificationDto;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RecurringInAppNotificationStore {

    public static final String TYPE_CONFIRM = "recurring_confirm";
    public static final String TYPE_DELIVERY = "recurring_delivery";
    public static final String TYPE_SKIPPED = "recurring_skipped";

    private static final String PREFS = "recurring_in_app_notifications";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 80;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Entry>>() {}.getType();

    public RecurringInAppNotificationStore(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void addPushNotification(
            @NonNull String recurringOrderId,
            @NonNull String occurrenceDate,
            @NonNull String type,
            @NonNull String title,
            @NonNull String body,
            @NonNull String action
    ) {
        addEntry(recurringOrderId, occurrenceDate, type, title, body, action, true);
    }

    public void addInAppNotification(
            @NonNull String recurringOrderId,
            @NonNull String occurrenceDate,
            @NonNull String type,
            @NonNull String title,
            @NonNull String body,
            @NonNull String action
    ) {
        addEntry(recurringOrderId, occurrenceDate, type, title, body, action, false);
    }

    private void addEntry(
            String recurringOrderId,
            String occurrenceDate,
            String type,
            String title,
            String body,
            String action,
            boolean outsideApp
    ) {
        List<Entry> items = all();
        String dedupeKey = recurringOrderId + "|" + occurrenceDate + "|" + type + "|" + outsideApp;
        for (Entry existing : items) {
            if (dedupeKey.equals(existing.dedupeKey())) {
                existing.title = title;
                existing.body = body;
                existing.action = action;
                existing.createdAt = System.currentTimeMillis();
                existing.read = false;
                save(items);
                pushRemoteAsync(existing);
                return;
            }
        }

        Entry entry = new Entry();
        entry.id = UUID.randomUUID().toString();
        entry.recurringOrderId = recurringOrderId;
        entry.occurrenceDate = occurrenceDate;
        entry.type = type;
        entry.title = title;
        entry.body = body;
        entry.action = action;
        entry.createdAt = System.currentTimeMillis();
        entry.read = false;
        entry.outsideApp = outsideApp;
        items.add(0, entry);

        if (items.size() > MAX_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_ITEMS));
        }
        save(items);
        pushRemoteAsync(entry);
    }

    public void replaceFromRemote(@Nullable List<RecurringNotificationDto> remoteItems) {
        if (remoteItems == null) {
            return;
        }
        List<Entry> items = new ArrayList<>();
        for (RecurringNotificationDto dto : remoteItems) {
            Entry entry = fromDto(dto);
            if (entry != null) {
                items.add(entry);
            }
        }
        items.sort(Comparator.comparingLong((Entry entry) -> entry.createdAt).reversed());
        save(items);
    }

    @NonNull
    public List<Entry> all() {
        String json = prefs.getString(KEY_ITEMS, "[]");
        List<Entry> items = gson.fromJson(json, listType);
        if (items == null) {
            return new ArrayList<>();
        }
        items.sort(Comparator.comparingLong((Entry entry) -> entry.createdAt).reversed());
        return items;
    }

    public int countUnread() {
        int count = 0;
        for (Entry entry : all()) {
            if (!entry.read) {
                count++;
            }
        }
        return count;
    }

    public void markRead(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        List<Entry> items = all();
        for (Entry entry : items) {
            if (id.equals(entry.id)) {
                entry.read = true;
                save(items);
                pushMarkReadAsync(id);
                return;
            }
        }
    }

    public void markAllRead() {
        List<Entry> items = all();
        for (Entry entry : items) {
            entry.read = true;
        }
        save(items);
        pushMarkAllReadAsync();
    }

    @NonNull
    public static String targetId(@NonNull String recurringOrderId, @NonNull String occurrenceDate) {
        return recurringOrderId.trim() + "|" + occurrenceDate.trim();
    }

    @NonNull
    public static String formatTime(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("vi", "VN")).format(timestamp);
    }

    private void save(List<Entry> items) {
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items, listType)).apply();
    }

    private void pushRemoteAsync(@NonNull Entry entry) {
        new Thread(() -> {
            try {
                String customerId = new AppPreferences(appContext).getCustomerId();
                if (customerId == null || customerId.trim().isEmpty()) {
                    return;
                }
                RecurringNotificationDto dto = new RecurringNotificationDto();
                dto.setCustomerId(customerId);
                dto.setRecurringOrderId(entry.recurringOrderId);
                dto.setOccurrenceDate(entry.occurrenceDate);
                dto.setType(entry.type);
                dto.setTitle(entry.title);
                dto.setBody(entry.body);
                dto.setAction(entry.action);
                dto.setOutsideApp(entry.outsideApp);
                RecurringOrderApi api = ApiClient.createService(RecurringOrderApi.class);
                retrofit2.Response<RecurringNotificationDto> response = api.createNotification(dto).execute();
                if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                    entry.id = response.body().getId();
                    save(all());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    private void pushMarkReadAsync(@NonNull String id) {
        if (id.length() != 24) {
            return;
        }
        new Thread(() -> {
            try {
                RecurringOrderApi api = ApiClient.createService(RecurringOrderApi.class);
                api.markNotificationRead(id).execute();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    private void pushMarkAllReadAsync() {
        new Thread(() -> {
            try {
                String customerId = new AppPreferences(appContext).getCustomerId();
                if (customerId == null || customerId.trim().isEmpty()) {
                    return;
                }
                RecurringOrderApi api = ApiClient.createService(RecurringOrderApi.class);
                api.markAllNotificationsRead(customerId).execute();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    @Nullable
    private static Entry fromDto(@Nullable RecurringNotificationDto dto) {
        if (dto == null) {
            return null;
        }
        Entry entry = new Entry();
        entry.id = dto.getId() != null && !dto.getId().trim().isEmpty()
                ? dto.getId()
                : UUID.randomUUID().toString();
        entry.recurringOrderId = dto.getRecurringOrderId();
        entry.occurrenceDate = dto.getOccurrenceDate();
        entry.type = dto.getType();
        entry.title = dto.getTitle();
        entry.body = dto.getBody();
        entry.action = dto.getAction();
        entry.createdAt = dto.getCreatedAt() > 0 ? dto.getCreatedAt() : System.currentTimeMillis();
        entry.read = dto.isRead();
        entry.outsideApp = dto.isOutsideApp();
        return entry;
    }

    public static class Entry {
        public String id;
        public String recurringOrderId;
        public String occurrenceDate;
        public String type;
        public String title;
        public String body;
        public String action;
        public long createdAt;
        public boolean read;
        public boolean outsideApp;

        @NonNull
        String dedupeKey() {
            return recurringOrderId + "|" + occurrenceDate + "|" + type + "|" + outsideApp;
        }
    }
}
