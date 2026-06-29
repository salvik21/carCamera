package com.example.cardvr.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cloud_folders", indices = {
        @Index(value = {"provider", "path"}, unique = true)
})
public class CloudFolderEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public CloudDestination provider = CloudDestination.GOOGLE_DRIVE;
    @NonNull public String path = "";
    @Nullable public String remoteFolderId;
    public long createdAt;
    public long updatedAt;
}
