package com.veggo.app.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.veggo.app.data.remote.dto.WalletTransactionDto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WalletTransactionReadState {
    private static final String PREFS_NAME = "wallet_history_read_state";
    private static final String KEY_LAST_READ_ALL_TS = "wallet_last_read_all_ts";
    private static final String KEY_READ_TX_IDS = "wallet_read_tx_ids";
    private static final String KEY_READ_BASELINE_INITIALIZED = "wallet_read_baseline_initialized";

    private WalletTransactionReadState() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int countUnread(Context context, List<WalletTransactionDto> transactions) {
        ensureBaselineIfNeeded(context, transactions);
        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (WalletTransactionDto tx : transactions) {
            if (isUnread(context, tx)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isUnread(Context context, WalletTransactionDto tx) {
        if (tx == null) return false;
        SharedPreferences prefs = prefs(context);
        long ts = parseIsoMillis(tx.getCreatedAt());
        long lastReadAll = prefs.getLong(KEY_LAST_READ_ALL_TS, 0L);
        Set<String> readIds = prefs.getStringSet(KEY_READ_TX_IDS, null);
        String txId = safeTxId(tx);
        if (txId != null && readIds != null && readIds.contains(txId)) {
            return false;
        }
        return ts > lastReadAll;
    }

    public static void markRead(Context context, WalletTransactionDto tx) {
        if (tx == null || !isUnread(context, tx)) return;
        SharedPreferences prefs = prefs(context);
        String txId = safeTxId(tx);
        if (txId == null || txId.trim().isEmpty()) {
            long current = prefs.getLong(KEY_LAST_READ_ALL_TS, 0L);
            prefs.edit()
                    .putLong(KEY_LAST_READ_ALL_TS, Math.max(current, System.currentTimeMillis()))
                    .apply();
            return;
        }
        Set<String> current = prefs.getStringSet(KEY_READ_TX_IDS, null);
        Set<String> copy = new HashSet<>();
        if (current != null) copy.addAll(current);
        copy.add(txId);
        prefs.edit().putStringSet(KEY_READ_TX_IDS, copy).apply();
    }

    public static void markAllRead(Context context) {
        prefs(context).edit()
                .putLong(KEY_LAST_READ_ALL_TS, System.currentTimeMillis())
                .putBoolean(KEY_READ_BASELINE_INITIALIZED, true)
                .remove(KEY_READ_TX_IDS)
                .apply();
    }

    public static void ensureBaselineIfNeeded(Context context, List<WalletTransactionDto> transactions) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(KEY_READ_BASELINE_INITIALIZED, false)) {
            return;
        }
        long baseline = System.currentTimeMillis();
        if (transactions != null) {
            for (WalletTransactionDto tx : transactions) {
                baseline = Math.max(baseline, parseIsoMillis(tx.getCreatedAt()));
            }
        }
        prefs.edit()
                .putLong(KEY_LAST_READ_ALL_TS, baseline)
                .putBoolean(KEY_READ_BASELINE_INITIALIZED, true)
                .apply();
    }

    public static String safeTxId(WalletTransactionDto tx) {
        String id = tx.getTransactionId();
        if (id != null && !id.trim().isEmpty()) {
            return id.trim();
        }
        String createdAt = tx.getCreatedAt();
        String desc = tx.getDescription();
        if (createdAt != null && desc != null) {
            return (createdAt + "|" + desc).trim();
        }
        return null;
    }

    private static long parseIsoMillis(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) return 0L;
        try {
            String cleanDate = isoDate.replace("Z", "+0000");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
            Date date = df.parse(cleanDate);
            return date == null ? 0L : date.getTime();
        } catch (Exception e) {
            try {
                String cleanDate = isoDate.replace("Z", "+00:00");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                Date date = df.parse(cleanDate);
                return date == null ? 0L : date.getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }
}
