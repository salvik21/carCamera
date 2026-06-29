package com.example.cardvr.sensors;

import androidx.annotation.Nullable;

public final class SensorSnapshotProvider {
    private volatile SensorSample latestValid;

    public void update(@Nullable SensorSample sample) {
        if (sample != null && sample.valid) {
            latestValid = sample;
        }
    }

    @Nullable
    public SensorSample getLatestValid() {
        return latestValid;
    }
}
