package com.veggo.app.core.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.presentation.order.RecurringConfirmOrderActivity;
import com.veggo.app.presentation.order.RecurringOrderStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringConfirmationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "recurring_confirmation_channel";
    public static final String PREF_NAME = "recurring_notifications";
    public static final String PREF_KEY_SUMMARY = "recurring_confirmation_summary";
    public static final String PREF_KEY_TIME = "recurring_confirmation_time";

    private static final SimpleDateFormat STORAGE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            RecurringConfirmationScheduler.scheduleDailyCheck(context);
        }
        processRecurringConfirmations(context.getApplicationContext());
    }

    static void processRecurringConfirmations(Context context) {
        AppPreferences preferences = new AppPreferences(context);
        String customerId = preferences.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            return;
        }

        RecurringOrderStore store = new RecurringOrderStore(context);
        Calendar today = Calendar.getInstance();
        clearTime(today);
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        String todayDate = STORAGE_FORMAT.format(today.getTime());
        String tomorrowDate = STORAGE_FORMAT.format(tomorrow.getTime());
        List<String> notifyMessages = new ArrayList<>();

        for (RecurringOrderStore.RecurringOrder order : store.forCustomer(customerId)) {
            if (occursOnDate(store, order, tomorrow)) {
                handleDayBeforeDelivery(context, store, order, tomorrowDate, notifyMessages);
            }
            if (occursOnDate(store, order, today)) {
                handleDeliveryDay(context, store, order, todayDate);
            }
        }

        if (!notifyMessages.isEmpty()) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY_SUMMARY, String.join(", ", notifyMessages))
                    .putLong(PREF_KEY_TIME, System.currentTimeMillis())
                    .apply();
        }
    }

    private static void handleDayBeforeDelivery(
            Context context,
            RecurringOrderStore store,
            RecurringOrderStore.RecurringOrder order,
            String occurrenceDate,
            List<String> notifyMessages
    ) {
        RecurringOrderStore.OccurrenceState state = store.getOccurrence(order.id, occurrenceDate);
        String displayDate = formatDisplayDate(occurrenceDate);

        if (state != null && RecurringConfirmationHelper.hasOrderPlaced(state.status)) {
            long lastNotified = state.deliveryReminderNotifiedAt;
            if (!RecurringConfirmationHelper.shouldSendNotification(lastNotified)) {
                return;
            }
            store.markDeliveryReminderNotified(order.id, occurrenceDate);
            notifyMessages.add(order.name + " - giao ngày mai");
            sendNotification(
                    context,
                    order.id,
                    occurrenceDate,
                    RecurringConfirmationHelper.NOTIFY_DELIVERY,
                    context.getString(R.string.recurring_notify_delivery_title),
                    context.getString(R.string.recurring_notify_delivery_body, order.name, displayDate),
                    context.getString(R.string.recurring_notify_action_delivery)
            );
            return;
        }

        if (!RecurringConfirmationHelper.isActionable(state == null ? null : state.status)) {
            return;
        }

        long lastNotified = state == null ? 0L : lastConfirmNotifiedAt(state);
        if (!RecurringConfirmationHelper.shouldSendNotification(lastNotified)) {
            return;
        }
        store.markConfirmNotified(order.id, occurrenceDate);
        notifyMessages.add(order.name + " - cần xác nhận");
        sendNotification(
                context,
                order.id,
                occurrenceDate,
                RecurringConfirmationHelper.NOTIFY_CONFIRM,
                context.getString(R.string.recurring_notify_confirm_title),
                context.getString(R.string.recurring_notify_confirm_body, order.name, displayDate),
                context.getString(R.string.recurring_notify_action_confirm)
        );
    }

    private static void handleDeliveryDay(
            Context context,
            RecurringOrderStore store,
            RecurringOrderStore.RecurringOrder order,
            String occurrenceDate
    ) {
        RecurringOrderStore.OccurrenceState state = store.getOccurrence(order.id, occurrenceDate);
        if (state != null
                && RecurringConfirmationHelper.STATUS_PENDING.equals(state.status)
                && !RecurringConfirmationHelper.hasOrderPlaced(state.status)) {
            store.markOccurrenceSkipped(order.id, occurrenceDate);
            RecurringInAppNotificationStore inAppStore = new RecurringInAppNotificationStore(context);
            inAppStore.addInAppNotification(
                    order.id,
                    occurrenceDate,
                    RecurringInAppNotificationStore.TYPE_SKIPPED,
                    context.getString(R.string.recurring_notify_skipped_title),
                    context.getString(
                            R.string.recurring_notify_skipped_body,
                            order.name,
                            formatDisplayDate(occurrenceDate)
                    ),
                    context.getString(R.string.recurring_notify_action_schedule)
            );
        }
    }

    private static long lastConfirmNotifiedAt(RecurringOrderStore.OccurrenceState state) {
        if (state.confirmNotifiedAt > 0) {
            return state.confirmNotifiedAt;
        }
        return state.notifiedAt;
    }

    private static boolean occursOnDate(
            RecurringOrderStore store,
            RecurringOrderStore.RecurringOrder order,
            Calendar date
    ) {
        return store.orderOccursOnDate(order, STORAGE_FORMAT.format(date.getTime()));
    }

    private static void sendNotification(
            Context context,
            String orderId,
            String occurrenceDate,
            String type,
            String title,
            String body,
            String action
    ) {
        createNotificationChannel(context);

        new RecurringInAppNotificationStore(context).addPushNotification(
                orderId,
                occurrenceDate,
                mapInAppType(type),
                title,
                body,
                action
        );

        Intent intent = new Intent(context, RecurringConfirmOrderActivity.class);
        intent.putExtra(RecurringConfirmOrderActivity.EXTRA_RECURRING_ORDER_ID, orderId);
        intent.putExtra(RecurringConfirmOrderActivity.EXTRA_OCCURRENCE_DATE, occurrenceDate);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = (RecurringConfirmationHelper.occurrenceKey(orderId, occurrenceDate) + "|" + type).hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_order_recurring_option)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private static String mapInAppType(String notifyType) {
        if (RecurringConfirmationHelper.NOTIFY_DELIVERY.equals(notifyType)) {
            return RecurringInAppNotificationStore.TYPE_DELIVERY;
        }
        return RecurringInAppNotificationStore.TYPE_CONFIRM;
    }

    private static String formatDisplayDate(String occurrenceDate) {
        try {
            return DISPLAY_FORMAT.format(STORAGE_FORMAT.parse(occurrenceDate));
        } catch (Exception exception) {
            return occurrenceDate;
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.recurring_notify_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.recurring_notify_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
