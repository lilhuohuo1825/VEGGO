package com.veggo.app.core.utils;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

public final class DeliveryTimeUtils {
    public static final int OPEN_HOUR = 7;
    public static final int CLOSE_HOUR = 22;
    public static final int WINDOW_HOURS = 2;

    private DeliveryTimeUtils() {
    }

    public static final class DeliveryWindow {
        public final Calendar start;
        public final Calendar end;

        public DeliveryWindow(@NonNull Calendar start, @NonNull Calendar end) {
            this.start = start;
            this.end = end;
        }

        @NonNull
        public String displayText() {
            return DeliveryTimeUtils.formatDeliveryWindow(start, end);
        }
    }

    @NonNull
    public static DeliveryWindow calculateFastDelivery(int travelMinutes) {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.MINUTE, Math.max(0, travelMinutes));
        start = clampToOpeningHour(start);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);

        if (minutesOfDay(start) >= CLOSE_HOUR * 60 || !fitsSameDayWindow(start, end)) {
            start = nextDayOpening(travelMinutes);
            end = (Calendar) start.clone();
            end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);
        }

        return new DeliveryWindow(start, end);
    }

    /**
     * Scheduled delivery must always start after the fast-delivery window ends.
     */
    @NonNull
    public static Calendar calculateScheduledMinimum(int travelMinutes) {
        DeliveryWindow fast = calculateFastDelivery(travelMinutes);
        Calendar minimum = (Calendar) fast.end.clone();
        return normalizeScheduledStart(minimum, travelMinutes);
    }

    @NonNull
    public static DeliveryWindow defaultScheduledWindow(int travelMinutes) {
        Calendar start = calculateScheduledMinimum(travelMinutes);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);
        return new DeliveryWindow(start, end);
    }

    @NonNull
    public static DeliveryWindow resolveScheduledPick(int travelMinutes, int hour, int minute) {
        Calendar minimum = calculateScheduledMinimum(travelMinutes);
        Calendar best = null;
        Calendar baseDay = Calendar.getInstance();

        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
            Calendar candidate = (Calendar) baseDay.clone();
            candidate.add(Calendar.DAY_OF_MONTH, dayOffset);
            setTime(candidate, hour, minute);
            if (!isValidScheduledWindow(candidate, travelMinutes)) {
                continue;
            }
            if (best == null || candidate.before(best)) {
                best = candidate;
            }
        }

        if (best == null || best.before(minimum)) {
            return defaultScheduledWindow(travelMinutes);
        }
        Calendar end = (Calendar) best.clone();
        end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);
        return new DeliveryWindow(best, end);
    }

    public static boolean isValidScheduledWindow(@NonNull Calendar start, int travelMinutes) {
        Calendar minimum = calculateScheduledMinimum(travelMinutes);
        if (start.before(minimum)) {
            return false;
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);
        return fitsSameDayWindow(start, end);
    }

    @NonNull
    public static String formatDeliveryWindow(@NonNull Calendar start, @NonNull Calendar end) {
        return String.format(
                Locale.getDefault(),
                "Dự kiến nhận hàng %s, %02d:%02d - %02d:%02d",
                dayLabel(start),
                start.get(Calendar.HOUR_OF_DAY),
                start.get(Calendar.MINUTE),
                end.get(Calendar.HOUR_OF_DAY),
                end.get(Calendar.MINUTE)
        );
    }

    @NonNull
    public static String formatMinimumScheduleHint(int travelMinutes) {
        return formatDeliveryWindow(
                calculateScheduledMinimum(travelMinutes),
                windowEnd(calculateScheduledMinimum(travelMinutes))
        );
    }

    @NonNull
    private static Calendar normalizeScheduledStart(@NonNull Calendar candidate, int travelMinutes) {
        Calendar minimum = (Calendar) candidate.clone();
        int guard = 0;
        while (!fitsSameDayWindow(minimum, windowEnd(minimum)) && guard < 14) {
            minimum.add(Calendar.DAY_OF_MONTH, 1);
            setTime(minimum, OPEN_HOUR, 0);
            minimum.add(Calendar.MINUTE, Math.max(0, travelMinutes));
            guard++;
        }
        return minimum;
    }

    @NonNull
    private static Calendar windowEnd(@NonNull Calendar start) {
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.HOUR_OF_DAY, WINDOW_HOURS);
        return end;
    }

    private static boolean fitsSameDayWindow(@NonNull Calendar start, @NonNull Calendar end) {
        if (minutesOfDay(start) < OPEN_HOUR * 60) {
            return false;
        }
        if (minutesOfDay(start) >= CLOSE_HOUR * 60) {
            return false;
        }
        return isSameDay(start, end) && minutesOfDay(end) <= CLOSE_HOUR * 60;
    }

    @NonNull
    private static String dayLabel(@NonNull Calendar time) {
        Calendar today = Calendar.getInstance();
        if (isSameDay(time, today)) {
            return "hôm nay";
        }
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        if (isSameDay(time, tomorrow)) {
            return "ngày mai";
        }
        return String.format(
                Locale.getDefault(),
                "ngày %02d/%02d",
                time.get(Calendar.DAY_OF_MONTH),
                time.get(Calendar.MONTH) + 1
        );
    }

    private static boolean isSameDay(@NonNull Calendar left, @NonNull Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    @NonNull
    private static Calendar clampToOpeningHour(@NonNull Calendar time) {
        if (minutesOfDay(time) < OPEN_HOUR * 60) {
            Calendar clamped = (Calendar) time.clone();
            setTime(clamped, OPEN_HOUR, 0);
            return clamped;
        }
        return time;
    }

    @NonNull
    private static Calendar nextDayOpening(int travelMinutes) {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, 1);
        setTime(start, OPEN_HOUR, 0);
        start.add(Calendar.MINUTE, Math.max(0, travelMinutes));
        return start;
    }

    private static void setTime(@NonNull Calendar calendar, int hour, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static int minutesOfDay(@NonNull Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }
}
