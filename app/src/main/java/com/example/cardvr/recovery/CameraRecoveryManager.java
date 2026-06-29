package com.example.cardvr.recovery;

public final class CameraRecoveryManager {
    private final long[] delays = {1_000L, 3_000L, 10_000L, 60_000L};
    private int attempts;

    public long nextDelayMillis() {
        int index = Math.min(attempts, delays.length - 1);
        attempts++;
        return delays[index];
    }

    public boolean shouldStopRecording() {
        return attempts >= 5;
    }

    public void reset() {
        attempts = 0;
    }
}
