package com.example.cardvr.trips;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class TripState {
    public static final TripState INSTANCE = new TripState();
    public final MutableLiveData<Snapshot> live = new MutableLiveData<>(new Snapshot());

    public static final class Snapshot {
        public long tripId;
        public long startedAt;
        public double speedKmh;
        public double maxSpeedKmh;
        public double distanceMeters;
        public boolean gpsActive;
        public float accuracyMeters;
        public String gpsMessage;
    }

    public LiveData<Snapshot> observe() { return live; }
}
