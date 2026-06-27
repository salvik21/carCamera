package com.example.cardvr.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsRepository {
    private static final String PREFS = "storage_settings";
    private static final String SEGMENT_MS = "segment_duration_ms";
    private static final String MAX_BYTES = "max_recording_bytes";
    private static final String RESERVE_BYTES = "reserve_free_bytes";
    private static final String GPS_INTERVAL = "gps_interval_ms";
    private static final String GPS_PRIORITY = "gps_priority";
    private static final String GPS_ACCURACY = "gps_max_accuracy_m";
    private static final String PARKING_SPEED = "parking_speed_kmh";
    private static final String LOW_BATTERY_GPS = "low_battery_gps";
    private static final String TRIP_START_MODE = "trip_start_mode";
    private static final String POWER_END_DELAY = "power_end_delay_ms";
    private static final String STOP_END_DELAY = "stop_end_delay_ms";
    public static final long GIB = 1024L * 1024L * 1024L;

    private final SharedPreferences preferences;

    public SettingsRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long getSegmentDurationMs() {
        return preferences.getLong(SEGMENT_MS, 60_000L);
    }

    public void setSegmentDurationMs(long value) {
        requirePositive(value);
        preferences.edit().putLong(SEGMENT_MS, value).apply();
    }

    public long getMaxRecordingBytes() {
        return preferences.getLong(MAX_BYTES, 5L * GIB);
    }

    public void setMaxRecordingBytes(long value) {
        requirePositive(value);
        preferences.edit().putLong(MAX_BYTES, value).apply();
    }

    public long getReserveFreeBytes() {
        return preferences.getLong(RESERVE_BYTES, 2L * GIB);
    }

    public void setReserveFreeBytes(long value) {
        requirePositive(value);
        preferences.edit().putLong(RESERVE_BYTES, value).apply();
    }

    private static void requirePositive(long value) {
        if (value <= 0) throw new IllegalArgumentException("Value must be positive");
    }

    public long getGpsIntervalMs() { return preferences.getLong(GPS_INTERVAL, 1_000L); }
    public void setGpsIntervalMs(long value) { requirePositive(value); preferences.edit().putLong(GPS_INTERVAL, value).apply(); }
    public int getGpsPriority() { return preferences.getInt(GPS_PRIORITY, 0); }
    public void setGpsPriority(int value) { preferences.edit().putInt(GPS_PRIORITY, value).apply(); }
    public float getMaxGpsAccuracyMeters() { return preferences.getFloat(GPS_ACCURACY, 20f); }
    public void setMaxGpsAccuracyMeters(float value) { if (value <= 0) throw new IllegalArgumentException(); preferences.edit().putFloat(GPS_ACCURACY, value).apply(); }
    public float getParkingSpeedKmh() { return preferences.getFloat(PARKING_SPEED, 3f); }
    public void setParkingSpeedKmh(float value) { if (value < 0) throw new IllegalArgumentException(); preferences.edit().putFloat(PARKING_SPEED, value).apply(); }
    public boolean reduceGpsOnLowBattery() { return preferences.getBoolean(LOW_BATTERY_GPS, true); }
    public void setReduceGpsOnLowBattery(boolean value) { preferences.edit().putBoolean(LOW_BATTERY_GPS, value).apply(); }
    public int getTripStartMode() { return preferences.getInt(TRIP_START_MODE, 0); }
    public void setTripStartMode(int value) { preferences.edit().putInt(TRIP_START_MODE, value).apply(); }
    public long getPowerEndDelayMs() { return preferences.getLong(POWER_END_DELAY, -1L); }
    public void setPowerEndDelayMs(long value) { preferences.edit().putLong(POWER_END_DELAY, value).apply(); }
    public long getStopEndDelayMs() { return preferences.getLong(STOP_END_DELAY, -1L); }
    public void setStopEndDelayMs(long value) { preferences.edit().putLong(STOP_END_DELAY, value).apply(); }
}
