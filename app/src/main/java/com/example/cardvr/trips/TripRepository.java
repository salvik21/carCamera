package com.example.cardvr.trips;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cardvr.database.AppDatabase;
import com.example.cardvr.database.LocationPointDao;
import com.example.cardvr.database.LocationPointEntity;
import com.example.cardvr.database.TripDao;
import com.example.cardvr.database.TripEntity;
import com.example.cardvr.database.TripStatus;

import java.util.List;

public final class TripRepository {
    private final TripDao trips;
    private final LocationPointDao points;

    public TripRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        trips = db.tripDao();
        points = db.locationPointDao();
    }

    public long insert(TripEntity trip) { return trips.insert(trip); }
    public void update(TripEntity trip) { trips.update(trip); }
    public TripEntity getActive() { return trips.getActive(); }
    public TripEntity getById(long id) { return trips.getById(id); }
    public LiveData<List<TripEntity>> observeAll() { return trips.observeAll(); }
    public TripEntity getLatestCompleted() { return trips.getLatestCompleted(); }
    public void finish(long id, TripStatus status, long time) { trips.finish(id, status, time); }
    public long insertPoint(LocationPointEntity point) { return points.insert(point); }
    public LocationPointEntity getLastPoint(long tripId) { return points.getLast(tripId); }
    public List<LocationPointEntity> getPoints(long tripId) { return points.getForTrip(tripId); }
}
