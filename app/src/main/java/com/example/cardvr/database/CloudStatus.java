package com.example.cardvr.database;

public enum CloudStatus {
    NONE,
    QUEUED,
    UPLOADING,
    COMPLETED,
    FAILED,
    WAITING_FOR_NETWORK,
    WAITING_FOR_WIFI,
    WAITING_FOR_AUTH,
    CANCELLED
}
