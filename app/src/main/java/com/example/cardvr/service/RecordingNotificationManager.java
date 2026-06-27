package com.example.cardvr.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.cardvr.R;
import com.example.cardvr.recording.RecordingStateManager;
import com.example.cardvr.ui.MainActivity;

import java.util.Locale;

public final class RecordingNotificationManager {

    public static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "recording_channel";

    private final Context appContext;
    private final NotificationManager notificationManager;

    public RecordingNotificationManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        notificationManager = (NotificationManager) appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        createChannel();
    }

    @NonNull
    public Notification createNotification(@NonNull RecordingStateManager.State state) {
        Intent contentIntent = new Intent(appContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                appContext,
                1,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent protectPendingIntent = PendingIntent.getService(
                appContext,
                2,
                new Intent(appContext, RecordingService.class)
                        .setAction(RecordingService.ACTION_PROTECT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent stopPendingIntent = PendingIntent.getService(
                appContext,
                3,
                new Intent(appContext, RecordingService.class)
                        .setAction(RecordingService.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String chargingText = state.isCharging()
                ? appContext.getString(R.string.charging_connected)
                : appContext.getString(R.string.charging_disconnected);
        String contentText = appContext.getString(
                R.string.notification_details,
                formatDuration(state.getElapsedMillis()),
                state.getCameraName(),
                chargingText
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                appContext,
                CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_stat_recording)
                .setContentTitle(appContext.getString(R.string.notification_title))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, appContext.getString(R.string.notification_protect),
                        protectPendingIntent)
                .addAction(0, appContext.getString(R.string.notification_stop),
                        stopPendingIntent);

        if (state.getStartedAtElapsedRealtime() > 0L) {
            builder.setWhen(System.currentTimeMillis() - state.getElapsedMillis())
                    .setUsesChronometer(true)
                    .setShowWhen(true);
        }
        return builder.build();
    }

    public void update(@NonNull RecordingStateManager.State state) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(state));
    }

    public void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void showStorageError() {
        Notification notification = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_recording)
                .setContentTitle(appContext.getString(R.string.storage_error_title))
                .setContentText(appContext.getString(R.string.storage_error_message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(appContext.getString(R.string.notification_channel_description));
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    @NonNull
    public static String formatDuration(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
