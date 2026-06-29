package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips", indices = {@Index(value = "status")})
public class TripEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long startTime;
    @Nullable public Long endTime;
    @NonNull public TripStatus status = TripStatus.ACTIVE;
    @Nullable public Double startLatitude;
    @Nullable public Double startLongitude;
    @Nullable public Double endLatitude;
    @Nullable public Double endLongitude;
    public double totalDistanceMeters;
    public double maxSpeedKmh;
    public double averageSpeedKmh;
    public long movingTimeMs;
    public long stoppedTimeMs;
    public long totalVideoSizeBytes;
    public int segmentCount;
    public int protectedSegmentCount;
    public int hardBrakingCount;
    public int impactCount;
    public int possibleCrashCount;
    public long createdAt;
}
