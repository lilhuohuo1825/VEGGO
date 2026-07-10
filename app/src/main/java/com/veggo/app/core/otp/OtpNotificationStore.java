package com.veggo.app.core.otp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory store for OTP codes received via push/SMS notifications.
 */
public final class OtpNotificationStore {

    public static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    public interface Listener {
        void onOtpReceived(@NonNull Entry entry);
    }

    public static final class Entry {
        public final String message;
        public final String otp;
        public final long receivedAtMs;

        Entry(@NonNull String message, @NonNull String otp, long receivedAtMs) {
            this.message = message;
            this.otp = otp;
            this.receivedAtMs = receivedAtMs;
        }
    }

    private static volatile Entry latest;
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private OtpNotificationStore() {
    }

    public static void save(@NonNull String message, @NonNull String otp) {
        if (!OtpMessageParser.isValidOtp(otp)) {
            return;
        }
        Entry entry = new Entry(message, otp, System.currentTimeMillis());
        latest = entry;
        for (Listener listener : listeners) {
            listener.onOtpReceived(entry);
        }
    }

    @Nullable
    public static Entry getLatest() {
        return latest;
    }

    @Nullable
    public static Entry consumeLatest(long maxAgeMs) {
        Entry entry = latest;
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.receivedAtMs > maxAgeMs) {
            latest = null;
            return null;
        }
        return entry;
    }

    public static void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public static void clear() {
        latest = null;
    }
}
