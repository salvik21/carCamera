package com.example.cardvr.cloud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public final class CloudNotificationManager {
    public static final String CHANNEL_ID = "cloud_upload_channel";
    private final NotificationManager manager;

    public CloudNotificationManager(Context context) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Загрузка в облако", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Состояние очереди загрузки Google Drive");
            manager.createNotificationChannel(channel);
        }
    }
}
