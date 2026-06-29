package com.example.cardvr.power;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

public final class ThermalMonitor {
    public enum Level { NORMAL, WARM, HOT, CRITICAL }

    private final Context context;

    public ThermalMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    public Level currentLevel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Level.NORMAL;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        int status = pm == null ? PowerManager.THERMAL_STATUS_NONE : pm.getCurrentThermalStatus();
        if (status >= PowerManager.THERMAL_STATUS_CRITICAL) return Level.CRITICAL;
        if (status >= PowerManager.THERMAL_STATUS_SEVERE) return Level.HOT;
        if (status >= PowerManager.THERMAL_STATUS_LIGHT) return Level.WARM;
        return Level.NORMAL;
    }
}
