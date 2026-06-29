package com.example.cardvr.cloud;

import android.content.Context;
import android.content.SharedPreferences;

public final class CloudSettingsRepository {
    private static final String PREFS = "cloud_settings";
    private static final String AUTO_MODE = "auto_mode";
    private static final String NETWORK_MODE = "network_mode";
    private static final String MOBILE_PLAN = "mobile_plan";
    private static final String MONTHLY_LIMIT = "monthly_limit";
    private static final String RESET_DAY = "reset_day";
    private static final String LOCAL_COPY = "local_copy";
    private static final String LARGE_UPLOAD_POWER = "large_upload_power";
    public static final long MIB = 1024L * 1024L;
    public static final long GIB = 1024L * MIB;

    public enum AutoUploadMode { CRASH_ONLY, CRASH_AND_IMPACT, ALL_PROTECTED, NOTHING }
    public enum NetworkMode { WIFI_ONLY, WIFI_AND_MOBILE, MOBILE_ONLY_CRITICAL, NO_INTERNET }
    public enum MobilePlan { UNLIMITED, LIMITED, UNKNOWN }
    public enum LocalCopyMode { ALWAYS_KEEP, ASK_DELETE, DELETE_NORMAL_AFTER_UPLOAD }
    public enum LargeUploadPowerMode { CHARGING_ONLY, MIN_20, MIN_40, NO_LIMIT }

    private final SharedPreferences preferences;

    public CloudSettingsRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public AutoUploadMode autoUploadMode() {
        return AutoUploadMode.valueOf(preferences.getString(AUTO_MODE,
                AutoUploadMode.CRASH_AND_IMPACT.name()));
    }

    public void setAutoUploadMode(AutoUploadMode mode) {
        preferences.edit().putString(AUTO_MODE, mode.name()).apply();
    }

    public NetworkMode networkMode() {
        return NetworkMode.valueOf(preferences.getString(NETWORK_MODE,
                NetworkMode.WIFI_ONLY.name()));
    }

    public void setNetworkMode(NetworkMode mode) {
        preferences.edit().putString(NETWORK_MODE, mode.name()).apply();
    }

    public MobilePlan mobilePlan() {
        return MobilePlan.valueOf(preferences.getString(MOBILE_PLAN,
                MobilePlan.UNKNOWN.name()));
    }

    public void setMobilePlan(MobilePlan plan) {
        preferences.edit().putString(MOBILE_PLAN, plan.name()).apply();
    }

    public long monthlyLimitBytes() {
        return preferences.getLong(MONTHLY_LIMIT, GIB);
    }

    public void setMonthlyLimitBytes(long bytes) {
        preferences.edit().putLong(MONTHLY_LIMIT, Math.max(0, bytes)).apply();
    }

    public int resetDay() {
        return preferences.getInt(RESET_DAY, 1);
    }

    public void setResetDay(int day) {
        preferences.edit().putInt(RESET_DAY, Math.max(1, Math.min(28, day))).apply();
    }

    public LocalCopyMode localCopyMode() {
        return LocalCopyMode.valueOf(preferences.getString(LOCAL_COPY,
                LocalCopyMode.ALWAYS_KEEP.name()));
    }

    public LargeUploadPowerMode largeUploadPowerMode() {
        return LargeUploadPowerMode.valueOf(preferences.getString(LARGE_UPLOAD_POWER,
                LargeUploadPowerMode.CHARGING_ONLY.name()));
    }
}
