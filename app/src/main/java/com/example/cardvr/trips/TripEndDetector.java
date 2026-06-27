package com.example.cardvr.trips;

public final class TripEndDetector {
    public boolean shouldSuggestEnd(boolean charging, double speedKmh,
                                    long lastMovementAt, boolean gpsAvailable,
                                    boolean recording, long powerDisconnectedAt,
                                    long powerDelayMs, long stopDelayMs, long now) {
        if (!recording) return false;
        boolean powerExpired = !charging && powerDelayMs >= 0
                && powerDisconnectedAt > 0 && now - powerDisconnectedAt >= powerDelayMs;
        boolean stoppedExpired = gpsAvailable && speedKmh == 0 && stopDelayMs >= 0
                && lastMovementAt > 0 && now - lastMovementAt >= stopDelayMs;
        return powerExpired && stoppedExpired;
    }
}
