package com.example.cardvr.protection;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

public final class ProtectionManager {
    private final SegmentRepository repository;
    private long currentId;
    private boolean protectCurrent;
    private boolean protectNext;
    private long protectUntilTimeMillis;

    public ProtectionManager(SegmentRepository repository) {
        this.repository = repository;
    }

    public synchronized void onSegmentCreated(long id) {
        currentId = id;
        if (protectNext) {
            protectCurrent = true;
            protectNext = false;
        }
        if (protectUntilTimeMillis > 0 && System.currentTimeMillis() <= protectUntilTimeMillis) {
            protectCurrent = true;
        }
    }

    public synchronized boolean shouldProtectOnFinalize(long id) {
        boolean result = id == currentId
                && (protectCurrent
                || (protectUntilTimeMillis > 0 && System.currentTimeMillis() <= protectUntilTimeMillis));
        if (result && System.currentTimeMillis() > protectUntilTimeMillis) {
            protectCurrent = false;
        }
        return result;
    }

    public synchronized void protectWindow() {
        VideoSegmentEntity previous = repository.getLatestNormal();
        if (previous != null) {
            repository.setStatus(previous.id, SegmentStatus.PROTECTED, "USER_EVENT");
        }
        protectCurrent = true;
        protectNext = true;
    }

    public synchronized void protectUntil(long untilTimeMillis) {
        protectUntilTimeMillis = Math.max(protectUntilTimeMillis, untilTimeMillis);
        protectCurrent = true;
    }
}
