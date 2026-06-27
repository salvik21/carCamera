package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_points",
        foreignKeys = @ForeignKey(entity = TripEntity.class, parentColumns = "id",
                childColumns = "tripId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("tripId"), @Index(value = {"tripId", "timestamp"})})
public class LocationPointEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long tripId;
    public long timestamp;
    public double latitude;
    public double longitude;
    public float accuracyMeters;
    public double speedKmh;
    public float bearing;
    @Nullable public Double altitude;
    @NonNull public String provider = "";
    public boolean valid;
}
