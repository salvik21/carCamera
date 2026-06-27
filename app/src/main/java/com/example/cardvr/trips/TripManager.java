package com.example.cardvr.trips;

import android.location.Location;

import com.example.cardvr.database.LocationPointEntity;
import com.example.cardvr.database.TripEntity;
import com.example.cardvr.database.TripStatus;
import com.example.cardvr.location.DistanceCalculator;
import com.example.cardvr.recording.SegmentRepository;

public final class TripManager {
    private final TripRepository repository;
    private final SegmentRepository segments;
    private final RouteRecorder route = new RouteRecorder();
    private final DistanceCalculator distance = new DistanceCalculator();
    private final TripStatisticsCalculator statistics = new TripStatisticsCalculator();
    private TripEntity active;
    private Location previousValid;

    public TripManager(TripRepository repository, SegmentRepository segments) {
        this.repository = repository;
        this.segments = segments;
        active = repository.getActive();
    }

    public synchronized TripEntity startTrip() {
        if (active != null) return active;
        long now = System.currentTimeMillis();
        active = new TripEntity();
        active.startTime = now;
        active.createdAt = now;
        active.status = TripStatus.ACTIVE;
        active.id = repository.insert(active);
        return active;
    }

    public synchronized TripEntity getActiveTrip() { return active; }

    public synchronized void recordLocation(Location location, double speed,
                                            boolean valid, long saveInterval) {
        if (active == null || !route.shouldSave(location, speed, saveInterval)) return;
        LocationPointEntity point = route.create(active.id, location, speed, valid);
        repository.insertPoint(point);
        if (valid) {
            if (active.startLatitude == null) {
                active.startLatitude = point.latitude;
                active.startLongitude = point.longitude;
            }
            active.endLatitude = point.latitude;
            active.endLongitude = point.longitude;
            statistics.apply(active, point,
                    distance.additionalMeters(previousValid, location, speed));
            previousValid = new Location(location);
            repository.update(active);
        }
    }

    public synchronized TripEntity complete(TripStatus status) {
        if (active == null) return null;
        active.status = status;
        active.endTime = System.currentTimeMillis();
        active.segmentCount = segments.countForTrip(active.id);
        active.protectedSegmentCount = segments.countProtectedForTrip(active.id);
        active.totalVideoSizeBytes = segments.sizeForTrip(active.id);
        repository.update(active);
        TripEntity result = active;
        active = null;
        return result;
    }
}
