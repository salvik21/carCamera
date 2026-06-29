package com.example.cardvr.events;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.cardvr.R;
import com.example.cardvr.database.EventEntity;
import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;
import com.example.cardvr.ui.EmergencyActivity;
import com.example.cardvr.ui.EventsActivity;

public final class EventNotificationManager {
    public static final String QUIET_CHANNEL_ID = "event_quiet_channel";
    public static final String ALERT_CHANNEL_ID = "event_alert_channel";
    private static final int BASE_ID = 2_000;

    private final Context context;
    private final NotificationManager manager;

    public EventNotificationManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.manager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    public void notifyEvent(@NonNull EventEntity event) {
        boolean alert = event.type == EventType.POSSIBLE_CRASH
                || event.severity == EventSeverity.HIGH
                || event.severity == EventSeverity.CRITICAL;
        Intent openIntent = alert
                ? new Intent(context, EmergencyActivity.class)
                : new Intent(context, EventsActivity.class);
        openIntent.putExtra(EmergencyActivity.EXTRA_EVENT_ID, event.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, (int) event.id,
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, alert ? ALERT_CHANNEL_ID : QUIET_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_recording)
                .setContentTitle(title(event))
                .setContentText(event.explanation)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(event.explanation))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(alert ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        if (alert) {
            builder.setFullScreenIntent(open, true)
                    .setVibrate(new long[]{0, 400, 200, 400});
        }
        Notification notification = builder.build();
        manager.notify(BASE_ID + (int) (event.id % 500), notification);
    }

    private String title(EventEntity event) {
        if (event.type == EventType.POSSIBLE_CRASH) return "Обнаружена возможная авария";
        if (event.type == EventType.IMPACT) return "Обнаружен сильный удар";
        if (event.type == EventType.HARD_BRAKING) return "Резкое торможение";
        if (event.type == EventType.SUDDEN_STOP) return "Резкая остановка";
        if (event.type == EventType.POTHOLE) return "Яма на дороге";
        if (event.type == EventType.PHONE_MOVED) return "Телефон смещён";
        return "Событие видеорегистратора";
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel quiet = new NotificationChannel(QUIET_CHANNEL_ID,
                "События регистратора", NotificationManager.IMPORTANCE_DEFAULT);
        quiet.setDescription("Тихие уведомления о событиях поездки");
        manager.createNotificationChannel(quiet);
        NotificationChannel alert = new NotificationChannel(ALERT_CHANNEL_ID,
                "Аварийные события", NotificationManager.IMPORTANCE_HIGH);
        alert.setDescription("Звук и вибрация для возможной аварии или сильного удара");
        alert.enableVibration(true);
        alert.setVibrationPattern(new long[]{0, 400, 200, 400});
        alert.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
        manager.createNotificationChannel(alert);
    }
}
