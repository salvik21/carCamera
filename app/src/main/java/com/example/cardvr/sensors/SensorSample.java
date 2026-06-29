package com.example.cardvr.sensors;

import androidx.annotation.Nullable;

public final class SensorSample {
    public final long timestampMillis;
    public final double accelerationX;
    public final double accelerationY;
    public final double accelerationZ;
    public final double linearAccelerationX;
    public final double linearAccelerationY;
    public final double linearAccelerationZ;
    public final double gyroX;
    public final double gyroY;
    public final double gyroZ;
    public final double totalAcceleration;
    @Nullable public final String orientation;
    public final boolean valid;

    public SensorSample(long timestampMillis,
                        double accelerationX,
                        double accelerationY,
                        double accelerationZ,
                        double linearAccelerationX,
                        double linearAccelerationY,
                        double linearAccelerationZ,
                        double gyroX,
                        double gyroY,
                        double gyroZ,
                        double totalAcceleration,
                        @Nullable String orientation,
                        boolean valid) {
        this.timestampMillis = timestampMillis;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.linearAccelerationX = linearAccelerationX;
        this.linearAccelerationY = linearAccelerationY;
        this.linearAccelerationZ = linearAccelerationZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.totalAcceleration = totalAcceleration;
        this.orientation = orientation;
        this.valid = valid;
    }

    public double gyroMagnitude() {
        return Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);
    }
}
