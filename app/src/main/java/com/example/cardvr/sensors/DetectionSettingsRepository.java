package com.example.cardvr.sensors;

import android.content.Context;
import android.content.SharedPreferences;

public final class DetectionSettingsRepository {
    private static final String PREFS = "detection_settings";
    private static final String BRAKING = "braking_sensitivity";
    private static final String IMPACT = "impact_sensitivity";
    private static final String CRASH = "crash_sensitivity";
    private static final String PHONE_MOVE = "phone_move_sensitivity";
    private static final String FREQUENCY = "sensor_frequency";
    private static final String MIN_ACCEL = "min_acceleration";
    private static final String MIN_SPEED = "min_speed_before";
    private static final String SPEED_DROP = "min_speed_drop";
    private static final String IMPULSE = "min_impulse_ms";
    private static final String STOP_CONFIRM = "stop_confirmation_ms";
    private static final String KEEP_CANCELLED = "keep_cancelled_protected";
    private final SharedPreferences preferences;

    public DetectionSettingsRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public DetectionSettings get() {
        DetectionSettings settings = new DetectionSettings();
        settings.brakingSensitivity = sensitivity(BRAKING, settings.brakingSensitivity);
        settings.impactSensitivity = sensitivity(IMPACT, settings.impactSensitivity);
        settings.crashSensitivity = sensitivity(CRASH, settings.crashSensitivity);
        settings.phoneMoveSensitivity = sensitivity(PHONE_MOVE, settings.phoneMoveSensitivity);
        settings.sensorFrequency = SensorFrequency.valueOf(preferences.getString(
                FREQUENCY, settings.sensorFrequency.name()));
        settings.minAccelerationMs2 = preferences.getFloat(MIN_ACCEL,
                (float) settings.minAccelerationMs2);
        settings.minSpeedBeforeKmh = preferences.getFloat(MIN_SPEED,
                (float) settings.minSpeedBeforeKmh);
        settings.minSpeedDropKmh = preferences.getFloat(SPEED_DROP,
                (float) settings.minSpeedDropKmh);
        settings.minImpulseDurationMs = preferences.getLong(IMPULSE,
                settings.minImpulseDurationMs);
        settings.stopConfirmationMs = preferences.getLong(STOP_CONFIRM,
                settings.stopConfirmationMs);
        settings.keepCancelledProtected = preferences.getBoolean(KEEP_CANCELLED, true);
        return settings;
    }

    public void save(DetectionSettings settings) {
        preferences.edit()
                .putString(BRAKING, settings.brakingSensitivity.name())
                .putString(IMPACT, settings.impactSensitivity.name())
                .putString(CRASH, settings.crashSensitivity.name())
                .putString(PHONE_MOVE, settings.phoneMoveSensitivity.name())
                .putString(FREQUENCY, settings.sensorFrequency.name())
                .putFloat(MIN_ACCEL, (float) settings.minAccelerationMs2)
                .putFloat(MIN_SPEED, (float) settings.minSpeedBeforeKmh)
                .putFloat(SPEED_DROP, (float) settings.minSpeedDropKmh)
                .putLong(IMPULSE, settings.minImpulseDurationMs)
                .putLong(STOP_CONFIRM, settings.stopConfirmationMs)
                .putBoolean(KEEP_CANCELLED, settings.keepCancelledProtected)
                .apply();
    }

    public void restoreRecommended() {
        preferences.edit().clear().apply();
    }

    private DetectionSensitivity sensitivity(String key, DetectionSensitivity fallback) {
        return DetectionSensitivity.valueOf(preferences.getString(key, fallback.name()));
    }
}
