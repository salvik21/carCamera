package com.example.cardvr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardvr.R;
import com.example.cardvr.database.TripEntity;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.trips.TripRepository;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class TripSummaryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        TextView summary = new TextView(this);
        root.addView(summary);
        Button recordings = new Button(this);
        recordings.setText(R.string.open_trip_recordings);
        recordings.setOnClickListener(v -> startActivity(
                new Intent(this, RecordingsActivity.class)));
        root.addView(recordings);
        Button cloud = new Button(this);
        cloud.setText(R.string.cloud_later);
        cloud.setEnabled(false);
        root.addView(cloud);
        Button close = new Button(this);
        close.setText(android.R.string.ok);
        close.setOnClickListener(v -> finish());
        root.addView(close);
        setContentView(root);
        SegmentRepository.ioExecutor().execute(() -> {
            TripEntity trip = new TripRepository(this).getLatestCompleted();
            runOnUiThread(() -> summary.setText(trip == null
                    ? getString(R.string.no_completed_trip)
                    : format(trip)));
        });
    }

    private String format(TripEntity trip) {
        long duration = (trip.endTime == null ? System.currentTimeMillis() : trip.endTime)
                - trip.startTime;
        return DateFormat.getDateTimeInstance().format(new Date(trip.startTime))
                + "\n" + String.format(Locale.getDefault(),
                "Длительность: %d мин\nРасстояние: %.2f км\nМаксимум: %.1f км/ч\nСредняя: %.1f км/ч\nФрагментов: %d\nРазмер: %.1f МБ\nЗащищено: %d",
                duration / 60_000L, trip.totalDistanceMeters / 1000d,
                trip.maxSpeedKmh, trip.averageSpeedKmh, trip.segmentCount,
                trip.totalVideoSizeBytes / 1048576d, trip.protectedSegmentCount);
    }
}
