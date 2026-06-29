package com.example.cardvr.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "traffic_usage")
public class TrafficUsageEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public int yearMonth;
    public long wifiBytes;
    public long mobileBytes;
    public long monthlyLimitBytes;
    public int resetDay;
    public long updatedAt;
}
