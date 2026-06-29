package com.example.cardvr.storage;

import android.content.Context;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

import java.io.File;

public final class StorageConsistencyChecker {
    private final SegmentRepository repository;

    public StorageConsistencyChecker(Context context) {
        repository = new SegmentRepository(context);
    }

    public StorageConsistencyResult check() {
        StorageConsistencyResult result = new StorageConsistencyResult();
        for (VideoSegmentEntity segment : repository.getAll()) {
            File file = new File(segment.filePath);
            if (!file.isFile()) {
                if (segment.status != SegmentStatus.MISSING) {
                    repository.setStatus(segment.id, SegmentStatus.MISSING, "FILE_MISSING");
                }
                result.missingFiles++;
            } else if (file.length() <= 0) {
                repository.setStatus(segment.id, SegmentStatus.ERROR, "ZERO_SIZE_FILE");
                result.errorFiles++;
            }
        }
        return result;
    }

    public static final class StorageConsistencyResult {
        public int missingFiles;
        public int errorFiles;
    }
}
