package com.example.cardvr.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public final class LocationTracker {
    public interface Listener {
        void onLocation(Location location);
        void onGpsStatus(boolean active, String reason);
    }

    private final Context context;
    private final FusedLocationProviderClient client;
    private final Listener listener;
    private Location last;
    private boolean running;
    private final LocationCallback callback = new LocationCallback() {
        @Override public void onLocationResult(@NonNull LocationResult result) {
            Location value = result.getLastLocation();
            if (value != null) {
                last = new Location(value);
                listener.onLocation(last);
                listener.onGpsStatus(true, null);
            }
        }
    };

    public LocationTracker(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        client = LocationServices.getFusedLocationProviderClient(this.context);
    }

    @SuppressLint("MissingPermission")
    public boolean start(long intervalMs, int mode) {
        if (!hasPermission()) {
            listener.onGpsStatus(false, "Разрешение геолокации не выдано");
            return false;
        }
        int priority = mode == 1 ? Priority.PRIORITY_BALANCED_POWER_ACCURACY
                : mode == 2 ? Priority.PRIORITY_LOW_POWER : Priority.PRIORITY_HIGH_ACCURACY;
        LocationRequest request = new LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(Math.max(500, intervalMs / 2))
                .build();
        client.requestLocationUpdates(request, callback, context.getMainLooper());
        running = true;
        return true;
    }

    public void stop() {
        if (running) client.removeLocationUpdates(callback);
        running = false;
        listener.onGpsStatus(false, "GPS остановлен");
    }

    public Location getLastLocation() { return last == null ? null : new Location(last); }
    public boolean isRunning() { return running; }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
