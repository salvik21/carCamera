package com.example.cardvr.recovery;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SafeShutdownManager {
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public boolean begin() {
        return shuttingDown.compareAndSet(false, true);
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    public void resetForTests() {
        shuttingDown.set(false);
    }
}
