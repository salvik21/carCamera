package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;
import com.example.cardvr.location.VideoMetadataSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;

public final class BrakingDetector {
    private final Deque<VideoMetadataSnapshot> gpsHistory = new ArrayDeque<>();
    private long brakingStartedAt;

    @Nullable
    public DetectionResult detect(DetectionContext context) {
        VideoMetadataSnapshot gps = context.gps;
        if (gps == null || gps.accuracy > 50) return DetectionResult.none();
        gpsHistory.addLast(gps);
        while (gpsHistory.size() > 8) gpsHistory.removeFirst();
        if (gpsHistory.size() < 3) return DetectionResult.none();
        VideoMetadataSnapshot before = gpsHistory.peekFirst();
        double speedDrop = before == null ? 0 : before.speedKmh - gps.speedKmh;
        double longitudinalDecel = -context.sensor.linearAccelerationX;
        boolean speedOk = before != null
                && before.speedKmh >= context.settings.minSpeedBeforeKmh
                && speedDrop >= context.settings.minSpeedDropKmh;
        boolean accelOk = longitudinalDecel >= context.settings.brakingAccelerationThreshold();
        long now = context.sensor.timestampMillis;
        if (accelOk) {
            if (brakingStartedAt == 0L) brakingStartedAt = now;
        } else {
            brakingStartedAt = 0L;
        }
        long duration = brakingStartedAt == 0L ? 0L : now - brakingStartedAt;
        if (!speedOk || !accelOk || duration < context.settings.minImpulseDurationMs) {
            return DetectionResult.none();
        }
        int confidence = 55
                + (int) Math.min(25, speedDrop)
                + (int) Math.min(15, longitudinalDecel * 2)
                + (context.calibrated ? 5 : -8);
        EventSeverity severity = speedDrop > 35 || longitudinalDecel > 7
                ? EventSeverity.HIGH : EventSeverity.MEDIUM;
        return new DetectionResult(EventType.HARD_BRAKING, severity, confidence,
                "Скорость заметно снизилась, продольное замедление держалось достаточно долго",
                context.settings.hardBrakingBeforeMs,
                context.settings.hardBrakingAfterMs);
    }
}
