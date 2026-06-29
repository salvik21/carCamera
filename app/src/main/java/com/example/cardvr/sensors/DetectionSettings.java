package com.example.cardvr.sensors;

public final class DetectionSettings {
    public DetectionSensitivity brakingSensitivity = DetectionSensitivity.MEDIUM;
    public DetectionSensitivity impactSensitivity = DetectionSensitivity.MEDIUM;
    public DetectionSensitivity crashSensitivity = DetectionSensitivity.MEDIUM;
    public DetectionSensitivity phoneMoveSensitivity = DetectionSensitivity.MEDIUM;
    public SensorFrequency sensorFrequency = SensorFrequency.STANDARD;
    public double minAccelerationMs2 = 3.8;
    public double minSpeedBeforeKmh = 15.0;
    public double minSpeedDropKmh = 12.0;
    public long minImpulseDurationMs = 350L;
    public long stopConfirmationMs = 2_500L;
    public long hardBrakingBeforeMs = 30_000L;
    public long hardBrakingAfterMs = 30_000L;
    public long impactBeforeMs = 30_000L;
    public long impactAfterMs = 60_000L;
    public long crashBeforeMs = 60_000L;
    public long crashAfterMs = 120_000L;
    public boolean keepCancelledProtected = true;

    public int sensorDelayMicros() {
        switch (sensorFrequency) {
            case ECONOMY: return 100_000;
            case HIGH: return 20_000;
            case STANDARD:
            default: return 50_000;
        }
    }

    public double brakingAccelerationThreshold() {
        return threshold(minAccelerationMs2, brakingSensitivity, 4.6, 3.8, 3.1);
    }

    public double impactAccelerationThreshold() {
        return threshold(16.0, impactSensitivity, 22.0, 16.0, 12.0);
    }

    public double phoneMoveGyroThreshold() {
        return threshold(2.4, phoneMoveSensitivity, 3.2, 2.4, 1.7);
    }

    public int crashConfidenceThreshold() {
        switch (crashSensitivity) {
            case LOW: return 78;
            case HIGH: return 58;
            case CUSTOM:
            case MEDIUM:
            default: return 68;
        }
    }

    private static double threshold(double custom, DetectionSensitivity sensitivity,
                                    double low, double medium, double high) {
        switch (sensitivity) {
            case LOW: return low;
            case HIGH: return high;
            case CUSTOM: return custom;
            case MEDIUM:
            default: return medium;
        }
    }
}
