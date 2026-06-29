package com.example.cardvr.database;

public enum CloudUploadStatus {
    PENDING,
    WAITING_FOR_NETWORK,
    WAITING_FOR_WIFI,
    WAITING_FOR_AUTH,
    UPLOADING,
    PAUSED,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
