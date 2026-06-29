package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cloud_errors", indices = {@Index("taskId"), @Index("createdAt")})
public class CloudErrorEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @Nullable public Long taskId;
    @NonNull public String category = "";
    @NonNull public String message = "";
    public boolean retryable;
    public long createdAt;
}
