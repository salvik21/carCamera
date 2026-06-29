package com.example.cardvr.cloud;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.example.cardvr.database.CloudUploadTaskEntity;
import com.example.cardvr.power.BatteryMonitor;
import com.example.cardvr.power.ChargingStateManager;

public final class NetworkPolicyManager {
    public enum Decision { ALLOW, WAIT_NETWORK, WAIT_WIFI, WAIT_LIMIT, WAIT_POWER }

    private final Context context;
    private final CloudSettingsRepository settings;
    private final TrafficUsageManager traffic;

    public NetworkPolicyManager(Context context) {
        this.context = context.getApplicationContext();
        settings = new CloudSettingsRepository(context);
        traffic = new TrafficUsageManager(context);
    }

    public Decision canUpload(CloudUploadTaskEntity task) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return Decision.WAIT_NETWORK;
        }
        boolean wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean mobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        if (settings.networkMode() == CloudSettingsRepository.NetworkMode.NO_INTERNET) {
            return Decision.WAIT_NETWORK;
        }
        if (!wifi && task.requiresWifi) return Decision.WAIT_WIFI;
        if (mobile && !task.allowMobileData) return Decision.WAIT_WIFI;
        if (mobile && settings.networkMode() == CloudSettingsRepository.NetworkMode.WIFI_ONLY) {
            return Decision.WAIT_WIFI;
        }
        if (mobile && traffic.remainingMobileBytes() < task.sizeBytes
                && task.priority > 1) {
            return Decision.WAIT_LIMIT;
        }
        if (task.sizeBytes > 100L * CloudSettingsRepository.MIB && task.priority > 2) {
            ChargingStateManager.ChargingState battery = BatteryMonitor.readCurrentState(context);
            if (!battery.isCharging() && battery.getBatteryPercent() >= 0
                    && battery.getBatteryPercent() < 20) {
                return Decision.WAIT_POWER;
            }
        }
        return Decision.ALLOW;
    }
}
