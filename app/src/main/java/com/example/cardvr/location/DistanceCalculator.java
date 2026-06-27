package com.example.cardvr.location;

import android.location.Location;

public final class DistanceCalculator {
    public double additionalMeters(Location previous, Location current, double speedKmh) {
        if (previous == null || current == null || speedKmh <= 0) return 0;
        float distance = previous.distanceTo(current);
        return Float.isFinite(distance) && distance >= 0 && distance < 5_000 ? distance : 0;
    }
}
