package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;

public final class SuddenStopDetector {
    private long stoppedSince;

    @Nullable
    public DetectionResult detect(DetectionContext context,
                                  @Nullable DetectionResult braking) {
        if (context.gps == null) return DetectionResult.none();
        boolean movingBefore = context.gps.speedKmh >= context.settings.minSpeedBeforeKmh;
        boolean nearZero = context.gps.speedKmh <= 3.0;
        long now = context.sensor.timestampMillis;
        if (nearZero) {
            if (stoppedSince == 0L) stoppedSince = now;
        } else {
            stoppedSince = 0L;
        }
        if (braking == null || !movingBefore
                || stoppedSince == 0L
                || now - stoppedSince < context.settings.stopConfirmationMs) {
            return DetectionResult.none();
        }
        return new DetectionResult(EventType.SUDDEN_STOP, EventSeverity.HIGH,
                Math.min(100, braking.confidence + 8),
                "После сильного замедления скорость стала близкой к нулю и не восстановилась",
                context.settings.impactBeforeMs,
                context.settings.impactAfterMs);
    }
}
