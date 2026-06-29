package com.example.cardvr.database;

import androidx.room.TypeConverter;

public final class DatabaseConverters {
    @TypeConverter
    public static String fromSegmentStatus(SegmentStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static SegmentStatus toSegmentStatus(String value) {
        return value == null ? null : SegmentStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromOperationStatus(OperationStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static OperationStatus toOperationStatus(String value) {
        return value == null ? null : OperationStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromTripStatus(TripStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static TripStatus toTripStatus(String value) {
        return value == null ? null : TripStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromEventType(EventType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static EventType toEventType(String value) {
        return value == null ? null : EventType.valueOf(value);
    }

    @TypeConverter
    public static String fromEventSeverity(EventSeverity value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static EventSeverity toEventSeverity(String value) {
        return value == null ? null : EventSeverity.valueOf(value);
    }

    @TypeConverter
    public static String fromEventStatus(EventStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static EventStatus toEventStatus(String value) {
        return value == null ? null : EventStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromCloudStatus(CloudStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static CloudStatus toCloudStatus(String value) {
        return value == null ? null : CloudStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromCloudUploadStatus(CloudUploadStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static CloudUploadStatus toCloudUploadStatus(String value) {
        return value == null ? null : CloudUploadStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromCloudUploadType(CloudUploadType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static CloudUploadType toCloudUploadType(String value) {
        return value == null ? null : CloudUploadType.valueOf(value);
    }

    @TypeConverter
    public static String fromCloudDestination(CloudDestination value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static CloudDestination toCloudDestination(String value) {
        return value == null ? null : CloudDestination.valueOf(value);
    }
}
