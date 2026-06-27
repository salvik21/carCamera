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
        LocationPointEntity.class}, version = 2, exportSchema = true)
@TypeConverters(DatabaseConverters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract VideoSegmentDao videoSegmentDao();
    public abstract TripDao tripDao();
    public abstract LocationPointDao locationPointDao();

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

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "car-dvr.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
