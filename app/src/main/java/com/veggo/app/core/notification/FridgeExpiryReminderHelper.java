package com.veggo.app.core.notification;

import com.veggo.app.data.remote.dto.FridgeItemDto;

public final class FridgeExpiryReminderHelper {

    public static final int REMIND_DAYS_BEFORE = 1;

    private FridgeExpiryReminderHelper() {
    }

    public static boolean isReminderEnabled(FridgeItemDto item) {
        return item != null && item.isRemindBeforeExpiry();
    }

    public static boolean shouldNotify(FridgeItemDto item, long diffDays) {
        if (!isReminderEnabled(item)) {
            return false;
        }
        return diffDays == 0 || diffDays == REMIND_DAYS_BEFORE;
    }

    public static String formatReminderLabel(long diffDays) {
        if (diffDays == 0) {
            return " (hôm nay)";
        }
        if (diffDays == 1) {
            return " (ngày mai)";
        }
        return " (còn " + diffDays + " ngày)";
    }
}
