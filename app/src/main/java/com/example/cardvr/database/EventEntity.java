package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "events", indices = {
        @Index("tripId"),
        @Index({"type", "timestamp"}),
        @Index("status")
})
public class EventEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @Nullable public Long tripId;
    @NonNull public EventType type = EventType.HARD_BRAKING;
    @NonNull public EventSeverity severity = EventSeverity.LOW;
    public int confidence;
    public long timestamp;
    @Nullable public Double latitude;
    @Nullable public Double longitude;
    public double speedBeforeKmh;
    public double speedAfterKmh;
    public double gpsAccuracyMeters;
    public double longitudinalAcceleration;
    public double lateralAcceleration;
    public double verticalAcceleration;
    public double totalAcceleration;
    public double impactG;
    public double gyroMagnitude;
    @Nullable public String orientationBefore;
    @Nullable public String orientationAfter;
    public boolean phonePositionChanged;
    public long protectedFromTime;
    public long protectedUntilTime;
    @NonNull public EventStatus status = EventStatus.DETECTED;
    public boolean simulated;
    @NonNull public String explanation = "";
    public long createdAt;
}
