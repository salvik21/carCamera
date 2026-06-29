package com.example.cardvr.cloud;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.example.cardvr.database.TrafficUsageEntity;

import java.util.Calendar;

public final class TrafficUsageManager {
    private final CloudUploadRepository repository;
    private final CloudSettingsRepository settings;
    private final Context context;

    public TrafficUsageManager(Context context) {
        this.context = context.getApplicationContext();
        repository = new CloudUploadRepository(context);
        settings = new CloudSettingsRepository(context);
    }

    public synchronized void recordUploaded(long bytes) {
        int month = currentYearMonth();
        TrafficUsageEntity usage = repository.getTraffic(month);
        if (usage == null) {
            usage = new TrafficUsageEntity();
            usage.yearMonth = month;
            usage.monthlyLimitBytes = settings.monthlyLimitBytes();
            usage.resetDay = settings.resetDay();
        }
        if (isWifi()) usage.wifiBytes += bytes;
        else usage.mobileBytes += bytes;
        usage.updatedAt = System.currentTimeMillis();
        repository.saveTraffic(usage);
    }

    public long remainingMobileBytes() {
        TrafficUsageEntity usage = repository.getTraffic(currentYearMonth());
        long limit = settings.monthlyLimitBytes();
        long used = usage == null ? 0 : usage.mobileBytes;
        return Math.max(0, limit - used);
    }

    public TrafficUsageEntity currentUsage() {
        return repository.getTraffic(currentYearMonth());
    }

    public static int currentYearMonth() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1;
    }

    private boolean isWifi() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
