package com.example.cardvr.recording;

import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;

import android.media.MediaMetadataRetriever;

import java.io.File;

public final class RecordingRecoveryManager {
    private final SegmentRepository repository;

    public RecordingRecoveryManager(SegmentRepository repository) {
        this.repository = repository;
    }

    public void recover() {
        recoverDetailed();
    }

    public int recoverDetailed() {
        long now = System.currentTimeMillis();
        int recovered = 0;
        for (VideoSegmentEntity segment : repository.getRecordingSegments()) {
            File file = new File(segment.filePath);
            segment.endTime = now;
            segment.durationMs = Math.max(0L, now - segment.startTime);
            segment.sizeBytes = file.isFile() ? file.length() : 0L;
            if (!file.isFile()) {
                segment.status = SegmentStatus.MISSING;
            } else if (segment.sizeBytes <= 0) {
                segment.status = SegmentStatus.ERROR;
            } else if (canReadVideo(file)) {
                Long duration = readDuration(file);
                if (duration != null && duration > 0) {
                    segment.durationMs = duration;
                }
                segment.status = SegmentStatus.RECOVERED;
                recovered++;
            } else {
                segment.status = SegmentStatus.ERROR;
            }
            repository.update(segment);
        }
        return recovered;
    }

    private static boolean canReadVideo(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null;
        } catch (RuntimeException e) {
            return false;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static Long readDuration(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return value == null ? null : Long.parseLong(value);
        } catch (RuntimeException e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }
}
