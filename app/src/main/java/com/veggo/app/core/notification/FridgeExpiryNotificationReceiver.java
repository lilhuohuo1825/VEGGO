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

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.dto.FridgeItemDto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FridgeExpiryNotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "fridge_expiry_channel";
    public static final String PREF_KEY_EXPIRY_ITEMS = "fridge_expiry_items_json";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reschedule alarm ngày hôm sau (vì setExactAndAllowWhileIdle không tự lặp)
        if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            FridgeExpiryScheduler.scheduleDailyCheck(context);
        }
        // Chạy trên background thread
        new Thread(() -> checkExpiringItems(context)).start();
    }

    private void checkExpiringItems(Context context) {
        try {
            String customerId = new AppPreferences(context).getCustomerId();
            if (customerId == null || customerId.isEmpty()) return;

            FridgeApi api = ApiClient.createService(FridgeApi.class);
            retrofit2.Response<List<FridgeItemDto>> response = api.getFridgeItems(customerId).execute();

            if (!response.isSuccessful() || response.body() == null) return;

            List<FridgeItemDto> items = response.body();
            List<String> expiringNames = new ArrayList<>();

            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            int tYear = tomorrow.get(Calendar.YEAR);
            int tMonth = tomorrow.get(Calendar.MONTH);
            int tDay = tomorrow.get(Calendar.DAY_OF_MONTH);

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat fmtAlt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            for (FridgeItemDto item : items) {
                if (item.getExpiryDate() == null || item.getExpiryDate().isEmpty()) continue;
                try {
                    java.util.Date expiry;
                    try {
                        expiry = fmt.parse(item.getExpiryDate().replace("Z", "").replace(".000", ""));
                    } catch (Exception e) {
                        expiry = fmtAlt.parse(item.getExpiryDate().substring(0, 10));
                    }
                    if (expiry == null) continue;

                    // Tính số ngày còn lại đến khi hết hạn
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    Calendar expDay = Calendar.getInstance();
                    expDay.setTime(expiry);
                    expDay.set(Calendar.HOUR_OF_DAY, 0);
                    expDay.set(Calendar.MINUTE, 0);
                    expDay.set(Calendar.SECOND, 0);
                    expDay.set(Calendar.MILLISECOND, 0);

                    long diffMs = expDay.getTimeInMillis() - today.getTimeInMillis();
                    long diffDays = diffMs / (1000 * 60 * 60 * 24);

                    // Cảnh báo khi bật nhắc hạn và còn 0–1 ngày (ngày mai / hôm nay hết hạn)
                    if (FridgeExpiryReminderHelper.shouldNotify(item, diffDays)) {
                        String label = item.getName() != null ? item.getName() : "Nguyên liệu";
                        expiringNames.add(label + FridgeExpiryReminderHelper.formatReminderLabel(diffDays));
                    }
                } catch (Exception ignored) {}
            }

            if (!expiringNames.isEmpty()) {
                // Lưu vào SharedPreferences để hiển thị trong tab Khác
                saveExpiryNotification(context, expiringNames);
                // Gửi push notification
                sendPushNotification(context, expiringNames);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveExpiryNotification(Context context, List<String> names) {
        String joined = String.join(", ", names);
        context.getSharedPreferences("fridge_notifications", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_EXPIRY_ITEMS, joined)
                .putLong("fridge_expiry_time", System.currentTimeMillis())
                .apply();
    }

    private void sendPushNotification(Context context, List<String> names) {
        createNotificationChannel(context);

        String title = "⚠️ Nguyên liệu sắp hết hạn!";
        String body;
        if (names.size() == 1) {
            body = names.get(0) + " - Hãy sử dụng sớm!";
        } else {
            body = names.size() + " nguyên liệu sắp hết hạn: " + String.join(", ", names);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_refrigerator)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {
            // Quyền POST_NOTIFICATIONS chưa được cấp
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc hạn nguyên liệu tủ lạnh",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi nguyên liệu trong tủ lạnh sắp hết hạn");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
