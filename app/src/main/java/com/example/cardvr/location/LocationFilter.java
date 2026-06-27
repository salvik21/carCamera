package com.example.cardvr.location;

import android.location.Location;
import android.os.SystemClock;

public final class LocationFilter {
    private static final long MAX_AGE_MS = 15_000L;
    private Location previous;

    public boolean isValid(Location location, float maxAccuracyMeters) {
        if (location == null || !location.hasAccuracy()
                || location.getAccuracy() > maxAccuracyMeters) return false;
        long age = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
        if (age < 0 || age > MAX_AGE_MS * 1_000_000L) return false;
        if (previous != null) {
            if (location.getTime() <= previous.getTime()) return false;
            double seconds = (location.getTime() - previous.getTime()) / 1000d;
            if (seconds > 0 && previous.distanceTo(location) / seconds * 3.6 > 350) return false;
        }
        previous = new Location(location);
        return true;
    }

    public Location getPrevious() {
        return previous == null ? null : new Location(previous);
    }
}
