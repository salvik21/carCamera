package com.example.cardvr.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "event_video_segments",
        primaryKeys = {"eventId", "segmentId"},
        foreignKeys = {
                @ForeignKey(entity = EventEntity.class, parentColumns = "id",
                        childColumns = "eventId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = VideoSegmentEntity.class, parentColumns = "id",
                        childColumns = "segmentId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("eventId"), @Index("segmentId")}
)
public class EventVideoSegmentCrossRef {
    public long eventId;
    public long segmentId;

    public EventVideoSegmentCrossRef(long eventId, long segmentId) {
        this.eventId = eventId;
        this.segmentId = segmentId;
    }
}
