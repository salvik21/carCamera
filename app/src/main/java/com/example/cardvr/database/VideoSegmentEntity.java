package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_segments", indices = {
        @Index(value = "filePath", unique = true),
        @Index(value = {"status", "startTime"})
})
public class VideoSegmentEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String fileName = "";
    @NonNull public String filePath = "";
    public long startTime;
    @Nullable public Long endTime;
    public long durationMs;
    public long sizeBytes;
    @NonNull public SegmentStatus status = SegmentStatus.RECORDING;
    @NonNull public String cameraId = "";
    public long createdAt;
    @Nullable public String protectedReason;
    @NonNull public OperationStatus uploadStatus = OperationStatus.NONE;
    @NonNull public OperationStatus transferStatus = OperationStatus.NONE;
    @Nullable public Long tripId;
    @Nullable public Double startLatitude;
    @Nullable public Double startLongitude;
    @Nullable public Double endLatitude;
    @Nullable public Double endLongitude;
    public double maxSpeedKmh;
}
