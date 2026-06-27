package com.example.cardvr.protection;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

public final class ProtectionManager {
    private final SegmentRepository repository;
    private long currentId;
    private boolean protectCurrent;
    private boolean protectNext;

    public ProtectionManager(SegmentRepository repository) {
        this.repository = repository;
    }

    public synchronized void onSegmentCreated(long id) {
        currentId = id;
        if (protectNext) {
            protectCurrent = true;
            protectNext = false;
        }
    }

    public synchronized boolean shouldProtectOnFinalize(long id) {
        boolean result = id == currentId && protectCurrent;
        if (result) protectCurrent = false;
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
}
