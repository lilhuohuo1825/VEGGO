package com.veggo.app.core.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.core.utils.DeliveryTimeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecurringConfirmationHelper {

    public static final int REMIND_DAYS_BEFORE = 1;

    public static final String STATUS_PENDING = "pending_confirmation";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_SKIPPED = "skipped";

    public static final String NOTIFY_CONFIRM = "confirm";
    public static final String NOTIFY_DELIVERY = "delivery";

    private static final Pattern SLOT_PATTERN = Pattern.compile(
            "(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})"
    );
    private static final SimpleDateFormat STORAGE_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private RecurringConfirmationHelper() {
    }

    @NonNull
    public static String occurrenceKey(@NonNull String orderId, @NonNull String date) {
        return orderId.trim() + "|" + date.trim();
    }

    public static boolean isActionable(@Nullable String status) {
        return status == null || STATUS_PENDING.equals(status);
    }

    public static boolean hasOrderPlaced(@Nullable String status) {
        return STATUS_COMPLETED.equals(status);
    }

    public static boolean isTerminal(@Nullable String status) {
        return STATUS_CANCELLED.equals(status) || STATUS_SKIPPED.equals(status);
    }

    public static int daysUntilDelivery(@NonNull String occurrenceDate) {
        Calendar today = Calendar.getInstance();
        clearTime(today);
        Calendar target = parseDate(occurrenceDate);
        if (target == null) {
            return -1;
        }
        clearTime(target);
        long diffMs = target.getTimeInMillis() - today.getTimeInMillis();
        return (int) (diffMs / (24L * 60L * 60L * 1000L));
    }

    /** Có thể xác nhận/huỷ trước hoặc trong ngày giao (không bắt buộc đợi D-1). */
    public static boolean isConfirmWindowOpen(@NonNull String occurrenceDate) {
        return daysUntilDelivery(occurrenceDate) >= 0;
    }

    public static boolean canCancelPlacedOrder(@NonNull String occurrenceDate) {
        return daysUntilDelivery(occurrenceDate) >= 0;
    }

    public static boolean isVisibleOnCalendar(@Nullable String status) {
        return status == null
                || STATUS_PENDING.equals(status)
                || STATUS_COMPLETED.equals(status);
    }

    public static boolean shouldSendNotification(long lastNotifiedAt) {
        if (lastNotifiedAt <= 0) {
            return true;
        }
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastNotifiedAt);
        Calendar today = Calendar.getInstance();
        return last.get(Calendar.YEAR) != today.get(Calendar.YEAR)
                || last.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR);
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Nullable
    public static DeliveryTimeUtils.DeliveryWindow buildDeliveryWindow(
            @NonNull String deliveryDate,
            @NonNull String deliverySlot
    ) {
        Calendar start = parseDate(deliveryDate);
        if (start == null) {
            return null;
        }
        Matcher matcher = SLOT_PATTERN.matcher(deliverySlot == null ? "" : deliverySlot.trim());
        if (!matcher.find()) {
            start.set(Calendar.HOUR_OF_DAY, 7);
            start.set(Calendar.MINUTE, 30);
        } else {
            start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(1)));
            start.set(Calendar.MINUTE, Integer.parseInt(matcher.group(2)));
        }
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        if (matcher.groupCount() >= 4) {
            end.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(3)));
            end.set(Calendar.MINUTE, Integer.parseInt(matcher.group(4)));
        } else {
            end.add(Calendar.HOUR_OF_DAY, DeliveryTimeUtils.WINDOW_HOURS);
        }
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        return new DeliveryTimeUtils.DeliveryWindow(start, end);
    }

    @Nullable
    private static Calendar parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(STORAGE_DATE.parse(value.trim()));
            return calendar;
        } catch (ParseException exception) {
            return null;
        }
    }
}
