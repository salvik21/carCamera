package com.example.cardvr.recording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class RecordingStateManager {

    public enum Status {
        IDLE,
        STARTING,
        RECORDING,
        STOPPING,
        ERROR
    }

    public static final class State {
        private final Status status;
        private final long startedAtElapsedRealtime;
        private final long elapsedMillis;
        private final String cameraName;
        private final boolean charging;
        private final int batteryPercent;
        private final boolean previewAttached;
        private final boolean protectedEventMarked;
        private final String outputPath;
        private final String message;

        private State(
                Status status,
                long startedAtElapsedRealtime,
                long elapsedMillis,
                String cameraName,
                boolean charging,
                int batteryPercent,
                boolean previewAttached,
                boolean protectedEventMarked,
                String outputPath,
                String message
        ) {
            this.status = status;
            this.startedAtElapsedRealtime = startedAtElapsedRealtime;
            this.elapsedMillis = elapsedMillis;
            this.cameraName = cameraName;
            this.charging = charging;
            this.batteryPercent = batteryPercent;
            this.previewAttached = previewAttached;
            this.protectedEventMarked = protectedEventMarked;
            this.outputPath = outputPath;
            this.message = message;
        }

        private static State idle(@Nullable String outputPath) {
            return new State(Status.IDLE, 0L, 0L, "—", false, -1,
                    false, false, outputPath, null);
        }

        @NonNull
        public Status getStatus() {
            return status;
        }

        public long getStartedAtElapsedRealtime() {
            return startedAtElapsedRealtime;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }

        @NonNull
        public String getCameraName() {
            return cameraName;
        }

        public boolean isCharging() {
            return charging;
        }

        public int getBatteryPercent() {
            return batteryPercent;
        }

        public boolean isPreviewAttached() {
            return previewAttached;
        }

        public boolean isProtectedEventMarked() {
            return protectedEventMarked;
        }

        @Nullable
        public String getOutputPath() {
            return outputPath;
        }

        @Nullable
        public String getMessage() {
            return message;
        }

        public boolean isServiceActive() {
            return status == Status.STARTING
                    || status == Status.RECORDING
                    || status == Status.STOPPING;
        }

        private State copy(
                Status newStatus,
                long newStartedAt,
                long newElapsed,
                String newCameraName,
                boolean newCharging,
                int newBatteryPercent,
                boolean newPreviewAttached,
                boolean newProtected,
                String newOutputPath,
                String newMessage
        ) {
            return new State(newStatus, newStartedAt, newElapsed, newCameraName,
                    newCharging, newBatteryPercent, newPreviewAttached, newProtected,
                    newOutputPath, newMessage);
        }
    }

    private static final RecordingStateManager INSTANCE = new RecordingStateManager();

    private final MutableLiveData<State> stateLiveData =
            new MutableLiveData<>(State.idle(null));
    private State currentState = State.idle(null);

    private RecordingStateManager() {
    }

    @NonNull
    public static RecordingStateManager getInstance() {
        return INSTANCE;
    }

    @NonNull
    public LiveData<State> getStateLiveData() {
        return stateLiveData;
    }

    @NonNull
    public synchronized State getCurrentState() {
        return currentState;
    }

    public synchronized boolean trySetStarting(String cameraName) {
        if (currentState.isServiceActive()) {
            return false;
        }
        publish(currentState.copy(Status.STARTING, 0L, 0L, cameraName,
                currentState.charging, currentState.batteryPercent, false,
                false, null, null));
        return true;
    }

    public synchronized void setRecording(long startedAtElapsedRealtime) {
        publish(currentState.copy(Status.RECORDING, startedAtElapsedRealtime, 0L,
                currentState.cameraName, currentState.charging,
                currentState.batteryPercent, currentState.previewAttached,
                currentState.protectedEventMarked, null, null));
    }

    public synchronized void setElapsed(long elapsedMillis) {
        publish(currentState.copy(currentState.status, currentState.startedAtElapsedRealtime,
                Math.max(0L, elapsedMillis), currentState.cameraName,
                currentState.charging, currentState.batteryPercent,
                currentState.previewAttached, currentState.protectedEventMarked,
                currentState.outputPath, currentState.message));
    }

    public synchronized void setChargingState(boolean charging, int batteryPercent) {
        publish(currentState.copy(currentState.status, currentState.startedAtElapsedRealtime,
                currentState.elapsedMillis, currentState.cameraName, charging,
                batteryPercent, currentState.previewAttached,
                currentState.protectedEventMarked, currentState.outputPath,
                currentState.message));
    }

    public synchronized void setPreviewAttached(boolean attached) {
        publish(currentState.copy(currentState.status, currentState.startedAtElapsedRealtime,
                currentState.elapsedMillis, currentState.cameraName,
                currentState.charging, currentState.batteryPercent, attached,
                currentState.protectedEventMarked, currentState.outputPath,
                currentState.message));
    }

    public synchronized void markProtectedEvent() {
        publish(currentState.copy(currentState.status, currentState.startedAtElapsedRealtime,
                currentState.elapsedMillis, currentState.cameraName,
                currentState.charging, currentState.batteryPercent,
                currentState.previewAttached, true, currentState.outputPath,
                "Событие защиты отмечено"));
    }

    public synchronized void setStopping() {
        if (currentState.status == Status.RECORDING
                || currentState.status == Status.STARTING) {
            publish(currentState.copy(Status.STOPPING,
                    currentState.startedAtElapsedRealtime, currentState.elapsedMillis,
                    currentState.cameraName, currentState.charging,
                    currentState.batteryPercent, currentState.previewAttached,
                    currentState.protectedEventMarked, currentState.outputPath,
                    null));
        }
    }

    public synchronized void setIdle(@Nullable String outputPath) {
        State idleState = State.idle(outputPath);
        publish(idleState.copy(Status.IDLE, 0L, 0L, currentState.cameraName,
                currentState.charging, currentState.batteryPercent, false,
                false, outputPath, null));
    }

    public synchronized void setError(@NonNull String message) {
        publish(currentState.copy(Status.ERROR, currentState.startedAtElapsedRealtime,
                currentState.elapsedMillis, currentState.cameraName,
                currentState.charging, currentState.batteryPercent, false,
                currentState.protectedEventMarked, currentState.outputPath, message));
    }

    private void publish(State state) {
        currentState = state;
        stateLiveData.postValue(state);
    }
}
