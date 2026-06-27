package com.example.cardvr.trips;

import android.location.Location;

import com.example.cardvr.database.LocationPointEntity;

public final class RouteRecorder {
    private long lastSavedAt;
    private Location lastSavedLocation;
    private double lastSavedSpeed;

    public boolean shouldSave(Location location, double speedKmh, long intervalMs) {
        if (lastSavedAt == 0 || location.getTime() - lastSavedAt >= intervalMs) return true;
        if (lastSavedLocation != null && lastSavedLocation.distanceTo(location) >= 15f) return true;
        return Math.abs(speedKmh - lastSavedSpeed) >= 10;
    }

    public LocationPointEntity create(long tripId, Location location,
                                      double speedKmh, boolean valid) {
        LocationPointEntity point = new LocationPointEntity();
        point.tripId = tripId;
        point.timestamp = location.getTime();
        point.latitude = location.getLatitude();
        point.longitude = location.getLongitude();
        point.accuracyMeters = location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
        point.speedKmh = speedKmh;
        point.bearing = location.hasBearing() ? location.getBearing() : 0;
        point.altitude = location.hasAltitude() ? location.getAltitude() : null;
        point.provider = location.getProvider() == null ? "" : location.getProvider();
        point.valid = valid;
        lastSavedAt = point.timestamp;
        lastSavedLocation = new Location(location);
        lastSavedSpeed = speedKmh;
        return point;
    }
}
