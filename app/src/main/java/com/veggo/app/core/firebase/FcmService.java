package com.veggo.app.core.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.google.firebase.messaging.FirebaseMessagingService;

public class FcmService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "veggo_push_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: Send this token to the VEGGO backend after auth/profile is implemented.
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = "Thông báo VEGGO";
        String body = "";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }
            if (message.getNotification().getBody() != null) {
                body = message.getNotification().getBody();
            }
        }
        if (message.getData().containsKey("title")) {
            title = message.getData().get("title");
        }
        if (message.getData().containsKey("body")) {
            body = message.getData().get("body");
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        createChannel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_profile_leaf)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        try {
            NotificationManagerCompat.from(this)
                    .notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS may not be granted yet on Android 13+.
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Thông báo VEGGO",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Thông báo đơn hàng và cập nhật từ VEGGO");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
