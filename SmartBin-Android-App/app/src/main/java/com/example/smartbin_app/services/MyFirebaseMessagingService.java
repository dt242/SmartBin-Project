package com.example.smartbin_app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.smartbin_app.ui.MainActivity;
import com.example.smartbin_app.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String eventCode = "";
        String binName = "";

        if (!remoteMessage.getData().isEmpty()) {
            eventCode = remoteMessage.getData().get("eventCode");
            binName = remoteMessage.getData().get("binName");
        }

        if (eventCode != null && !eventCode.isEmpty()) {
            String title = getString(R.string.app_name);
            String body = "";

            switch (eventCode) {
                case "fire_alert":
                    title = getString(R.string.push_fire_alert_title);
                    body = getString(R.string.push_fire_alert_body) + " " + binName;
                    break;
                case "fire_cleared":
                    title = getString(R.string.push_fire_cleared_title);
                    body = getString(R.string.push_fire_cleared_body) + " " + binName;
                    break;
                case "gas_alert":
                    title = getString(R.string.push_gas_alert_title);
                    body = getString(R.string.push_gas_alert_body) + " " + binName;
                    break;
                case "gas_cleared":
                    title = getString(R.string.push_gas_cleared_title);
                    body = getString(R.string.push_gas_cleared_body) + " " + binName;
                    break;
                case "bin_full":
                    title = getString(R.string.push_full_alert_title);
                    body = getString(R.string.push_full_alert_body) + " " + binName;
                    break;
                case "bin_emptied":
                    title = getString(R.string.push_emptied_title);
                    body = getString(R.string.push_emptied_body) + " " + binName;
                    break;
            }
            showNotification(title, body);
        }
    }

    private void showNotification(String title, String message) {
        String channelId = getString(R.string.notification_channel_id);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        manager.notify(new Random().nextInt(), builder.build());
    }
}
