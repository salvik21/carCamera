package com.example.cardvr.recording;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;

import java.io.File;

public final class RecordingRecoveryManager {
    private final SegmentRepository repository;

    public RecordingRecoveryManager(SegmentRepository repository) {
        this.repository = repository;
    }

    public void recover() {
        long now = System.currentTimeMillis();
        for (VideoSegmentEntity segment : repository.getRecordingSegments()) {
            File file = new File(segment.filePath);
            segment.endTime = now;
            segment.durationMs = Math.max(0L, now - segment.startTime);
            segment.sizeBytes = file.isFile() ? file.length() : 0L;
            segment.status = file.isFile() && segment.sizeBytes > 0
                    ? SegmentStatus.NORMAL : SegmentStatus.ERROR;
            repository.update(segment);
        }
    }
}
