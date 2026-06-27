package com.example.cardvr.location;

import android.location.Location;

public final class SpeedCalculator {
    private static final double MAX_KMH = 300.0;
    private double smoothed;
    private boolean initialized;

    public static double metersPerSecondToKmh(double metersPerSecond) {
        if (!Double.isFinite(metersPerSecond) || metersPerSecond < 0) return 0;
        return metersPerSecond * 3.6;
    }

    public double calculate(Location current, Location previous, float parkingThresholdKmh) {
        double value = Double.NaN;
        if (current.hasSpeed() && current.getSpeed() >= 0 && Float.isFinite(current.getSpeed())) {
            value = metersPerSecondToKmh(current.getSpeed());
        } else if (previous != null && current.getTime() > previous.getTime()) {
            double seconds = (current.getTime() - previous.getTime()) / 1000d;
            value = previous.distanceTo(current) / seconds * 3.6;
        }
        if (!Double.isFinite(value) || value < 0 || value > MAX_KMH) return initialized ? smoothed : 0;
        if (value < parkingThresholdKmh) value = 0;
        if (!initialized) {
            smoothed = value;
            initialized = true;
        } else {
            double delta = value - smoothed;
            if (Math.abs(delta) > 90) value = smoothed + Math.copySign(30, delta);
            smoothed = smoothed * 0.65 + value * 0.35;
        }
        return smoothed < parkingThresholdKmh ? 0 : smoothed;
    }
}
