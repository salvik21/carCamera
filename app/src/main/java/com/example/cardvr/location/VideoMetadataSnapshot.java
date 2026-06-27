package com.example.cardvr.location;

public final class VideoMetadataSnapshot {
    public final long timestamp;
    public final double speedKmh;
    public final double latitude;
    public final double longitude;
    public final float accuracy;
    public final float bearing;

    public VideoMetadataSnapshot(long timestamp, double speedKmh, double latitude,
                                 double longitude, float accuracy, float bearing) {
        this.timestamp = timestamp;
        this.speedKmh = speedKmh;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.bearing = bearing;
    }
}
