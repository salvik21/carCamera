package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cloud_accounts", indices = {@Index(value = {"provider"}, unique = true)})
public class CloudAccountEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public CloudDestination provider = CloudDestination.GOOGLE_DRIVE;
    @Nullable public String accountName;
    @Nullable public String accountHash;
    public boolean connected;
    public long createdAt;
    public long updatedAt;
    @Nullable public String lastAuthError;
}
