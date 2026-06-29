package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;

public final class ImpactDetector {
    private long impulseStartedAt;
    private String orientationBefore;

    @Nullable
    public DetectionResult detect(DetectionContext context) {
        double g = context.sensor.totalAcceleration / 9.81d;
        double gyro = context.sensor.gyroMagnitude();
        boolean impulse = context.sensor.totalAcceleration
                >= context.settings.impactAccelerationThreshold();
        long now = context.sensor.timestampMillis;
        if (impulse) {
            if (impulseStartedAt == 0L) {
                impulseStartedAt = now;
                orientationBefore = context.sensor.orientation;
            }
        } else {
            impulseStartedAt = 0L;
            orientationBefore = context.sensor.orientation;
        }
        long duration = impulseStartedAt == 0L ? 0L : now - impulseStartedAt;
        if (!impulse || duration < 80L) return DetectionResult.none();
        boolean moving = context.gps != null && context.gps.speedKmh >= 8.0;
        boolean phoneLikelyMoved = gyro > context.settings.phoneMoveGyroThreshold()
                && !moving && g < 2.2;
        if (phoneLikelyMoved) {
            return new DetectionResult(EventType.PHONE_MOVED, EventSeverity.LOW, 50,
                    "Похоже на перемещение телефона без подтверждения скоростью",
                    15_000L, 15_000L);
        }
        boolean pothole = moving && duration < 180L && g < 2.6 && gyro < 1.8;
        if (pothole) {
            return new DetectionResult(EventType.POTHOLE, EventSeverity.LOW, 55,
                    "Короткий вертикальный импульс похож на яму",
                    15_000L, 15_000L);
        }
        int confidence = 55 + (int) Math.min(25, g * 8)
                + (moving ? 10 : -5)
                + (gyro > 1.2 ? 8 : 0);
        EventSeverity severity = g >= 3.2 ? EventSeverity.CRITICAL
                : g >= 2.4 ? EventSeverity.HIGH : EventSeverity.MEDIUM;
        return new DetectionResult(EventType.IMPACT, severity, confidence,
                "Сильный импульс ускорения подтверждён гироскопом/движением; ориентация до: "
                        + (orientationBefore == null ? "нет данных" : orientationBefore),
                context.settings.impactBeforeMs,
                context.settings.impactAfterMs);
    }
}
