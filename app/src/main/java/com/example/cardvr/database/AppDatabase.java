package com.example.cardvr.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {VideoSegmentEntity.class, TripEntity.class,
        LocationPointEntity.class, EventEntity.class,
        EventVideoSegmentCrossRef.class}, version = 3, exportSchema = true)
@TypeConverters(DatabaseConverters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract VideoSegmentDao videoSegmentDao();
    public abstract TripDao tripDao();
    public abstract LocationPointDao locationPointDao();
    public abstract EventDao eventDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE video_segments ADD COLUMN tripId INTEGER");
            db.execSQL("ALTER TABLE video_segments ADD COLUMN startLatitude REAL");
            db.execSQL("ALTER TABLE video_segments ADD COLUMN startLongitude REAL");
            db.execSQL("ALTER TABLE video_segments ADD COLUMN endLatitude REAL");
            db.execSQL("ALTER TABLE video_segments ADD COLUMN endLongitude REAL");
            db.execSQL("ALTER TABLE video_segments ADD COLUMN maxSpeedKmh REAL NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS trips (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER, status TEXT NOT NULL, startLatitude REAL, startLongitude REAL, endLatitude REAL, endLongitude REAL, totalDistanceMeters REAL NOT NULL, maxSpeedKmh REAL NOT NULL, averageSpeedKmh REAL NOT NULL, movingTimeMs INTEGER NOT NULL, stoppedTimeMs INTEGER NOT NULL, totalVideoSizeBytes INTEGER NOT NULL, segmentCount INTEGER NOT NULL, protectedSegmentCount INTEGER NOT NULL, createdAt INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_status ON trips(status)");
            db.execSQL("CREATE TABLE IF NOT EXISTS location_points (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId INTEGER NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracyMeters REAL NOT NULL, speedKmh REAL NOT NULL, bearing REAL NOT NULL, altitude REAL, provider TEXT NOT NULL, valid INTEGER NOT NULL, FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_location_points_tripId ON location_points(tripId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_location_points_tripId_timestamp ON location_points(tripId,timestamp)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE trips ADD COLUMN hardBrakingCount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE trips ADD COLUMN impactCount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE trips ADD COLUMN possibleCrashCount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId INTEGER, type TEXT NOT NULL, severity TEXT NOT NULL, confidence INTEGER NOT NULL, timestamp INTEGER NOT NULL, latitude REAL, longitude REAL, speedBeforeKmh REAL NOT NULL, speedAfterKmh REAL NOT NULL, gpsAccuracyMeters REAL NOT NULL, longitudinalAcceleration REAL NOT NULL, lateralAcceleration REAL NOT NULL, verticalAcceleration REAL NOT NULL, totalAcceleration REAL NOT NULL, impactG REAL NOT NULL, gyroMagnitude REAL NOT NULL, orientationBefore TEXT, orientationAfter TEXT, phonePositionChanged INTEGER NOT NULL, protectedFromTime INTEGER NOT NULL, protectedUntilTime INTEGER NOT NULL, status TEXT NOT NULL, simulated INTEGER NOT NULL DEFAULT 0, explanation TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_tripId ON events(tripId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_type_timestamp ON events(type,timestamp)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_status ON events(status)");
            db.execSQL("CREATE TABLE IF NOT EXISTS event_video_segments (eventId INTEGER NOT NULL, segmentId INTEGER NOT NULL, PRIMARY KEY(eventId, segmentId), FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(segmentId) REFERENCES video_segments(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_event_video_segments_eventId ON event_video_segments(eventId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_event_video_segments_segmentId ON event_video_segments(segmentId)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "car-dvr.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
