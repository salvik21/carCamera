package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "error_logs", indices = {
        @Index("timestamp"),
        @Index("module"),
        @Index("severity")
})
public class ErrorLogEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    @NonNull public String module = "";
    @NonNull public String errorType = "";
    @NonNull public ErrorSeverity severity = ErrorSeverity.ERROR;
    @NonNull public String message = "";
    @Nullable public String stackTrace;
    @Nullable public Long tripId;
    @Nullable public Long segmentId;
    @Nullable public Long eventId;
    public boolean recovered;
    public boolean userVisible;
    @NonNull public String appVersion = "";
    @NonNull public String androidVersion = "";
    @NonNull public String deviceModel = "";
    public boolean simulated;
}
