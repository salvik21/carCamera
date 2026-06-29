package com.example.cardvr.storage;

import com.example.cardvr.recording.SegmentRepository;

public final class StorageEmergencyManager {
    public enum Level { NORMAL, WARNING, CRITICAL }

    private final StorageLimitManager limits;
    private final FileCleanupManager cleanup;
    private final SegmentRepository repository;

    public StorageEmergencyManager(StorageLimitManager limits,
                                   FileCleanupManager cleanup,
                                   SegmentRepository repository) {
        this.limits = limits;
        this.cleanup = cleanup;
        this.repository = repository;
    }

    public Level evaluate() {
        long normalBytes = repository.getNormalSizeBytes();
        if (!limits.hasEnoughFreeSpace()
                && cleanup.cleanupToLimits() != FileCleanupManager.Result.SATISFIED) {
            return Level.CRITICAL;
        }
        return limits.requiresCleanup(normalBytes) ? Level.WARNING : Level.NORMAL;
    }
}
