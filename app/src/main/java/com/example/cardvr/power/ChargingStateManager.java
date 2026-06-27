package com.example.cardvr.power;

import androidx.annotation.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ChargingStateManager {

    public interface Listener {
        void onChargingStateChanged(@NonNull ChargingState state);
    }

    public static final class ChargingState {
        private final boolean charging;
        private final int batteryPercent;

        public ChargingState(boolean charging, int batteryPercent) {
            this.charging = charging;
            this.batteryPercent = batteryPercent;
        }

        public boolean isCharging() {
            return charging;
        }

        public int getBatteryPercent() {
            return batteryPercent;
        }
    }

    private static final ChargingStateManager INSTANCE = new ChargingStateManager();

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private volatile ChargingState currentState = new ChargingState(false, -1);
    private volatile boolean warningSuppressedForCurrentTrip;

    private ChargingStateManager() {
    }

    @NonNull
    public static ChargingStateManager getInstance() {
        return INSTANCE;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        listener.onChargingStateChanged(currentState);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void updateState(@NonNull ChargingState state) {
        currentState = state;
        for (Listener listener : listeners) {
            listener.onChargingStateChanged(state);
        }
    }

    @NonNull
    public ChargingState getCurrentState() {
        return currentState;
    }

    public boolean isWarningSuppressedForCurrentTrip() {
        return warningSuppressedForCurrentTrip;
    }

    public void setWarningSuppressedForCurrentTrip(boolean suppressed) {
        warningSuppressedForCurrentTrip = suppressed;
    }

    public void resetTripWarningPreference() {
        warningSuppressedForCurrentTrip = false;
    }
}
