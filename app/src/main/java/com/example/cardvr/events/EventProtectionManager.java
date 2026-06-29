package com.example.cardvr.events;

import androidx.annotation.NonNull;

import com.example.cardvr.database.EventEntity;
import com.example.cardvr.database.EventStatus;
import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;

import java.util.List;

public final class EventProtectionManager {
    private final EventRepository events;
    private final SegmentRepository segments;

    public EventProtectionManager(@NonNull EventRepository events,
                                  @NonNull SegmentRepository segments) {
        this.events = events;
        this.segments = segments;
    }

    public int protectExistingSegments(@NonNull EventEntity event) {
        List<VideoSegmentEntity> overlapping = segments.getSegmentsOverlapping(
                event.protectedFromTime,
                event.protectedUntilTime
        );
        int protectedCount = 0;
        for (VideoSegmentEntity segment : overlapping) {
            events.linkSegment(event.id, segment.id);
            if (segment.status != SegmentStatus.PROTECTED) {
                segments.setStatus(segment.id, SegmentStatus.PROTECTED,
                        "EVENT_" + event.type.name());
            }
            protectedCount++;
        }
        if (protectedCount > 0 && event.status == EventStatus.DETECTED) {
            events.setStatus(event.id, EventStatus.PROTECTED);
        }
        return protectedCount;
    }
}
