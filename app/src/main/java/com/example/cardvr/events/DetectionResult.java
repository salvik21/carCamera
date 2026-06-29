package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.database.EventSeverity;
import com.example.cardvr.database.EventType;

public final class DetectionResult {
    public final EventType type;
    public final EventSeverity severity;
    public final int confidence;
    public final String explanation;
    public final long beforeMs;
    public final long afterMs;

    public DetectionResult(EventType type, EventSeverity severity, int confidence,
                           String explanation, long beforeMs, long afterMs) {
        this.type = type;
        this.severity = severity;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.explanation = explanation;
        this.beforeMs = beforeMs;
        this.afterMs = afterMs;
    }

    @Nullable
    public static DetectionResult none() {
        return null;
    }
}
