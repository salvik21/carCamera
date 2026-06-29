package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;

public final class EventClassifier {
    private long lastHardBraking;
    private long lastImpact;
    private long lastCrash;

    @Nullable
    public DetectionResult classify(DetectionResult braking,
                                    DetectionResult suddenStop,
                                    DetectionResult impact,
                                    DetectionResult crash,
                                    long now) {
        DetectionResult candidate = strongest(braking, suddenStop, impact, crash);
        if (candidate == null) return null;
        if (isSuppressed(candidate, now)) return null;
        mark(candidate.type, now);
        return candidate;
    }

    private boolean isSuppressed(DetectionResult result, long now) {
        if (result.type == EventType.POSSIBLE_CRASH) return now - lastCrash < 30_000L;
        if (result.type == EventType.IMPACT || result.type == EventType.POTHOLE
                || result.type == EventType.PHONE_MOVED) {
            return now - lastImpact < 15_000L && result.severity.ordinal() <= EventSeverity.HIGH.ordinal();
        }
        if (result.type == EventType.HARD_BRAKING || result.type == EventType.SUDDEN_STOP) {
            return now - lastHardBraking < 10_000L;
        }
        return false;
    }

    private void mark(EventType type, long now) {
        if (type == EventType.POSSIBLE_CRASH) lastCrash = now;
        else if (type == EventType.IMPACT || type == EventType.POTHOLE
                || type == EventType.PHONE_MOVED) lastImpact = now;
        else if (type == EventType.HARD_BRAKING || type == EventType.SUDDEN_STOP) {
            lastHardBraking = now;
        }
    }

    @Nullable
    private static DetectionResult strongest(DetectionResult... results) {
        DetectionResult best = null;
        for (DetectionResult result : results) {
            if (result == null) continue;
            if (best == null
                    || result.type == EventType.POSSIBLE_CRASH
                    || result.severity.ordinal() > best.severity.ordinal()
                    || result.confidence > best.confidence) {
                best = result;
            }
        }
        return best;
    }
}
