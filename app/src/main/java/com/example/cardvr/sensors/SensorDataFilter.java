package com.example.cardvr.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SensorDataFilter {
    private final float[] gravity = new float[3];
    private final float[] accel = new float[3];
    private final float[] linear = new float[3];
    private final float[] gyro = new float[3];
    private long lastTimestampNs;
    private boolean hasAccel;
    private boolean hasLinear;
    private boolean hasGyro;

    public synchronized void onAccelerometer(float[] values, long timestampNs) {
        float dt = deltaSeconds(timestampNs);
        float alpha = dt <= 0 ? 0.82f : (float) Math.exp(-dt / 0.35f);
        for (int i = 0; i < 3; i++) {
            gravity[i] = alpha * gravity[i] + (1f - alpha) * values[i];
            accel[i] = smooth(accel[i], values[i], 0.35f);
            if (!hasLinear) {
                linear[i] = accel[i] - gravity[i];
            }
        }
        hasAccel = true;
    }

    public synchronized void onLinearAcceleration(float[] values) {
        for (int i = 0; i < 3; i++) {
            linear[i] = smooth(linear[i], values[i], 0.45f);
        }
        hasLinear = true;
    }

    public synchronized void onGyroscope(float[] values) {
        for (int i = 0; i < 3; i++) {
            gyro[i] = smooth(gyro[i], values[i], 0.5f);
        }
        hasGyro = true;
    }

    @Nullable
    public synchronized SensorSample snapshot(@NonNull SensorCalibrationManager calibration,
                                              @Nullable String orientation) {
        if (!hasAccel && !hasLinear) return null;
        double lx = linear[0];
        double ly = linear[1];
        double lz = linear[2];
        if (isSingleSampleSpike(lx, ly, lz)) return null;
        double total = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (total > 80.0) return null;
        SensorCalibrationManager.ProjectedAcceleration projected =
                calibration.project(lx, ly, lz);
        return new SensorSample(
                System.currentTimeMillis(),
                accel[0], accel[1], accel[2],
                projected.longitudinal, projected.lateral, projected.vertical,
                hasGyro ? gyro[0] : 0, hasGyro ? gyro[1] : 0, hasGyro ? gyro[2] : 0,
                total,
                orientation,
                true
        );
    }

    private float deltaSeconds(long timestampNs) {
        if (lastTimestampNs == 0L) {
            lastTimestampNs = timestampNs;
            return 0f;
        }
        long delta = Math.max(0L, timestampNs - lastTimestampNs);
        lastTimestampNs = timestampNs;
        return delta / 1_000_000_000f;
    }

    private static float smooth(float oldValue, float newValue, float weight) {
        return oldValue * weight + newValue * (1f - weight);
    }

    private static boolean isSingleSampleSpike(double x, double y, double z) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z);
    }
}
