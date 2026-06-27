package com.example.cardvr.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class BatteryMonitor {

    private final Context appContext;
    private final ChargingStateManager chargingStateManager;
    private boolean registered;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            chargingStateManager.updateState(readState(intent));
        }
    };

    public BatteryMonitor(@NonNull Context context) {
        appContext = context.getApplicationContext();
        chargingStateManager = ChargingStateManager.getInstance();
    }

    public void start() {
        if (registered) {
            return;
        }
        registered = true;
        Intent stickyIntent = ContextCompat.registerReceiver(
                appContext,
                batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        if (stickyIntent != null) {
            chargingStateManager.updateState(readState(stickyIntent));
        }
    }

    public void stop() {
        if (!registered) {
            return;
        }
        registered = false;
        appContext.unregisterReceiver(batteryReceiver);
    }

    @NonNull
    public static ChargingStateManager.ChargingState readCurrentState(
            @NonNull Context context
    ) {
        Intent intent = context.registerReceiver(
                null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );
        return intent == null
                ? new ChargingStateManager.ChargingState(false, -1)
                : readState(intent);
    }

    private static ChargingStateManager.ChargingState readState(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
                || plugged != 0;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        return new ChargingStateManager.ChargingState(charging, percent);
    }
}
