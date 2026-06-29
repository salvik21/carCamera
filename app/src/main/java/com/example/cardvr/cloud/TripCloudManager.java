package com.example.cardvr.cloud;

import android.content.Context;

import com.example.cardvr.database.CloudUploadType;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

public final class TripCloudManager {
    private final SegmentRepository segments;
    private final UploadQueueManager queue;

    public TripCloudManager(Context context) {
        segments = new SegmentRepository(context);
        queue = new UploadQueueManager(context);
    }

    public void enqueueProtectedOnly() {
        for (VideoSegmentEntity segment : segments.getAll()) {
            if (segment.status == com.example.cardvr.database.SegmentStatus.PROTECTED) {
                queue.enqueueSegment(segment, CloudUploadType.PROTECTED_VIDEO);
            }
        }
    }

    public void enqueueFullTrip(long tripId) {
        for (VideoSegmentEntity segment : segments.getAll()) {
            if (segment.tripId != null && segment.tripId == tripId) {
                queue.enqueueSegment(segment, CloudUploadType.TRIP_SEGMENT);
            }
        }
    }
}
