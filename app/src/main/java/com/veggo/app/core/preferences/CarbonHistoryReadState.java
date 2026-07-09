package com.veggo.app.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class CarbonHistoryReadState {
    private static final String PREFS_NAME = "carbon_history_read_state";
    private static final String KEY_READ_IDS = "carbon_read_ids";
    private static final String KEY_BASELINE_INITIALIZED = "carbon_read_baseline_initialized";

    private CarbonHistoryReadState() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isUnread(Context context, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_BASELINE_INITIALIZED, false)) {
            return false;
        }
        Set<String> readIds = prefs.getStringSet(KEY_READ_IDS, null);
        if (readIds == null) {
            return false;
        }
        return !readIds.contains(itemId);
    }

    public static void markRead(Context context, String itemId) {
        if (itemId == null || itemId.trim().isEmpty() || !isUnread(context, itemId)) {
            return;
        }
        SharedPreferences prefs = prefs(context);
        Set<String> current = prefs.getStringSet(KEY_READ_IDS, null);
        Set<String> copy = new HashSet<>();
        if (current != null) {
            copy.addAll(current);
        }
        copy.add(itemId);
        prefs.edit().putStringSet(KEY_READ_IDS, copy).apply();
    }

    public static void markAllRead(Context context, Collection<String> allItemIds) {
        Set<String> readIds = new HashSet<>();
        if (allItemIds != null) {
            for (String id : allItemIds) {
                if (id != null && !id.trim().isEmpty()) {
                    readIds.add(id);
                }
            }
        }
        prefs(context).edit()
                .putStringSet(KEY_READ_IDS, readIds)
                .putBoolean(KEY_BASELINE_INITIALIZED, true)
                .apply();
    }

    public static void ensureBaselineIfNeeded(Context context, Collection<String> existingIds) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(KEY_BASELINE_INITIALIZED, false)) {
            return;
        }
        Set<String> readIds = new HashSet<>();
        if (existingIds != null) {
            for (String id : existingIds) {
                if (id != null && !id.trim().isEmpty()) {
                    readIds.add(id);
                }
            }
        }
        prefs.edit()
                .putStringSet(KEY_READ_IDS, readIds)
                .putBoolean(KEY_BASELINE_INITIALIZED, true)
                .apply();
    }
}
