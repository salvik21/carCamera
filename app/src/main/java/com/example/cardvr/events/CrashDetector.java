package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;

public final class CrashDetector {
    @Nullable
    public DetectionResult detect(DetectionContext context,
                                  @Nullable DetectionResult braking,
                                  @Nullable DetectionResult suddenStop,
                                  @Nullable DetectionResult impact) {
        if (impact == null || impact.type != EventType.IMPACT) {
            return DetectionResult.none();
        }
        int score = impact.confidence;
        StringBuilder explanation = new StringBuilder("Удар");
        if (braking != null) {
            score += 12;
            explanation.append(", резкое торможение");
        }
        if (suddenStop != null) {
            score += 16;
            explanation.append(", остановка после события");
        }
        if (context.gps != null && context.gps.speedKmh < 5.0) {
            score += 6;
            explanation.append(", скорость после события близка к нулю");
        }
        if (context.sensor.gyroMagnitude() > 2.0) {
            score += 6;
            explanation.append(", изменилось положение телефона");
        }
        if (score < context.settings.crashConfidenceThreshold()) {
            return DetectionResult.none();
        }
        EventSeverity severity = score >= 90 ? EventSeverity.CRITICAL : EventSeverity.HIGH;
        return new DetectionResult(EventType.POSSIBLE_CRASH, severity, Math.min(100, score),
                explanation.toString(),
                context.settings.crashBeforeMs,
                context.settings.crashAfterMs);
    }
}
