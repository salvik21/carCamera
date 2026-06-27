package com.example.cardvr.trips;

import com.example.cardvr.database.LocationPointEntity;
import com.example.cardvr.database.TripEntity;

public final class TripStatisticsCalculator {
    private long lastTimestamp;
    private double movingSpeedSum;
    private int movingSamples;

    public void apply(TripEntity trip, LocationPointEntity point, double addedDistance) {
        trip.totalDistanceMeters += addedDistance;
        trip.maxSpeedKmh = Math.max(trip.maxSpeedKmh, point.speedKmh);
        if (lastTimestamp > 0) {
            long interval = Math.max(0, point.timestamp - lastTimestamp);
            if (point.speedKmh > 0) {
                trip.movingTimeMs += interval;
                movingSpeedSum += point.speedKmh;
                movingSamples++;
            } else {
                trip.stoppedTimeMs += interval;
            }
        }
        if (movingSamples > 0) trip.averageSpeedKmh = movingSpeedSum / movingSamples;
        lastTimestamp = point.timestamp;
    }
}
