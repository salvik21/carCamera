package com.example.cardvr.events;

import androidx.annotation.Nullable;

import com.example.cardvr.location.VideoMetadataSnapshot;
import com.example.cardvr.sensors.DetectionSettings;
import com.example.cardvr.sensors.SensorSample;

public final class DetectionContext {
    public final SensorSample sensor;
    @Nullable public final VideoMetadataSnapshot gps;
    @Nullable public final Long tripId;
    public final DetectionSettings settings;
    public final boolean calibrated;
    public final boolean simulated;

    public DetectionContext(SensorSample sensor,
                            @Nullable VideoMetadataSnapshot gps,
                            @Nullable Long tripId,
                            DetectionSettings settings,
                            boolean calibrated,
                            boolean simulated) {
        this.sensor = sensor;
        this.gps = gps;
        this.tripId = tripId;
        this.settings = settings;
        this.calibrated = calibrated;
        this.simulated = simulated;
    }
}
