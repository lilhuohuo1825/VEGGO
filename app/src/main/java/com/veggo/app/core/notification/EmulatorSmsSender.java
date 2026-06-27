package com.veggo.app.core.notification;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.veggo.app.MainActivity;
import com.veggo.app.R;

public final class EmulatorSmsSender {
    public static final int REQUEST_SEND_SMS = 7312;
    private static final String CHANNEL_ID = "veggo_otp_heads_up_channel";
    private static String pendingMessage;

    private EmulatorSmsSender() {}

    public static boolean send(Activity activity, String message) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingMessage = message;
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_SEND_SMS
            );
            Toast.makeText(activity, "Vui lòng cấp quyền thông báo rồi bấm Gửi lại mã", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            pendingMessage = null;
            createChannel(activity);
            Intent intent = new Intent(activity, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    activity,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_phone)
                    .setContentTitle("Tin nhắn mới")
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);
            NotificationManagerCompat.from(activity)
                    .notify((int) System.currentTimeMillis(), builder.build());
            return true;
        } catch (Exception exception) {
            Toast.makeText(activity, "Không thể hiển thị thông báo OTP", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static void handlePermissionResult(Activity activity, int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_SEND_SMS || pendingMessage == null) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String message = pendingMessage;
            pendingMessage = null;
            send(activity, message);
            return;
        }
        pendingMessage = null;
        Toast.makeText(activity, "Không thể hiển thị OTP vì chưa được cấp quyền thông báo", Toast.LENGTH_SHORT).show();
    }

    private static void createChannel(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Mã OTP VEGGO",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Thông báo mã OTP hiển thị từ mép trên màn hình");
        channel.enableVibration(true);
        NotificationManager manager = activity.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
