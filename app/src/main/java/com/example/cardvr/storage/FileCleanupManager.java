package com.example.cardvr.storage;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

import java.io.File;

public final class FileCleanupManager {
    public enum Result { SATISFIED, NO_DELETABLE_FILES, DELETE_FAILED }

    private final SegmentRepository repository;
    private final StorageManager storage;
    private final StorageLimitManager limits;

    public FileCleanupManager(SegmentRepository repository, StorageManager storage,
                              StorageLimitManager limits) {
        this.repository = repository;
        this.storage = storage;
        this.limits = limits;
    }

    public Result cleanupToLimits() {
        while (limits.requiresCleanup(repository.getNormalSizeBytes())) {
            VideoSegmentEntity candidate = repository.getOldestDeletableNormal();
            if (candidate == null) return Result.NO_DELETABLE_FILES;
            repository.setStatus(candidate.id, SegmentStatus.DELETE_PENDING, null);
            if (!storage.delete(new File(candidate.filePath))) {
                repository.setStatus(candidate.id, SegmentStatus.ERROR, "Delete failed");
                return Result.DELETE_FAILED;
            }
            repository.deleteById(candidate.id);
        }
        return Result.SATISFIED;
    }
}
